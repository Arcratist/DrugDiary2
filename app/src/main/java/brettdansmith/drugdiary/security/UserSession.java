package brettdansmith.drugdiary.security;

import org.json.JSONObject;
import javax.crypto.spec.SecretKeySpec;

/**
 * Memory-only boundary for an unlocked profile.
 *
 * The encrypted vault key is derived after PIN verification and kept only for the
 * active process session. Ending the session drops the key and cached decrypted JSON,
 * which is why repositories return no profile data when this session is inactive.
 */
public class UserSession {
    private static UserSession instance;
    private SecretKeySpec sessionKey;
    private String profileName;
    private JSONObject cachedData;
    private JSONObject cachedDrugData;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void startSession(String profileName, SecretKeySpec key) {
        this.profileName = profileName;
        this.sessionKey = key;
        this.cachedData = null;
        this.cachedDrugData = null;
    }

    public void endSession() {
        this.sessionKey = null;
        this.profileName = null;
        this.cachedData = null;
        this.cachedDrugData = null;
    }

    public SecretKeySpec getSessionKey() {
        return sessionKey;
    }

    public String getProfileName() {
        return profileName;
    }

    public JSONObject getCachedData() {
        return cachedData;
    }

    public void setCachedData(JSONObject data) {
        this.cachedData = data;
    }

    public JSONObject getCachedDrugData() {
        return cachedDrugData;
    }

    public void setCachedDrugData(JSONObject data) {
        this.cachedDrugData = data;
    }

    public boolean isActive() {
        return sessionKey != null;
    }
}
