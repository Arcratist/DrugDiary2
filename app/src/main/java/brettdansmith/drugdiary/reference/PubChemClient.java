package brettdansmith.drugdiary.reference;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;

import brettdansmith.drugdiary.data.profile.DrugCacheStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.util.JsonUtils;

public final class PubChemClient {
    private static final long CACHE_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final int MAX_CACHE_ENTRIES = 20;
    private static final String BASE = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/";
    private static final String PROPERTIES = "/property/MolecularFormula,MolecularWeight,CanonicalSMILES,IsomericSMILES,IUPACName,InChIKey/JSON";
    private static final String SYNONYMS = "/synonyms/JSON";
    private static final String PUG_BASE = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/";
    private static final String PUG_VIEW_BASE = "https://pubchem.ncbi.nlm.nih.gov/rest/pug_view/data/compound/";

    private PubChemClient() {}

    public static JSONObject lookupCompound(Context context, String query) throws Exception {
        List<String> candidates = MedicationQueryResolver.candidatesFor(context, query);
        if (candidates.isEmpty()) throw new IllegalArgumentException("A medication or substance name is required.");
        Exception last = null;
        for (String candidate : candidates) {
            try {
                JSONObject result = lookupSingleCompound(context, candidate);
                result.put("original_query", query == null ? "" : query.trim());
                result.put("lookup_query", candidate);
                result.put("lookup_candidates", new JSONArray(candidates));
                return result;
            } catch (Exception e) {
                last = e;
            }
        }
        throw last == null ? new IllegalStateException("No PubChem result available for " + query) : last;
    }

    private static JSONObject lookupSingleCompound(Context context, String query) throws Exception {
        String clean = query == null ? "" : query.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Query required.");
        String key = normalizeKey(clean);
        
        JSONObject data = DrugCacheStore.loadDrugCache(context);
        JSONObject cache = JsonUtils.object(data, ProfileJson.KEY_PUBCHEM_CACHE);
        JSONObject cached = cache.optJSONObject(key);
        
        if (cached != null && System.currentTimeMillis() - cached.optLong("cached_at", 0) < CACHE_TTL_MS) {
            cached.put("cache_status", "hit");
            return cached;
        }

        JSONObject result = fetchCompoundBundle(clean);
        result.put("cached_at", System.currentTimeMillis());
        result.put("cache_status", "fresh");

        pruneCache(cache);
        cache.put(key, result);
        DrugCacheStore.saveDrugCache(context, data);
        return result;
    }

    private static void pruneCache(JSONObject cache) {
        JSONArray names = cache.names();
        if (names == null || names.length() < MAX_CACHE_ENTRIES) return;

        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            JSONObject entry = cache.optJSONObject(key);
            if (entry != null) {
                long time = entry.optLong("cached_at", 0);
                if (time < oldestTime) {
                    oldestTime = time;
                    oldestKey = key;
                }
            }
        }
        if (oldestKey != null) cache.remove(oldestKey);
    }

    public static String formatForAssistant(JSONObject compound) {
        JSONObject props = compound.optJSONObject("properties");
        if (props == null) props = new JSONObject();
        StringBuilder text = new StringBuilder();
        text.append("PubChem lookup (").append(compound.optString("cache_status", "unknown")).append(")\n");
        append(text, "IUPAC name", props.optString("IUPACName", ""));
        append(text, "Formula", props.optString("MolecularFormula", ""));
        append(text, "Molecular weight", props.optString("MolecularWeight", ""));
        return text.toString().trim();
    }

    public static String formatRawSections(JSONObject compound) {
        if (compound == null) return "No PubChem payload available.";
        try {
            return "```json\n" + compound.toString(2) + "\n```";
        } catch (Exception ignored) {
            return compound.toString();
        }
    }

    private static JSONObject fetchCompoundBundle(String query) throws Exception {
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name()).replace("+", "%20");
        JSONObject propertyRoot = readJson(BASE + encoded + PROPERTIES);
        JSONArray propertyTable = propertyRoot.getJSONObject("PropertyTable").getJSONArray("Properties");
        JSONObject properties = propertyTable.getJSONObject(0);
        int cid = properties.optInt("CID", 0);

        JSONObject result = new JSONObject();
        result.put("query", query);
        result.put("properties", properties);
        
        JSONArray available = new JSONArray();
        putOptional(result, available, "description", PUG_BASE + cid + "/description/JSON");
        result.put("available_sections", available);
        return result;
    }

    private static void putOptional(JSONObject result, JSONArray available, String key, String url) {
        try {
            result.put(key, readJson(url));
            available.put(key);
        } catch (Exception ignored) {}
    }

    private static JSONObject readJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        if (connection.getResponseCode() >= 400) throw new Exception("HTTP " + connection.getResponseCode());
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return new JSONObject(body.toString());
        }
    }

    private static String normalizeKey(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9._-]", "_");
    }

    private static void append(StringBuilder text, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            text.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }
}
