package brettdansmith.drugdiary.data.profile;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Migration support removed. Fresh installs only.
 */
public final class ProfileMigrations {
    private ProfileMigrations() {}

    public static JSONObject migrateIfNeeded(JSONObject data, String profileName) throws JSONException {
        // No migrations performed in fresh-install environment.
        return data;
    }
}
