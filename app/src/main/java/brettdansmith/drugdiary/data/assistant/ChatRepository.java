package brettdansmith.drugdiary.data.assistant;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import brettdansmith.drugdiary.ui.assistant.ChatMessage;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;

/**
 * Encrypted, profile-local assistant chat history.
 * Closing a chat removes it from this vault-backed array so future AI requests cannot see it.
 */
public final class ChatRepository {
    private final Context appContext;

    public ChatRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public JSONObject loadProfileData() {
        return EncryptedProfileStore.loadProfileData(appContext);
    }

    public void saveSessions(JSONArray sessions, String activeChatId) throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        data.put(ProfileJson.KEY_ASSISTANT_CHATS, sessions);
        data.put(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, activeChatId == null ? "" : activeChatId);
        EncryptedProfileStore.saveProfileData(appContext, data);
    }

    public JSONArray messagesToJson(List<ChatMessage> messages) throws JSONException {
        JSONArray array = new JSONArray();
        for (ChatMessage msg : messages) {
            array.put(new JSONObject()
                    .put("content", msg.getContent())
                    .put("isSent", msg.isSent()));
        }
        return array;
    }
}

