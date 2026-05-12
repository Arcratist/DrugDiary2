package brettdansmith.drugdiary.data.reference;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import brettdansmith.drugdiary.data.profile.DrugCacheStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;

/**
 * Manages metadata for the public drug reference cache.
 */
public final class DrugReferenceCacheStore {
    private final Context appContext;

    public DrugReferenceCacheStore(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public int clear() throws Exception {
        JSONObject data = DrugCacheStore.loadDrugCache(appContext);
        int count = 0;
        
        JSONObject cache = data.optJSONObject(ProfileJson.KEY_DRUG_DATABASE_CACHE);
        if (cache != null) count += countEntries(cache);
        
        JSONObject pubChem = data.optJSONObject(ProfileJson.KEY_PUBCHEM_CACHE);
        if (pubChem != null) count += countEntries(pubChem);

        DrugCacheStore.saveDrugCache(appContext, new JSONObject());
        return count;
    }

    public String status() throws Exception {
        JSONObject data = DrugCacheStore.loadDrugCache(appContext);
        JSONObject cache = data.optJSONObject(ProfileJson.KEY_DRUG_DATABASE_CACHE);
        JSONObject pubChem = data.optJSONObject(ProfileJson.KEY_PUBCHEM_CACHE);
        
        return "Drug reference cache\n"
                + "- Aggregated source entries: " + countEntries(cache) + "\n"
                + "- PubChem entries: " + countEntries(pubChem) + "\n"
                + "- Scope: unencrypted public reference vault";
    }

    public JSONObject metadata(String source, String query, String status, JSONObject payload) throws Exception {
        JSONArray sections = new JSONArray();
        if (payload != null && payload.names() != null) {
            JSONArray names = payload.names();
            for (int i = 0; i < names.length(); i++) {
                sections.put(names.optString(i));
            }
        }
        return new JSONObject()
                .put("source", source)
                .put("query", query)
                .put("timestamp", System.currentTimeMillis())
                .put("status", status)
                .put("available_sections", sections);
    }

    private int countEntries(JSONObject object) {
        if (object == null) return 0;
        JSONArray names = object.names();
        if (names == null) return 0;
        int count = 0;
        for (int i = 0; i < names.length(); i++) {
            Object child = object.opt(names.optString(i));
            count += child instanceof JSONObject ? Math.max(1, countEntries((JSONObject) child)) : 1;
        }
        return count;
    }
}
