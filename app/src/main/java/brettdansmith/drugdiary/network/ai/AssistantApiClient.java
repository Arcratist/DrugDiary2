package brettdansmith.drugdiary.network.ai;

import android.content.Context;
import android.util.Base64;

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
import brettdansmith.drugdiary.model.ai.AiDebugMetadata;
import brettdansmith.drugdiary.model.ai.AiResolvedConfig;
import brettdansmith.drugdiary.network.ai.capabilities.AiConfigResolver;
import brettdansmith.drugdiary.ui.assistant.ChatAdapter;
import brettdansmith.drugdiary.ui.assistant.ChatMessage;
import brettdansmith.drugdiary.settings.AppSettings;

public final class AssistantApiClient {
    // Provider endpoints differ mostly in request/stream envelope shape.
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
        String providerPref = AppSettings.getAiProvider(context);
        AiProvider provider = AiProvider.fromPreference(providerPref);
        SettingsRepository settingsRepository = new SettingsRepository(context);
        ProviderSettings providerSettings = settingsRepository.getProviderSettings(provider);
        if (!providerSettings.enabled) {
            callback.onError("Selected provider is disabled in settings.");
            return;
        }

        AiResolvedConfig resolved = AiConfigResolver.resolve(
                provider,
                providerSettings,
                settingsRepository.isAiWebSearchEnabled(),
                settingsRepository.isAiCitationsRequired());
        try {
            if (provider == AiProvider.ANTHROPIC) {
                new AnthropicProviderClient().stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.GEMINI) {
                new GeminiProviderClient().stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.DEEPSEEK) {
                new OpenAiCompatibleProviderClient(
                    resolved.apiKey,
                    resolved.model,
                    resolved.endpointUrl, "DeepSeek",
                    AttachmentMode.PORTABLE_TEXT_ONLY
                ).stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.XAI) {
                new OpenAiCompatibleProviderClient(
                        resolved.apiKey,
                        resolved.model,
                        resolved.endpointUrl, "xAI",
                        openAiCompatibleAttachmentMode("xAI", resolved.model)
                ).stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.GROQ) {
                new OpenAiCompatibleProviderClient(
                        resolved.apiKey,
                        resolved.model,
                        resolved.endpointUrl, "Groq",
                        openAiCompatibleAttachmentMode("Groq", resolved.model)
                ).stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.MISTRAL) {
                new OpenAiCompatibleProviderClient(
                        resolved.apiKey,
                        resolved.model,
                        resolved.endpointUrl, "Mistral",
                        openAiCompatibleAttachmentMode("Mistral", resolved.model)
                ).stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.PERPLEXITY) {
                new OpenAiCompatibleProviderClient(
                        resolved.apiKey,
                        resolved.model,
                        resolved.endpointUrl, "Perplexity",
                        openAiCompatibleAttachmentMode("Perplexity", resolved.model)
                ).stream(context, messages, profileContextText, callback);
            } else if (provider == AiProvider.OPENROUTER) {
                new OpenAiCompatibleProviderClient(
                    resolved.apiKey,
                    resolved.model,
                    resolved.endpointUrl, "OpenRouter",
                    openAiCompatibleAttachmentMode("OpenRouter", resolved.model)
                ).stream(context, messages, profileContextText, callback);
            } else {
                new OpenAiProviderClient().stream(context, messages, profileContextText, callback);
            }
        } catch (Exception e) {
            callback.onError(AiDebugMetadata.sanitizeError(e.getMessage()));
        }
    }

    public static void streamOpenAiCompatibleResponse(
            Context context,
            List<ChatMessage> messages, String profileContextText,
            StreamCallback callback, String apiKey, String model, String endpointUrl, String providerName,
            AttachmentMode attachmentMode) throws Exception {
            
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
        }, callback);
    }

    public static void streamAnthropicResponse(Context context, List<ChatMessage> messages, String profileContextText, StreamCallback callback) throws Exception {
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
        }, callback);
    }

    public static void streamGeminiResponse(Context context, List<ChatMessage> messages, String profileContextText, StreamCallback callback) throws Exception {
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
                    // Remove "data: " prefix
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
        }, callback);
    }

    private static String buildInstructions(String profileContextText, String provider) {
        // The developer context is a private command contract. /context exposes only the user-approved
        // profile context and must never echo this private section back to the user.
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

            String content = msg.getContent() == null ? "" : msg.getContent();
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
        if (mode == AttachmentMode.GEMINI_NATIVE_MULTIMODAL) return canGeminiInline(msg);
        if (mode == AttachmentMode.OPENAI_NATIVE_IMAGE || mode == AttachmentMode.ANTHROPIC_NATIVE_IMAGE) {
            return msg.hasImageAttachment();
        }
        return false;
    }

    private static boolean canGeminiInline(ChatMessage msg) {
        String mime = normalizedMime(msg);
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

    private static ChatMessage copyWithoutAttachment(ChatMessage msg, String content) {
        return new ChatMessage(msg.getId(), content, msg.isSent(), msg.getCreatedAt());
    }

    private static Object buildOpenAiContent(ChatMessage msg, String text) throws Exception {
        if (msg == null || !msg.hasAttachment()) return text;
        if (!msg.hasImageAttachment()) return text + attachmentTextFallback(msg);
        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "text").put("text", text));
        content.put(new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject().put("url", dataUri(msg))));
        return content;
    }

    private static Object buildAnthropicContent(ChatMessage msg, String text) throws Exception {
        if (msg == null || !msg.hasAttachment()) return text;
        if (!msg.hasImageAttachment()) return text + attachmentTextFallback(msg);
        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "text").put("text", text));
        content.put(new JSONObject()
                .put("type", "image")
                .put("source", new JSONObject()
                        .put("type", "base64")
                        .put("media_type", msg.getAttachmentMimeType())
                        .put("data", msg.getAttachmentBase64())));
        return content;
    }

    private static JSONArray buildGeminiParts(ChatMessage msg, String text) throws Exception {
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", text));
        if (msg != null && msg.hasAttachment()) {
            parts.put(new JSONObject().put("inline_data", new JSONObject()
                    .put("mime_type", msg.getAttachmentMimeType())
                    .put("data", msg.getAttachmentBase64())));
        }
        return parts;
    }

    private static String dataUri(ChatMessage msg) {
        return "data:" + msg.getAttachmentMimeType() + ";base64," + msg.getAttachmentBase64();
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

    private static String safeAttachmentName(ChatMessage msg) {
        String name = msg == null ? "" : msg.getAttachmentName();
        return name == null || name.trim().isEmpty() ? "attachment" : name.trim();
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

    private static void postStream(String url, JSONObject body, RequestHeaders headers, String label, LineProcessor processor, StreamCallback callback) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(60_000);
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
            // We use char-by-char read because depending on the AI endpoint, chunk borders may differ
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


