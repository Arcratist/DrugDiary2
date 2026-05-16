package brettdansmith.drugdiary.ui.assistant;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brettdansmith.drugdiary.assistant.AssistantContextBuilder;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.domain.assistant.AssistantCommandParser;
import brettdansmith.drugdiary.domain.assistant.AssistantCommandRegistry;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.domain.service.ServiceLocator;
import brettdansmith.drugdiary.network.ai.AssistantApiClient;

public class AssistantViewModel extends ViewModel {
    public static final class CommandAutocompleteOption {
        public final String label;
        public final String insertText;

        public CommandAutocompleteOption(String label, String insertText) {
            this.label = label;
            this.insertText = insertText;
        }
    }

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<JSONArray> chatSessionsData = new MutableLiveData<>(new JSONArray());
    private final MutableLiveData<String> activeChatIdData = new MutableLiveData<>("");
    private final MutableLiveData<String> activeChatTitleData = new MutableLiveData<>("New Chat");
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> streamingLiveData = new MutableLiveData<>(false);

    private final ExecutorService networkExecutor = Executors.newCachedThreadPool();
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger conversationRevision = new AtomicInteger();
    
    private final Object streamLock = new Object();
    private String streamingText = "";
    private String initialPrompt = "";
    private Context appContext;
    private String activeChatId = "";
    private JSONArray chatSessions = new JSONArray();
    private AssistantCommandRegistry commandRegistry;

    public AssistantViewModel(ServiceLocator serviceLocator) {
        this.appContext = serviceLocator.getContext();
        this.commandRegistry = new AssistantCommandRegistry(appContext, new AssistantCommandRegistry.UiActions() {
            @Override
            public void createNewChat(String title) {
                AssistantViewModel.this.createNewChat();
            }
        });
        loadOrInitializeChatState();
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<JSONArray> getChatSessions() { return chatSessionsData; }
    public LiveData<String> getActiveChatId() { return activeChatIdData; }
    public LiveData<String> getActiveChatTitle() { return activeChatTitleData; }
    public JSONArray getChatSessionsRaw() { return chatSessions; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }
    public LiveData<Boolean> isStreaming() { return streamingLiveData; }

    public void setApplicationContext(Context context) {
        if (context != null) this.appContext = context.getApplicationContext();
    }

    public void loadOrInitializeChatState() {
        if (appContext == null) return;
        loadingLiveData.setValue(true);
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                if (data != null) {
                    chatSessions = ProfileJson.array(data, ProfileJson.KEY_ASSISTANT_CHATS);
                    activeChatId = data.optString(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, "");
                    
                    postToMain(() -> {
                        JSONArray filteredSessions = filterBlankSessions(chatSessions);
                        chatSessionsData.setValue(filteredSessions);
                        activeChatIdData.setValue(activeChatId);
                        if (!activeChatId.isEmpty()) {
                            loadMessagesForChat(activeChatId);
                        } else {
                            createNewChat();
                        }
                    });
                }
            } catch (Exception e) {
                postToMain(() -> errorLiveData.setValue("Failed to load history: " + e.getMessage()));
            } finally {
                postToMain(() -> loadingLiveData.setValue(false));
            }
        });
    }

    private void loadMessagesForChat(String chatId) {
        JSONObject session = findSession(chatId);
        if (session != null) {
            JSONArray msgsJson = session.optJSONArray("messages");
            List<ChatMessage> list = messagesFromJson(msgsJson);
            messages.setValue(list);
            activeChatId = chatId;
            activeChatIdData.setValue(chatId);
            
            boolean isPrivate = session.optBoolean("private_session", false);
            activeChatTitleData.setValue(session.optString("title", isPrivate ? "Private Chat" : "New Chat"));
        }
    }

    public void createNewChat(boolean isPrivate) {
        String id = UUID.randomUUID().toString();
        activeChatId = id;
        messages.setValue(new ArrayList<>());
        activeChatIdData.setValue(id);
        
        String initialTitle = isPrivate ? "Private Chat" : "New Chat";
        activeChatTitleData.setValue(initialTitle);
        
        try {
            JSONObject session = new JSONObject();
            session.put("id", id);
            session.put("title", initialTitle);
            session.put("created_at", System.currentTimeMillis());
            session.put("messages", new JSONArray());
            session.put("private_session", isPrivate);
            
            chatSessions.put(session);
            // We update chatSessionsData immediately if it's private to ensure persistence in history
            if (isPrivate) {
                chatSessionsData.setValue(filterBlankSessions(chatSessions));
            }
            persistChatState();
        } catch (Exception ignored) {}
    }

    public void createNewChat() {
        createNewChat(false);
    }

    public void switchChat(String chatId) {
        if (chatId.equals(activeChatId)) return;
        loadMessagesForChat(chatId);
        persistChatState();
    }

    public void deleteChat(String chatId) {
        JSONArray next = new JSONArray();
        for (int i = 0; i < chatSessions.length(); i++) {
            JSONObject s = chatSessions.optJSONObject(i);
            if (s != null && !chatId.equals(s.optString("id"))) {
                next.put(s);
            }
        }
        chatSessions = next;
        chatSessionsData.setValue(chatSessions);
        if (chatId.equals(activeChatId)) {
            if (chatSessions.length() > 0) {
                String nextId = firstSessionId(chatSessions);
                if (!nextId.isEmpty()) {
                    switchChat(nextId);
                } else {
                    createNewChat();
                }
            } else {
                createNewChat();
            }
        }
        persistChatState();
    }

    public void sendMessage(String content, List<ChatMessage.PendingAttachment> attachments) {
        if (content.trim().isEmpty() && (attachments == null || attachments.isEmpty())) return;
        
        List<ChatMessage> current = new ArrayList<>(messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        ChatMessage userMsg = new ChatMessage(UUID.randomUUID().toString(), content, true, System.currentTimeMillis());
        if (attachments != null) {
            for (ChatMessage.PendingAttachment attachment : attachments) {
                if (attachment != null) userMsg.addAttachment(attachment.name, attachment.mimeType, attachment.base64);
            }
        }
        current.add(userMsg);
        messages.setValue(current);
        
        if (current.size() == 1 || (current.size() == 2 && !current.get(0).isSent())) {
            updateChatTitle(userMsg);
        }

        if (content.startsWith("/")) {
            executeLocalCommand(content);
        } else {
            requestAiResponse();
        }
    }

    private void executeLocalCommand(String command) {
        String result = commandRegistry.executeUnified(command);
        if (!result.isEmpty()) {
            addMessage(new ChatMessage(ChatAdapter.COMMAND_PREFIX + " " + result, false));
            persistChatState();
        }
    }

    private void requestAiResponse() {
        if (appContext == null) return;

        final int revision = conversationRevision.incrementAndGet();
        ChatMessage loadingMsg = new ChatMessage(UUID.randomUUID().toString(), " ", false, System.currentTimeMillis());
        addMessage(loadingMsg);
        
        streamingLiveData.setValue(true);
        synchronized (streamLock) { streamingText = ""; }

        List<ChatMessage> snapshot = new ArrayList<>(messages.getValue());
        if (!snapshot.isEmpty()) snapshot.remove(snapshot.size() - 1);

        networkExecutor.execute(() -> {
            try {
                boolean isPrivate = isPrivateSession(activeChatId);
                JSONObject data = isPrivate ? new JSONObject() : EncryptedProfileStore.loadProfileData(appContext);
                String contextText = AssistantContextBuilder.buildPlainText(appContext, data, snapshot);

                StringBuilder fullResponse = new StringBuilder();

                AssistantApiClient.streamAssistantResponse(appContext, snapshot, contextText, new AssistantApiClient.StreamCallback() {
                    @Override
                    public void onChunk(String text) {
                        if (revision != conversationRevision.get()) return;
                        synchronized (streamLock) { streamingText += text; }
                        fullResponse.append(text);
                        postToMain(() -> updateMessage(loadingMsg.getId(), getStreamingText()));
                    }

                    @Override
                    public void onDone() {
                        if (revision != conversationRevision.get()) return;
                        postToMain(() -> {
                            streamingLiveData.setValue(false);
                            handleAiToolCalls(fullResponse.toString());
                            persistChatState();
                            AssistantNotificationController.notifyReplyFinished(appContext, activeChatId);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (revision != conversationRevision.get()) return;
                        postToMain(() -> {
                            streamingLiveData.setValue(false);
                            updateMessage(loadingMsg.getId(), "[[system-message]] Error: " + error);
                            persistChatState();
                        });
                    }
                });
            } catch (Exception e) {
                postToMain(() -> {
                    streamingLiveData.setValue(false);
                    updateMessage(loadingMsg.getId(), "[[system-message]] Critical Error: " + e.getMessage());
                });
            }
        });
    }

    private void handleAiToolCalls(String response) {
        Pattern pattern = Pattern.compile("\\[\\[execute:(.*?)\\]\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        boolean found = false;
        while (matcher.find()) {
            String command = matcher.group(1).trim();
            String result = commandRegistry.executeUnified(command);
            if (!result.isEmpty()) {
                addMessage(new ChatMessage(ChatAdapter.COMMAND_PREFIX + " " + result, false));
                found = true;
            }
        }
        if (found) {
            persistChatState();
        }
    }

    private void updateChatTitle(ChatMessage message) {
        if (isPrivateSession(activeChatId)) return; // Keep "Private Chat" title

        String raw = message == null ? "" : (message.getContent() == null ? "" : message.getContent().trim());
        String title;
        if (!raw.isEmpty()) {
            title = raw.length() > 30 ? raw.substring(0, 27) + "..." : raw;
        } else if (message != null && message.hasAttachment()) {
            String attachmentName = message.getAttachmentName();
            if (attachmentName == null || attachmentName.trim().isEmpty()) attachmentName = "Attachment";
            title = "📎 " + attachmentName;
            if (title.length() > 30) title = title.substring(0, 27) + "...";
        } else {
            title = "New Chat";
        }
        JSONObject session = findSession(activeChatId);
        if (session != null) {
            try {
                session.put("title", title);
                activeChatTitleData.postValue(title);
                chatSessionsData.postValue(filterBlankSessions(chatSessions));
            } catch (Exception ignored) {}
        }
    }

    private JSONArray filterBlankSessions(JSONArray source) {
        JSONArray filtered = new JSONArray();
        if (source == null) return filtered;
        for (int i = 0; i < source.length(); i++) {
            JSONObject s = source.optJSONObject(i);
            if (s != null) {
                String id = s.optString("id");
                JSONArray msgs = s.optJSONArray("messages");
                boolean isPrivate = s.optBoolean("private_session", false);
                // Include if it has messages OR if it's the current active session OR if it's private
                if ((msgs != null && msgs.length() > 0) || id.equals(activeChatId) || isPrivate) {
                    filtered.put(s);
                }
            }
        }
        return filtered;
    }

    private void persistChatState() {
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                if (data == null) return;
                
                JSONObject currentSession = findSession(activeChatId);
                if (currentSession != null) {
                    currentSession.put("messages", messagesToJson(messages.getValue()));
                }
                
                JSONArray sessionsToSave = filterBlankSessions(chatSessions);
                data.put(ProfileJson.KEY_ASSISTANT_CHATS, sessionsToSave);
                data.put(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, activeChatId);
                EncryptedProfileStore.saveProfileData(appContext, data);
                
                postToMain(() -> chatSessionsData.setValue(sessionsToSave));
            } catch (Exception ignored) {}
        });
    }

    private boolean isPrivateSession(String id) {
        JSONObject s = findSession(id);
        return s != null && s.optBoolean("private_session", false);
    }

    private JSONObject findSession(String id) {
        if (id == null) return null;
        for (int i = 0; i < chatSessions.length(); i++) {
            JSONObject s = chatSessions.optJSONObject(i);
            if (s != null && id.equals(s.optString("id"))) return s;
        }
        return null;
    }

    private JSONArray messagesToJson(List<ChatMessage> list) {
        JSONArray array = new JSONArray();
        if (list == null) return array;
        for (ChatMessage m : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", m.getId());
                o.put("content", m.getContent());
                o.put("is_sent", m.isSent());
                o.put("created_at", m.getCreatedAt());
                if (m.hasAttachment()) {
                    JSONArray attachmentsJson = new JSONArray();
                    for (ChatMessage.Attachment attachment : m.getAttachments()) {
                        JSONObject a = new JSONObject();
                        a.put("name", attachment.name);
                        a.put("mime", attachment.mimeType);
                        a.put("base64", attachment.base64);
                        attachmentsJson.put(a);
                    }
                    o.put("attachments", attachmentsJson);
                }
                array.put(o);
            } catch (Exception ignored) {}
        }
        return array;
    }

    private List<ChatMessage> messagesFromJson(JSONArray array) {
        List<ChatMessage> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o != null) {
                ChatMessage m = new ChatMessage(
                    o.optString("id"),
                    o.optString("content"),
                    o.optBoolean("is_sent"),
                    o.optLong("created_at")
                );
                JSONArray attachments = o.optJSONArray("attachments");
                if (attachments != null) {
                    for (int j = 0; j < attachments.length(); j++) {
                        JSONObject a = attachments.optJSONObject(j);
                        if (a != null) {
                            m.addAttachment(a.optString("name"), a.optString("mime"), a.optString("base64"));
                        }
                    }
                } else if (o.has("att_base64")) {
                    m.setAttachment(o.optString("att_name"), o.optString("att_mime"), o.optString("att_base64"));
                }
                list.add(m);
            }
        }
        return list;
    }

    public void updateMessage(String id, String content) {
        List<ChatMessage> list = messages.getValue();
        if (list == null) return;
        for (ChatMessage m : list) {
            if (id.equals(m.getId())) {
                m.setContent(content);
                messages.setValue(new ArrayList<>(list));
                return;
            }
        }
    }

    public void addMessage(ChatMessage message) {
        List<ChatMessage> list = new ArrayList<>(messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        list.add(message);
        messages.setValue(list);
    }

    public void deleteMessage(String id) {
        List<ChatMessage> list = messages.getValue();
        if (list == null) return;
        List<ChatMessage> next = new ArrayList<>();
        int targetIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) return;
        for (int i = 0; i < targetIndex; i++) next.add(list.get(i));
        messages.setValue(next);
        conversationRevision.incrementAndGet();
        refreshTitleFromCurrentMessages();
        persistChatState();
    }

    public void editMessageAndReplay(String id, String updatedContent) {
        editMessageAndReplay(id, updatedContent, false);
    }

    public void editMessageAndReplay(String id, String updatedContent, boolean branchToNewChat) {
        editMessageAndReplay(id, updatedContent, branchToNewChat, false);
    }

    public void editMessageAndReplay(String id, String updatedContent, boolean branchToNewChat, boolean forcePrivateBranch) {
        editMessageAndReplay(id, updatedContent, branchToNewChat, forcePrivateBranch, null);
    }

    public void editMessageAndReplay(String id, String updatedContent, boolean branchToNewChat, boolean forcePrivateBranch, List<ChatMessage.Attachment> updatedAttachments) {
        List<ChatMessage> list = messages.getValue();
        if (list == null || updatedContent == null || updatedContent.trim().isEmpty()) return;
        int targetIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) return;
        ChatMessage target = list.get(targetIndex);
        if (!target.isSent()) return;

        List<ChatMessage> next = new ArrayList<>();
        for (int i = 0; i < targetIndex; i++) next.add(list.get(i));
        ChatMessage edited = new ChatMessage(target.getId(), updatedContent, true, target.getCreatedAt());
        if (updatedAttachments == null) {
            edited.setAttachments(target.getAttachments());
        } else {
            edited.setAttachments(updatedAttachments);
        }
        next.add(edited);

        if (branchToNewChat) {
            branchFromEditedMessages(next, forcePrivateBranch);
        } else {
            messages.setValue(next);
            conversationRevision.incrementAndGet();
            refreshTitleFromCurrentMessages();
            persistChatState();
        }

        if (!updatedContent.startsWith("/")) {
            requestAiResponse();
        } else {
            executeLocalCommand(updatedContent);
        }
    }

    public String getStreamingText() {
        synchronized (streamLock) { return streamingText; }
    }

    public void postToMain(Runnable r) { mainHandler.post(r); }

    public void setInitialPrompt(String prompt) {
        this.initialPrompt = prompt;
    }

    public String consumeInitialPrompt() {
        String p = initialPrompt;
        initialPrompt = "";
        return p;
    }

    public List<String> getAvailableCommands() {
        return commandRegistry.getAvailableCommands();
    }

    public List<CommandAutocompleteOption> getCommandAutocompleteOptions(String typed) {
        if (typed == null) return Collections.emptyList();
        String input = typed.trim();
        if (!input.startsWith("/")) return Collections.emptyList();

        String normalized = AssistantCommandParser.normalize(input);
        if (normalized.isEmpty()) normalized = input;
        String body = normalized.substring(1);
        int firstSpace = body.indexOf(' ');
        String commandToken = firstSpace >= 0 ? body.substring(0, firstSpace).toLowerCase(Locale.US) : body.toLowerCase(Locale.US);
        String payload = firstSpace >= 0 ? body.substring(firstSpace + 1).trim() : "";

        List<CommandAutocompleteOption> options = new ArrayList<>();
        if (firstSpace < 0) {
            for (AssistantCommandRegistry.CommandSpec spec : AssistantCommandRegistry.matchingSpecs("/" + commandToken)) {
                options.add(new CommandAutocompleteOption(spec.usage, "/" + spec.name + " "));
            }
            return limit(options, 10);
        }

        AssistantCommandRegistry.CommandSpec spec = AssistantCommandRegistry.specFor(commandToken);
        if (spec == null) return Collections.emptyList();

        if ("help".equals(spec.name)) {
            String prefix = payload.toLowerCase(Locale.US);
            for (AssistantCommandRegistry.CommandSpec cmd : AssistantCommandRegistry.allSpecs()) {
                if (!cmd.name.startsWith(prefix)) continue;
                options.add(new CommandAutocompleteOption("/help " + cmd.name + "  -  " + cmd.usageCases, "/help " + cmd.name));
            }
            return limit(options, 8);
        }

        if ("drugcache".equals(spec.name)) {
            addIfStartsWith(options, payload, "status", "/drugcache status");
            addIfStartsWith(options, payload, "clear", "/drugcache clear");
            addIfStartsWith(options, payload, "warm common", "/drugcache warm common");
            addIfStartsWith(options, payload, "warm recreational", "/drugcache warm recreational");
            return limit(options, 8);
        }

        if (!spec.supportsPayloadAutofill || appContext == null) return Collections.emptyList();
        for (String candidate : MedicationQueryResolver.suggestionsFor(appContext)) {
            if (!payload.isEmpty() && !candidate.toLowerCase(Locale.US).startsWith(payload.toLowerCase(Locale.US))) continue;
            options.add(new CommandAutocompleteOption("/" + spec.name + " " + candidate, "/" + spec.name + " " + candidate));
            if (options.size() >= 8) break;
        }
        return options;
    }

    public void requestOpenChat(String chatId) {
        if (chatId != null && !chatId.isEmpty()) switchChat(chatId);
    }

    public void clear() {
        messages.setValue(new ArrayList<>());
        chatSessions = new JSONArray();
        activeChatId = "";
        chatSessionsData.setValue(chatSessions);
        activeChatIdData.setValue("");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        networkExecutor.shutdownNow();
        diskExecutor.shutdownNow();
    }

    private String firstSessionId(JSONArray sessions) {
        if (sessions == null) return "";
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;
            String id = session.optString("id", "");
            if (!id.isEmpty()) return id;
        }
        return "";
    }

    private void addIfStartsWith(List<CommandAutocompleteOption> options, String typedPayload, String value, String insert) {
        String typed = typedPayload == null ? "" : typedPayload.toLowerCase(Locale.US);
        if (value.startsWith(typed)) {
            options.add(new CommandAutocompleteOption("/drugcache " + value, insert));
        }
    }

    private List<CommandAutocompleteOption> limit(List<CommandAutocompleteOption> options, int max) {
        if (options.size() <= max) return options;
        return new ArrayList<>(options.subList(0, max));
    }

    private void refreshTitleFromCurrentMessages() {
        if (isPrivateSession(activeChatId)) return;
        List<ChatMessage> list = messages.getValue();
        if (list == null || list.isEmpty()) {
            JSONObject session = findSession(activeChatId);
            if (session != null) {
                try {
                    session.put("title", "New Chat");
                    activeChatTitleData.setValue("New Chat");
                    chatSessionsData.setValue(filterBlankSessions(chatSessions));
                } catch (Exception ignored) {}
            }
            return;
        }
        for (ChatMessage msg : list) {
            if (msg != null && msg.isSent()) {
                updateChatTitle(msg);
                return;
            }
        }
    }

    private void branchFromEditedMessages(List<ChatMessage> branchMessages, boolean forcePrivateBranch) {
        if (branchMessages == null) return;
        JSONObject source = findSession(activeChatId);
        boolean isPrivate = forcePrivateBranch || (source != null && source.optBoolean("private_session", false));
        String sourceTitle = source == null ? "Chat" : source.optString("title", "Chat");
        String baseTitle = sourceTitle == null || sourceTitle.trim().isEmpty() ? "Chat" : sourceTitle.trim();
        String branchTitle = forcePrivateBranch ? (baseTitle + " (private branch)") : (baseTitle + " (branch)");

        String newId = UUID.randomUUID().toString();
        JSONObject session = new JSONObject();
        try {
            session.put("id", newId);
            session.put("title", branchTitle);
            session.put("created_at", System.currentTimeMillis());
            session.put("messages", messagesToJson(branchMessages));
            session.put("private_session", isPrivate);
            chatSessions.put(session);
        } catch (Exception ignored) {}

        activeChatId = newId;
        messages.setValue(new ArrayList<>(branchMessages));
        activeChatIdData.setValue(newId);
        activeChatTitleData.setValue(branchTitle);
        conversationRevision.incrementAndGet();
        chatSessionsData.setValue(filterBlankSessions(chatSessions));
        persistChatState();
    }
}
