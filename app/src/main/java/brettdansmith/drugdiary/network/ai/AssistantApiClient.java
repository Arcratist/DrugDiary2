package brettdansmith.drugdiary.network.ai;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.domain.model.ai.AiResolvedConfig;
import brettdansmith.drugdiary.network.ai.capabilities.AiConfigResolver;
import brettdansmith.drugdiary.ui.assistant.ChatAdapter;
import brettdansmith.drugdiary.ui.assistant.ChatMessage;
import brettdansmith.drugdiary.settings.AppSettings;

public final class AssistantApiClient {
    private static final String TAG = "AssistantApiClient";
    public static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    public static final String DEEPSEEK_CHAT_URL = "https://api.deepseek.com/chat/completions";
    public static final String OPENROUTER_CHAT_URL = "https://openrouter.ai/api/v1/chat/completions";
    public static final String XAI_CHAT_URL = "https://api.x.ai/v1/chat/completions";
    public static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final String MISTRAL_CHAT_URL = "https://api.mistral.ai/v1/chat/completions";
    public static final String PERPLEXITY_CHAT_URL = "https://api.perplexity.ai/chat/completions";
    private static final String ANTHROPIC_MESSAGES_URL = "https://api.anthropic.com/v1/messages";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int GEMINI_MAX_OUTPUT_TOKENS = 2400;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 2400;
    private static final int MAX_TEXT_ATTACHMENT_CHARS = 120_000;

    public enum AttachmentMode {
        OPENAI_NATIVE_IMAGE,
        ANTHROPIC_NATIVE_IMAGE,
        GEMINI_NATIVE_MULTIMODAL,
        PORTABLE_TEXT_ONLY
    }

    public interface StreamCallback {
        void onChunk(String text);
        void onDone();
        void onError(String error);
    }

    private AssistantApiClient() {}

    public static void streamAssistantResponse(Context context, List<ChatMessage> messages, String profileContextText, StreamCallback callback) {
        SettingsRepository settingsRepository = new SettingsRepository(context);
        AiProvider primaryProvider = AiProvider.fromPreference(AppSettings.getAiProvider(context));

        List<AiProvider> providersToTry = new ArrayList<>();
        providersToTry.add(primaryProvider);

        if (settingsRepository.isAiFallbackEnabled()) {
            String fallbackOrder = settingsRepository.getAiFallbackOrder();
            if (!fallbackOrder.trim().isEmpty()) {
                for (String p : fallbackOrder.split(",")) {
                    AiProvider provider = AiProvider.fromPreference(p.trim());
                    if (provider != primaryProvider && !providersToTry.contains(provider)) {
                        providersToTry.add(provider);
                    }
                }
            }
            // Add other core providers as ultimate fallback if not already in list
            AiProvider[] core = {AiProvider.OPENAI, AiProvider.GEMINI, AiProvider.ANTHROPIC, AiProvider.GROQ};
            for (AiProvider p : core) {
                if (!providersToTry.contains(p)) {
                    providersToTry.add(p);
                }
            }
        }

        tryNextProvider(context, messages, profileContextText, providersToTry, 0, callback);
    }

    private static void tryNextProvider(Context context, List<ChatMessage> messages, String profileContextText, List<AiProvider> providers, int index, StreamCallback callback) {
        if (index >= providers.size()) {
            callback.onError("All configured AI providers failed to respond.");
            return;
        }

        AiProvider provider = providers.get(index);
        SettingsRepository settingsRepository = new SettingsRepository(context);
        ProviderSettings providerSettings = settingsRepository.getProviderSettings(provider);

        if (!providerSettings.enabled || providerSettings.apiKey.isEmpty()) {
            // Skip disabled or unconfigured providers in the fallback chain
            tryNextProvider(context, messages, profileContextText, providers, index + 1, callback);
            return;
        }

        AiProviderClient client = createProviderClient(provider, providerSettings, settingsRepository);
        int maxRetries = providerSettings.maxRetries;
        int timeout = providerSettings.timeoutSeconds;

        attemptWithRetry(context, client, messages, profileContextText, maxRetries, 0, timeout, new StreamCallback() {
            @Override
            public void onChunk(String text) {
                callback.onChunk(text);
            }

            @Override
            public void onDone() {
                callback.onDone();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Provider " + provider.displayName() + " failed: " + error);
                // On error, try the next provider in the list
                tryNextProvider(context, messages, profileContextText, providers, index + 1, callback);
            }
        });
    }

    private static void attemptWithRetry(Context context, AiProviderClient client, List<ChatMessage> messages, String profileContextText, int maxRetries, int currentRetry, int timeout, StreamCallback callback) {
        try {
            client.stream(context, messages, profileContextText, new StreamCallback() {
                @Override
                public void onChunk(String text) {
                    callback.onChunk(text);
                }

                @Override
                public void onDone() {
                    callback.onDone();
                }

                @Override
                public void onError(String error) {
                    if (currentRetry < maxRetries) {
                        Log.i(TAG, "Retrying provider... (" + (currentRetry + 1) + "/" + maxRetries + ")");
                        attemptWithRetry(context, client, messages, profileContextText, maxRetries, currentRetry + 1, timeout, callback);
                    } else {
                        callback.onError(error);
                    }
                }
            }, timeout);
        } catch (Exception e) {
            if (currentRetry < maxRetries) {
                attemptWithRetry(context, client, messages, profileContextText, maxRetries, currentRetry + 1, timeout, callback);
            } else {
                callback.onError(e.getMessage());
            }
        }
    }

    private static AiProviderClient createProviderClient(AiProvider provider, ProviderSettings settings, SettingsRepository repo) {
        AiResolvedConfig resolved = AiConfigResolver.resolve(provider, settings, repo.isAiWebSearchEnabled(), repo.isAiCitationsRequired());
        
        switch (provider) {
            case ANTHROPIC: return new AnthropicProviderClient();
            case GEMINI: return new GeminiProviderClient();
            case DEEPSEEK: return new OpenAiCompatibleProviderClient(resolved.apiKey, resolved.model, resolved.endpointUrl, "DeepSeek", AttachmentMode.PORTABLE_TEXT_ONLY);
            case XAI: return new OpenAiCompatibleProviderClient(resolved.apiKey, resolved.model, resolved.endpointUrl, "xAI", openAiCompatibleAttachmentMode("xAI", resolved.model));
            case GROQ: return new OpenAiCompatibleProviderClient(resolved.apiKey, resolved.model, resolved.endpointUrl, "Groq", openAiCompatibleAttachmentMode("Groq", resolved.model));
            case MISTRAL: return new OpenAiCompatibleProviderClient(resolved.apiKey, resolved.model, resolved.endpointUrl, "Mistral", openAiCompatibleAttachmentMode("Mistral", resolved.model));
            case PERPLEXITY: return new OpenAiCompatibleProviderClient(resolved.apiKey, resolved.model, resolved.endpointUrl, "Perplexity", openAiCompatibleAttachmentMode("Perplexity", resolved.model));
            case OPENROUTER: return new OpenAiCompatibleProviderClient(resolved.apiKey, resolved.model, resolved.endpointUrl, "OpenRouter", openAiCompatibleAttachmentMode("OpenRouter", resolved.model));
            case OPENAI: 
            default:
                return new OpenAiProviderClient();
        }
    }

    public static void streamOpenAiCompatibleResponse(
            Context context,
            List<ChatMessage> messages, String profileContextText,
            StreamCallback callback, String apiKey, String model, String endpointUrl, String providerName,
            AttachmentMode attachmentMode, int timeoutSeconds) throws Exception {
            
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(providerName + " API key is not configured.");
        }

        List<ChatMessage> preparedMessages = prepareMessagesForProvider(messages, attachmentMode, providerName);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", true);
        body.put("temperature", 0.35);
        body.put("max_tokens", DEFAULT_MAX_OUTPUT_TOKENS);
        
        JSONArray input = new JSONArray();
        input.put(new JSONObject().put("role", "system").put("content", buildInstructions(profileContextText, providerName)));
        
        int start = Math.max(0, preparedMessages.size() - 14);
        for (int i = start; i < preparedMessages.size(); i++) {
            ChatMessage msg = preparedMessages.get(i);
            String content = msg.getContent();
            if (shouldSkipProviderMessage(content)) continue;
            input.put(new JSONObject()
                    .put("role", msg.isSent() ? "user" : "assistant")
                    .put("content", buildOpenAiContent(msg, content)));
        }
        body.put("messages", input);

        postStream(endpointUrl, body, connection -> {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            if ("OpenRouter".equals(providerName)) {
                connection.setRequestProperty("HTTP-Referer", "https://drugdiary.app"); 
                connection.setRequestProperty("X-Title", "DrugDiary Assistant");
            }
        }, providerName, line -> {
            if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                try {
                    JSONObject json = new JSONObject(line.substring(6));
                    JSONArray choices = json.optJSONArray("choices");
                    if (choices != null && choices.length() > 0) {
                        JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
                        if (delta != null && delta.has("content")) {
                            callback.onChunk(delta.getString("content"));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, callback, timeoutSeconds);
    }

    public static void streamAnthropicResponse(Context context, List<ChatMessage> messages, String profileContextText, StreamCallback callback, int timeoutSeconds) throws Exception {
        String apiKey = AppSettings.getAnthropicApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Claude API key is not configured.");
        }

        List<ChatMessage> preparedMessages = prepareMessagesForProvider(messages, AttachmentMode.ANTHROPIC_NATIVE_IMAGE, "Claude");

        JSONObject body = new JSONObject();
        body.put("model", AppSettings.getAnthropicModel(context));
        body.put("max_tokens", DEFAULT_MAX_OUTPUT_TOKENS);
        body.put("temperature", 0.35);
        body.put("stream", true);
        body.put("system", buildInstructions(profileContextText, "Claude"));
        body.put("messages", buildAnthropicMessages(preparedMessages));

        postStream(ANTHROPIC_MESSAGES_URL, body, connection -> {
            connection.setRequestProperty("x-api-key", apiKey.trim());
            connection.setRequestProperty("anthropic-version", "2023-06-01");
        }, "Claude", line -> {
            if (line.startsWith("data: ")) {
                try {
                    JSONObject json = new JSONObject(line.substring(6));
                    if ("content_block_delta".equals(json.optString("type"))) {
                        JSONObject delta = json.optJSONObject("delta");
                        if (delta != null && "text_delta".equals(delta.optString("type"))) {
                            callback.onChunk(delta.optString("text", ""));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, callback, timeoutSeconds);
    }

    public static void streamGeminiResponse(Context context, List<ChatMessage> messages, String profileContextText, StreamCallback callback, int timeoutSeconds) throws Exception {
        String apiKey = AppSettings.getGeminiApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        List<ChatMessage> preparedMessages = prepareMessagesForProvider(messages, AttachmentMode.GEMINI_NATIVE_MULTIMODAL, "Gemini");

        JSONObject body = new JSONObject();
        JSONObject system = new JSONObject();
        system.put("parts", new JSONArray().put(new JSONObject().put("text", buildInstructions(profileContextText, "Gemini"))));
        body.put("system_instruction", system);
        body.put("contents", buildGeminiContents(preparedMessages));
        body.put("generationConfig", new JSONObject()
                .put("maxOutputTokens", GEMINI_MAX_OUTPUT_TOKENS)
                .put("temperature", 0.35));

        String model = AppSettings.getGeminiModel(context);
        String url = GEMINI_BASE_URL + model + ":streamGenerateContent?alt=sse";
        
        postStream(url, body, connection -> {
            connection.setRequestProperty("x-goog-api-key", apiKey.trim());
        }, "Gemini", line -> {
            if (line.startsWith("data: ")) {
                try {
                    String payload = line.substring(6).trim();
                    if (payload.isEmpty()) return;
                    JSONObject json = new JSONObject(payload);
                    JSONArray candidates = json.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject contentObj = candidates.optJSONObject(0).optJSONObject("content");
                        if (contentObj != null) {
                            JSONArray parts = contentObj.optJSONArray("parts");
                            if (parts != null && parts.length() > 0) {
                                callback.onChunk(parts.optJSONObject(0).optString("text", ""));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, callback, timeoutSeconds);
    }

    private static String buildInstructions(String profileContextText, String provider) {
        return brettdansmith.drugdiary.assistant.AssistantConfig.systemPrompt(provider)
                + "\n\n"
                + brettdansmith.drugdiary.assistant.AssistantConfig.developerPrompt()
                + "\n\nPROFILE_CONTEXT_TEXT (already shared by the user; use this as known context)\n"
                + (profileContextText == null || profileContextText.trim().isEmpty() ? "none" : profileContextText);
    }

    public static AttachmentMode openAiCompatibleAttachmentMode(String providerName, String model) {
        String normalizedProvider = (providerName == null ? "" : providerName).toLowerCase(Locale.US);
        String normalizedModel = (model == null ? "" : model).toLowerCase(Locale.US);
        if (normalizedModel.contains("vision")
                || normalizedModel.contains("multimodal")
                || normalizedModel.contains("vl")
                || normalizedModel.contains("llava")
                || normalizedModel.contains("pixtral")
                || normalizedModel.contains("qwen-vl")
                || normalizedModel.contains("qwen2-vl")
                || normalizedModel.contains("qwen2.5-vl")
                || normalizedModel.contains("llama-4")
                || normalizedModel.contains("llama-3.2-11b")
                || normalizedModel.contains("llama-3.2-90b")
                || normalizedModel.contains("gpt-4o")
                || normalizedModel.contains("gpt-4.1")
                || normalizedModel.contains("gpt-5")
                || normalizedModel.contains("o3")
                || normalizedModel.contains("o4")
                || normalizedModel.contains("claude-3")
                || normalizedModel.contains("gemini")) {
            return AttachmentMode.OPENAI_NATIVE_IMAGE;
        }
        if ("openai".equals(normalizedProvider)) {
            return normalizedModel.startsWith("gpt-4") ? AttachmentMode.OPENAI_NATIVE_IMAGE : AttachmentMode.PORTABLE_TEXT_ONLY;
        }
        return AttachmentMode.PORTABLE_TEXT_ONLY;
    }

    private static List<ChatMessage> prepareMessagesForProvider(List<ChatMessage> messages, AttachmentMode mode, String providerName) {
        List<ChatMessage> prepared = new ArrayList<>();
        if (messages == null) return prepared;
        for (ChatMessage msg : messages) {
            if (msg == null) continue;
            if (!msg.hasAttachment()) {
                prepared.add(msg);
                continue;
            }

            String content = (msg.getContent() == null ? "" : msg.getContent()) + nonPrimaryAttachmentSummary(msg);
            if (isTextLikeAttachment(msg)) {
                prepared.add(copyWithoutAttachment(msg, content + decodedTextAttachmentBlock(msg)));
            } else if (canSendNativeAttachment(mode, msg)) {
                prepared.add(msg);
            } else {
                prepared.add(copyWithoutAttachment(msg, content + unsupportedAttachmentNote(msg, providerName)));
            }
        }
        return prepared;
    }

    private static boolean canSendNativeAttachment(AttachmentMode mode, ChatMessage msg) {
        if (mode == null || msg == null || !msg.hasAttachment()) return false;
        if (mode == AttachmentMode.GEMINI_NATIVE_MULTIMODAL) {
            for (ChatMessage.Attachment attachment : msg.getAttachments()) {
                if (canGeminiInline(attachment)) return true;
            }
            return false;
        }
        if (mode == AttachmentMode.OPENAI_NATIVE_IMAGE || mode == AttachmentMode.ANTHROPIC_NATIVE_IMAGE) {
            for (ChatMessage.Attachment attachment : msg.getAttachments()) {
                if (isImageAttachment(attachment)) return true;
            }
            return false;
        }
        return false;
    }

    private static boolean canGeminiInline(ChatMessage msg) {
        String mime = normalizedMime(msg);
        return canGeminiInline(mime);
    }

    private static boolean canGeminiInline(ChatMessage.Attachment attachment) {
        String mime = normalizedMime(attachment);
        return canGeminiInline(mime);
    }

    private static boolean canGeminiInline(String mime) {
        return mime.startsWith("image/")
                || mime.startsWith("audio/")
                || mime.startsWith("video/")
                || "application/pdf".equals(mime);
    }

    private static boolean isTextLikeAttachment(ChatMessage msg) {
        String mime = normalizedMime(msg);
        return mime.startsWith("text/")
                || mime.contains("json")
                || mime.contains("xml")
                || mime.contains("csv")
                || mime.contains("yaml")
                || mime.contains("markdown")
                || mime.contains("x-www-form-urlencoded");
    }

    private static String normalizedMime(ChatMessage msg) {
        String mime = msg == null ? "" : msg.getAttachmentMimeType();
        return mime == null ? "" : mime.toLowerCase(Locale.US).trim();
    }

    private static String normalizedMime(ChatMessage.Attachment attachment) {
        String mime = attachment == null ? "" : attachment.mimeType;
        return mime == null ? "" : mime.toLowerCase(Locale.US).trim();
    }

    private static boolean isImageAttachment(ChatMessage.Attachment attachment) {
        return normalizedMime(attachment).startsWith("image/");
    }

    private static ChatMessage copyWithoutAttachment(ChatMessage msg, String content) {
        return new ChatMessage(msg.getId(), content, msg.isSent(), msg.getCreatedAt());
    }

    private static Object buildOpenAiContent(ChatMessage msg, String text) throws Exception {
        if (msg == null || !msg.hasAttachment()) return text;
        List<ChatMessage.Attachment> imageAttachments = imageAttachments(msg);
        if (imageAttachments.isEmpty()) return text + attachmentTextFallback(msg);
        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "text").put("text", text));
        for (ChatMessage.Attachment attachment : imageAttachments) {
            content.put(new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", new JSONObject().put("url", dataUri(attachment))));
        }
        return content;
    }

    private static Object buildAnthropicContent(ChatMessage msg, String text) throws Exception {
        if (msg == null || !msg.hasAttachment()) return text;
        List<ChatMessage.Attachment> imageAttachments = imageAttachments(msg);
        if (imageAttachments.isEmpty()) return text + attachmentTextFallback(msg);
        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "text").put("text", text));
        for (ChatMessage.Attachment attachment : imageAttachments) {
            content.put(new JSONObject()
                    .put("type", "image")
                    .put("source", new JSONObject()
                            .put("type", "base64")
                            .put("media_type", attachment.mimeType)
                            .put("data", attachment.base64)));
        }
        return content;
    }

    private static JSONArray buildGeminiParts(ChatMessage msg, String text) throws Exception {
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", text));
        if (msg != null && msg.hasAttachment()) {
            for (ChatMessage.Attachment attachment : msg.getAttachments()) {
                if (attachment == null) continue;
                if (!canGeminiInline(attachment)) continue;
                parts.put(new JSONObject().put("inline_data", new JSONObject()
                        .put("mime_type", attachment.mimeType)
                        .put("data", attachment.base64)));
            }
        }
        return parts;
    }

    private static String dataUri(ChatMessage msg) {
        return "data:" + msg.getAttachmentMimeType() + ";base64," + msg.getAttachmentBase64();
    }

    private static String dataUri(ChatMessage.Attachment attachment) {
        return "data:" + attachment.mimeType + ";base64," + attachment.base64;
    }

    private static String attachmentTextFallback(ChatMessage msg) {
        if (!isTextLikeAttachment(msg)) {
            return unsupportedAttachmentNote(msg, "this provider");
        }
        return decodedTextAttachmentBlock(msg);
    }

    private static String decodedTextAttachmentBlock(ChatMessage msg) {
        try {
            byte[] bytes = Base64.decode(msg.getAttachmentBase64(), Base64.DEFAULT);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_ATTACHMENT_CHARS) text = text.substring(0, MAX_TEXT_ATTACHMENT_CHARS) + "\n[truncated]";
            return "\n\nAttached file: " + safeAttachmentName(msg) + " (" + msg.getAttachmentMimeType() + ")\n```text\n" + text + "\n```";
        } catch (Exception e) {
            return "\n\n[Attached file: " + safeAttachmentName(msg) + " could not be decoded as text.]";
        }
    }

    private static String unsupportedAttachmentNote(ChatMessage msg, String providerName) {
        return "\n\n[Attached file: " + safeAttachmentName(msg) + " (" + msg.getAttachmentMimeType()
                + "). " + providerName + " cannot directly read this attachment type in the current request path.]";
    }

    private static String nonPrimaryAttachmentSummary(ChatMessage msg) {
        if (msg == null || msg.getAttachments().size() <= 1) return "";
        StringBuilder builder = new StringBuilder("\n\nAdditional attachments in this message:\n");
        for (int i = 1; i < msg.getAttachments().size(); i++) {
            ChatMessage.Attachment attachment = msg.getAttachments().get(i);
            String name = attachment == null || attachment.name == null || attachment.name.trim().isEmpty()
                    ? "attachment" : attachment.name.trim();
            String mime = attachment == null || attachment.mimeType == null || attachment.mimeType.trim().isEmpty()
                    ? "application/octet-stream" : attachment.mimeType.trim();
            builder.append("- ").append(name).append(" (").append(mime).append(")\n");
        }
        return builder.toString().trim().isEmpty() ? "" : ("\n\n" + builder.toString().trim());
    }

    private static String safeAttachmentName(ChatMessage msg) {
        String name = msg == null ? "" : msg.getAttachmentName();
        return name == null || name.trim().isEmpty() ? "attachment" : name.trim();
    }

    private static List<ChatMessage.Attachment> imageAttachments(ChatMessage msg) {
        List<ChatMessage.Attachment> images = new ArrayList<>();
        if (msg == null) return images;
        for (ChatMessage.Attachment attachment : msg.getAttachments()) {
            if (isImageAttachment(attachment)) images.add(attachment);
        }
        return images;
    }

    private static JSONArray buildAnthropicMessages(List<ChatMessage> messages) throws Exception {
        JSONArray input = new JSONArray();
        int start = Math.max(0, messages.size() - 14);
        String lastRole = "";
        for (int i = start; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String content = msg.getContent();
            if (shouldSkipProviderMessage(content)) continue;
            String role = msg.isSent() ? "user" : "assistant";
            if (!msg.hasAttachment() && role.equals(lastRole) && input.length() > 0) {
                JSONObject previous = input.getJSONObject(input.length() - 1);
                if (previous.opt("content") instanceof String) {
                    previous.put("content", previous.optString("content", "") + "\n\n" + content);
                } else {
                    input.put(new JSONObject().put("role", role).put("content", buildAnthropicContent(msg, content)));
                }
            } else {
                input.put(new JSONObject().put("role", role).put("content", buildAnthropicContent(msg, content)));
                lastRole = role;
            }
        }
        if (input.length() == 0) {
            input.put(new JSONObject().put("role", "user").put("content", "Please start the conversation."));
        }
        return input;
    }

    private static boolean shouldSkipProviderMessage(String content) {
        if (content == null || content.trim().isEmpty()) return true;
        String trimmed = content.trim();
        return trimmed.startsWith(ChatAdapter.COMMAND_PREFIX)
                || trimmed.startsWith("/")
                || trimmed.startsWith("[[system-message]]")
                || (trimmed.startsWith("Hi ") && trimmed.contains("What can I help with today?"));
    }

    private static JSONArray buildGeminiContents(List<ChatMessage> messages) throws Exception {
        JSONArray contents = new JSONArray();
        int start = Math.max(0, messages.size() - 14);
        String lastRole = "";
        for (int i = start; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String content = msg.getContent();
            if (shouldSkipProviderMessage(content)) continue;
            String role = msg.isSent() ? "user" : "model";
            JSONArray parts = buildGeminiParts(msg, content);
            if (role.equals(lastRole) && contents.length() > 0) {
                JSONObject previous = contents.getJSONObject(contents.length() - 1);
                JSONArray previousParts = previous.getJSONArray("parts");
                for (int p = 0; p < parts.length(); p++) previousParts.put(parts.getJSONObject(p));
            } else {
                contents.put(new JSONObject().put("role", role).put("parts", parts));
                lastRole = role;
            }
        }
        if (contents.length() == 0) {
            contents.put(new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", "Please start the conversation."))));
        }
        return contents;
    }

    private interface LineProcessor {
        void process(String line);
    }

    private static void postStream(String url, JSONObject body, RequestHeaders headers, String label, LineProcessor processor, StreamCallback callback, int timeoutSeconds) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        int timeoutMs = Math.max(5000, timeoutSeconds * 1000);
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs + 20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "text/event-stream");
        if (headers != null) headers.apply(connection);
        
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            output.flush();
        }

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(label + " request failed: HTTP " + code + " " + readResponseBody(connection, true));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            int charRead;
            StringBuilder lineBuilder = new StringBuilder();
            while ((charRead = reader.read()) != -1) {
                char c = (char) charRead;
                lineBuilder.append(c);
                if (c == '\n') {
                    String line = lineBuilder.toString().trim();
                    if (!line.isEmpty()) {
                        processor.process(line);
                    }
                    lineBuilder.setLength(0);
                }
            }
            if (lineBuilder.length() > 0) {
                processor.process(lineBuilder.toString().trim());
            }
        }
        
        callback.onDone();
    }

    private static String readResponseBody(HttpURLConnection connection, boolean error) throws Exception {
        if (connection == null) return "";
        InputStream stream = error ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        }
    }

    private interface RequestHeaders {
        void apply(HttpURLConnection connection);
    }
}
