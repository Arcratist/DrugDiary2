package brettdansmith.drugdiary.ui.assistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.json.JSONArray;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AssistantViewModel extends ViewModel {
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final ExecutorService networkExecutor = Executors.newCachedThreadPool();
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object streamLock = new Object();
    private final AtomicInteger conversationRevision = new AtomicInteger();
    private JSONArray chatSessions = new JSONArray();
    private String activeChatId = "";
    private boolean chatStateLoaded;
    private boolean assistantOpened;
    private boolean streaming;
    private String streamingText = "";
    private String requestedChatId = "";
    private String initialPrompt = "";
    private Context appContext;

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messageList) {
        if (messageList == null) {
            messages.setValue(new ArrayList<>());
        } else {
            messages.setValue(new ArrayList<>(messageList));
        }
    }

    public void setApplicationContext(Context context) {
        if (context != null) appContext = context.getApplicationContext();
    }

    public Context getApplicationContext() {
        return appContext;
    }

    public void markAssistantOpened() {
        assistantOpened = true;
    }

    public void addMessage(ChatMessage message) {
        List<ChatMessage> currentMessages = messages.getValue();
        List<ChatMessage> nextMessages = currentMessages == null ? new ArrayList<>() : new ArrayList<>(currentMessages);
        nextMessages.add(message);
        messages.setValue(nextMessages);
    }
    
    public void setInitialPrompt(String prompt) {
        this.initialPrompt = prompt;
    }

    public String consumeInitialPrompt() {
        String prompt = initialPrompt;
        initialPrompt = "";
        return prompt;
    }

    // ... (rest of the ViewModel is unchanged)
    public void updateMessage(String messageId, String content) {
        List<ChatMessage> currentMessages = messages.getValue();
        if (currentMessages == null || messageId == null) return;
        
        List<ChatMessage> nextMessages = new ArrayList<>(currentMessages);
        boolean found = false;
        for (ChatMessage m : nextMessages) {
            if (messageId.equals(m.getId())) {
                m.setContent(content);
                found = true;
                break;
            }
        }
        if (found) {
            messages.setValue(nextMessages);
        }
    }

    public void truncateAfter(String messageId) {
        List<ChatMessage> currentMessages = messages.getValue();
        if (currentMessages == null || messageId == null) return;
        int index = -1;
        for (int i = 0; i < currentMessages.size(); i++) {
            if (messageId.equals(currentMessages.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index < currentMessages.size() - 1) {
            messages.setValue(new ArrayList<>(currentMessages.subList(0, index + 1)));
        }
    }

    public void deleteMessage(String messageId) {
        List<ChatMessage> currentMessages = messages.getValue();
        if (currentMessages == null || messageId == null) return;
        List<ChatMessage> nextMessages = new ArrayList<>();
        for (ChatMessage m : currentMessages) {
            if (!messageId.equals(m.getId())) {
                nextMessages.add(m);
            }
        }
        messages.setValue(nextMessages);
    }

    public void deleteLastUserTurn(String messageId) {
        List<ChatMessage> currentMessages = messages.getValue();
        if (currentMessages == null || messageId == null) return;
        int index = -1;
        for (int i = 0; i < currentMessages.size(); i++) {
            if (messageId.equals(currentMessages.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            messages.setValue(new ArrayList<>(currentMessages.subList(0, index)));
        }
    }

    public boolean isLastUserMessage(String messageId) {
        List<ChatMessage> list = messages.getValue();
        if (list == null || list.isEmpty()) return false;
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessage m = list.get(i);
            if (m.isSent()) return messageId.equals(m.getId());
        }
        return false;
    }

    public void clear() {
        messages.setValue(new ArrayList<>());
        chatSessions = new JSONArray();
        activeChatId = "";
        chatStateLoaded = false;
        assistantOpened = false;
        clearStreaming();
        requestedChatId = "";
    }

    public void requestOpenChat(String chatId) {
        requestedChatId = chatId == null ? "" : chatId;
    }

    public String consumeRequestedChatId() {
        String value = requestedChatId;
        requestedChatId = "";
        return value == null ? "" : value;
    }

    public int currentRevision() {
        return conversationRevision.get();
    }

    public int incrementRevision() {
        return conversationRevision.incrementAndGet();
    }

    public void beginStreaming() {
        synchronized (streamLock) {
            streaming = true;
            streamingText = "";
        }
    }

    public void appendStreamingText(String text) {
        if (text == null || text.isEmpty()) return;
        synchronized (streamLock) {
            streamingText += text;
        }
    }

    public boolean isStreaming() {
        synchronized (streamLock) {
            return streaming;
        }
    }

    public String getStreamingText() {
        synchronized (streamLock) {
            return streamingText;
        }
    }

    public void finishStreaming() {
        synchronized (streamLock) {
            streaming = false;
        }
    }

    public void clearStreaming() {
        synchronized (streamLock) {
            streaming = false;
            streamingText = "";
        }
    }

    public void postToMain(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public boolean isChatStateLoaded() {
        return chatStateLoaded;
    }

    public JSONArray getChatSessions() {
        return chatSessions;
    }

    public String getActiveChatId() {
        return activeChatId;
    }

    public void setChatState(JSONArray sessions, String activeChatId) {
        this.chatSessions = sessions == null ? new JSONArray() : sessions;
        this.activeChatId = activeChatId == null ? "" : activeChatId;
        this.chatStateLoaded = true;
    }

    public ExecutorService getNetworkExecutor() {
        return networkExecutor;
    }

    public ExecutorService getDiskExecutor() {
        return diskExecutor;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        networkExecutor.shutdownNow();
        diskExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
