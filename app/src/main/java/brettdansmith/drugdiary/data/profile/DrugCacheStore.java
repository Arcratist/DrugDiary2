package brettdansmith.drugdiary.data.profile;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import brettdansmith.drugdiary.security.UserSession;

/**
 * Manages storage for drug database lookups.
 * This data is public (FDA, Wikipedia, etc.) and does not contain sensitive user information,
 * so it is stored unencrypted for performance and to prevent OOM errors during encryption.
 */
public final class DrugCacheStore {
    private static final String TAG = "DrugCacheStore";

    private DrugCacheStore() {}

    public static void saveDrugCache(Context context, JSONObject data) {
        UserSession session = UserSession.getInstance();
        if (!session.isActive()) return;

        try {
            String profileName = session.getProfileName();
            File file = getCacheFile(context, profileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data.toString().getBytes(StandardCharsets.UTF_8));
            }
            session.setCachedDrugData(data);
        } catch (Exception e) {
            Log.e(TAG, "Error saving drug cache", e);
        }
    }

    public static JSONObject loadDrugCache(Context context) {
        UserSession session = UserSession.getInstance();
        if (!session.isActive()) return new JSONObject();

        try {
            if (session.getCachedDrugData() != null) {
                return session.getCachedDrugData();
            }

            String profileName = session.getProfileName();
            File file = getCacheFile(context, profileName);
            if (!file.exists()) {
                JSONObject created = new JSONObject();
                session.setCachedDrugData(created);
                return created;
            }

            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int ignored = fis.read(bytes);
            }

            JSONObject data = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            session.setCachedDrugData(data);
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error loading drug cache", e);
            return new JSONObject();
        }
    }

    public static boolean deleteCache(Context context, String profileName) {
        File file = getCacheFile(context, profileName);
        return !file.exists() || file.delete();
    }

    private static File getCacheFile(Context context, String profileName) {
        return new File(context.getFilesDir(), "drug_cache_" + safeFilePart(profileName) + ".json");
    }

    private static String safeFilePart(String profileName) {
        return profileName == null ? "unknown" : profileName.toLowerCase(Locale.US).replaceAll("[^a-z0-9._-]", "_");
    }
}
