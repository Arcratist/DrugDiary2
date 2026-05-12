package brettdansmith.drugdiary.data.profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Schema constants and safe JSON accessors for encrypted profile vault data.
 *
 * All profile data is stored as one encrypted JSON document per active profile.
 * Fragments should use repositories/helpers instead of mutating nested objects by hand.
 */
public final class ProfileJson {
    public static final int SCHEMA_VERSION = 2;

    public static final String KEY_SCHEMA_VERSION = "schema_version";
    public static final String KEY_PROFILE = "profile";
    public static final String KEY_MEDICAL = "medical";
    public static final String KEY_PRIVACY = "privacy";
    public static final String KEY_TRACKERS = "trackers";
    public static final String KEY_MEDICATIONS = "medications";
    public static final String KEY_MEDICATION_DOSE_LOGS = "medication_dose_logs";
    public static final String KEY_MEDICATION_SCHEDULES = "medication_schedules";
    public static final String KEY_MEDICATION_INVENTORY = "medication_inventory";
    public static final String KEY_LOGS = "logs";
    public static final String KEY_DIARY_ENTRIES = "diary_entries";
    public static final String KEY_MOOD_CHECKINS = "mood_checkins";
    public static final String KEY_SYMPTOM_LOGS = "symptom_logs";
    public static final String KEY_SLEEP_LOGS = "sleep_logs";
    public static final String KEY_REAGENT_TESTS = "reagent_tests";
    public static final String KEY_REMINDERS = "reminders";
    public static final String KEY_ASSISTANT_CHATS = "assistant_chats";
    public static final String KEY_ASSISTANT_ACTIVE_CHAT_ID = "assistant_active_chat_id";
    public static final String KEY_ASSISTANT_PROFILES = "assistant_profiles";
    public static final String KEY_DRUG_DATABASE_CACHE = "drug_database_cache";
    public static final String KEY_PUBCHEM_CACHE = "pubchem_cache";

    public static final String PROFILE_NAME = "name";
    public static final String PROFILE_AGE = "age";
    public static final String PROFILE_SEX = "sex";
    public static final String PROFILE_WEIGHT_KG = "weight_kg";
    public static final String PROFILE_HEIGHT_CM = "height_cm";
    public static final String PROFILE_BLOOD_TYPE = "blood_type";
    public static final String PROFILE_LOCATION = "location";
    public static final String PROFILE_BIO = "bio";
    public static final String PROFILE_AVATAR = "avatar";
    public static final String PROFILE_AVATAR_URI = "avatar_uri";

    private ProfileJson() {
    }

    public static JSONObject emptyProfile(String name) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION);

        JSONObject profile = new JSONObject();
        profile.put(PROFILE_NAME, name);
        profile.put(PROFILE_AGE, "");
        profile.put(PROFILE_SEX, "");
        profile.put(PROFILE_WEIGHT_KG, 0);
        profile.put(PROFILE_HEIGHT_CM, 0);
        profile.put(PROFILE_BLOOD_TYPE, "");
        profile.put(PROFILE_LOCATION, "");
        profile.put(PROFILE_BIO, "");
        root.put(KEY_PROFILE, profile);

        JSONObject medical = new JSONObject();
        medical.put("allergies", "");
        medical.put("conditions", "");
        medical.put("active_medications", "");
        medical.put("emergency_note", "");
        root.put(KEY_MEDICAL, medical);

        JSONObject privacy = new JSONObject();
        privacy.put("share_profile", false);
        privacy.put("share_biometrics", false);
        privacy.put("share_meds", false);
        privacy.put("share_logs", false);
        privacy.put("show_on_dashboard", true);
        root.put(KEY_PRIVACY, privacy);

        JSONObject trackers = new JSONObject();
        trackers.put(KEY_MEDICATIONS, new JSONArray());
        trackers.put(KEY_MEDICATION_DOSE_LOGS, new JSONArray());
        trackers.put(KEY_MEDICATION_SCHEDULES, new JSONArray());
        trackers.put(KEY_MEDICATION_INVENTORY, new JSONArray());
        trackers.put(KEY_LOGS, new JSONArray());
        trackers.put(KEY_DIARY_ENTRIES, new JSONArray());
        trackers.put(KEY_MOOD_CHECKINS, new JSONArray());
        trackers.put(KEY_SYMPTOM_LOGS, new JSONArray());
        trackers.put(KEY_SLEEP_LOGS, new JSONArray());
        trackers.put(KEY_REAGENT_TESTS, new JSONArray());
        root.put(KEY_TRACKERS, trackers);
        root.put(KEY_REMINDERS, new JSONArray());
        root.put(KEY_ASSISTANT_CHATS, new JSONArray());
        root.put(KEY_ASSISTANT_ACTIVE_CHAT_ID, "");
        root.put(KEY_ASSISTANT_PROFILES, new JSONArray());
        return root;
    }

    public static JSONObject object(JSONObject parent, String key) throws JSONException {
        JSONObject object = parent.optJSONObject(key);
        if (object == null) {
            object = new JSONObject();
            parent.put(key, object);
        }
        return object;
    }

    public static JSONArray array(JSONObject parent, String key) throws JSONException {
        JSONArray array = parent.optJSONArray(key);
        if (array == null) {
            array = new JSONArray();
            parent.put(key, array);
        }
        return array;
    }
}

