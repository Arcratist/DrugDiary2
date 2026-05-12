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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import brettdansmith.drugdiary.data.profile.DrugCacheStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.util.JsonUtils;

/**
 * Client for public drug databases.
 * Uses unencrypted storage for public data to optimize memory and performance.
 */
public final class DrugDatabaseClient {
    private static final long CACHE_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final int MAX_CACHE_ENTRIES_PER_SOURCE = 50;
    private static final String RXNAV_BASE = "https://rxnav.nlm.nih.gov/REST/";
    private static final String OPENFDA_BASE = "https://api.fda.gov/drug/";
    private static final String CHEMBL_BASE = "https://www.ebi.ac.uk/chembl/api/data/";
    private static final String WIKI_BASE = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final String DAILYMED_BASE = "https://dailymed.nlm.nih.gov/dailymed/services/v2/";

    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern ALLOWED_DRUG_QUERY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 '\\-+/().,]{1,79}");

    private DrugDatabaseClient() {}

    public static boolean looksLikeDrugQuery(String query) {
        if (query == null) return false;
        String clean = query.trim();
        if (clean.length() < 2 || clean.length() > 80) return false;
        String lower = clean.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.contains("\n")) return false;
        if (lower.contains("{") || lower.contains("}") || lower.contains("[") || lower.contains("]") || lower.contains("<") || lower.contains(">")) return false;
        if (!HAS_LETTER.matcher(clean).matches()) return false;
        if (!ALLOWED_DRUG_QUERY.matcher(clean).matches()) return false;
        return true;
    }

    public static JSONObject lookupRxNorm(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        return cached(context, ProfileJson.KEY_DRUG_DATABASE_CACHE, "rxnorm", normalized, () -> fetchWithCandidates(context, normalized, DrugDatabaseClient::fetchRxNorm));
    }

    public static JSONObject lookupOpenFdaLabel(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        return cached(context, ProfileJson.KEY_DRUG_DATABASE_CACHE, "openfda_label", normalized, () -> fetchWithCandidates(context, normalized, candidate -> fetchOpenFda("label", candidate)));
    }

    public static JSONObject lookupOpenFdaEvent(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        return cached(context, ProfileJson.KEY_DRUG_DATABASE_CACHE, "openfda_event", normalized, () -> fetchWithCandidates(context, normalized, candidate -> fetchOpenFda("event", candidate)));
    }

    public static JSONObject lookupChembl(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        return cached(context, ProfileJson.KEY_DRUG_DATABASE_CACHE, "chembl", normalized, () -> fetchWithCandidates(context, normalized, DrugDatabaseClient::fetchChembl));
    }

    public static JSONObject lookupWikipedia(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        return cached(context, ProfileJson.KEY_DRUG_DATABASE_CACHE, "wikipedia", normalized, () -> fetchWithCandidates(context, normalized, DrugDatabaseClient::fetchWikipedia));
    }

    public static JSONObject lookupDailyMed(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        return cached(context, ProfileJson.KEY_DRUG_DATABASE_CACHE, "dailymed", normalized, () -> fetchWithCandidates(context, normalized, DrugDatabaseClient::fetchDailyMed));
    }

    public static JSONObject lookupAll(Context context, String query) throws Exception {
        String normalized = requireQuery(query);
        JSONObject result = new JSONObject();
        result.put("query", normalized);
        putLookup(result, "rxnorm", () -> lookupRxNorm(context, normalized));
        putLookup(result, "openfda_label", () -> lookupOpenFdaLabel(context, normalized));
        putLookup(result, "openfda_event", () -> lookupOpenFdaEvent(context, normalized));
        putLookup(result, "chembl", () -> lookupChembl(context, normalized));
        putLookup(result, "pubchem", () -> PubChemRepository.getCompoundByName(context, normalized));
        putLookup(result, "wikipedia", () -> lookupWikipedia(context, normalized));
        putLookup(result, "dailymed", () -> lookupDailyMed(context, normalized));
        result.put("cached_at", System.currentTimeMillis());
        return result;
    }

    private static JSONObject cached(Context context, String rootKey, String source, String query, JsonFetcher fetcher) throws Exception {
        JSONObject data = DrugCacheStore.loadDrugCache(context);
        JSONObject root = JsonUtils.object(data, rootKey);
        JSONObject sourceCache = JsonUtils.object(root, source);
        String key = normalizeKey(query);
        
        JSONObject cached = sourceCache.optJSONObject(key);
        if (cached != null && System.currentTimeMillis() - cached.optLong("cached_at", 0) < CACHE_TTL_MS) {
            cached.put("cache_status", "hit");
            return cached;
        }

        JSONObject result = fetcher.fetch();
        result.put("cached_at", System.currentTimeMillis());
        result.put("cache_status", "fresh");

        pruneCache(sourceCache);
        sourceCache.put(key, result);
        DrugCacheStore.saveDrugCache(context, data);
        return result;
    }

    private static void pruneCache(JSONObject sourceCache) {
        JSONArray names = sourceCache.names();
        if (names == null || names.length() < MAX_CACHE_ENTRIES_PER_SOURCE) return;
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            JSONObject entry = sourceCache.optJSONObject(key);
            if (entry != null) {
                long time = entry.optLong("cached_at", 0);
                if (time < oldestTime) {
                    oldestTime = time; oldestKey = key;
                }
            }
        }
        if (oldestKey != null) sourceCache.remove(oldestKey);
    }

    public static String formatSummary(JSONObject bundle) {
        if (bundle == null) return "No data available";
        StringBuilder text = new StringBuilder();
        String source = bundle.optString("source", "Drug database");
        String query = bundle.optString("query", bundle.optString("lookup_query", ""));
        text.append("### ").append(query.isEmpty() ? source : query).append("\n");
        text.append("*Source: ").append(source).append("*\n\n");
        if ("RxNorm/RxNav".equals(source)) appendRxNormInterpreted(text, bundle);
        else if ("openFDA drug labels".equals(source)) appendOpenFdaLabelInterpreted(text, bundle);
        else if ("openFDA adverse events".equals(source)) appendOpenFdaEventInterpreted(text, bundle);
        else if ("ChEMBL".equals(source)) appendChemblInterpreted(text, bundle);
        else if ("Wikipedia".equals(source)) appendWikipediaInterpreted(text, bundle);
        else if ("PubChem".equals(source)) appendPubChemInterpreted(text, bundle);
        else if ("DailyMed".equals(source)) appendDailyMedInterpreted(text, bundle);
        else appendAggregateInterpreted(text, bundle);
        return text.toString().trim();
    }

    public static String formatRaw(JSONObject payload) {
        if (payload == null) return "No raw payload available.";
        try {
            return "```json\n" + payload.toString(2) + "\n```";
        } catch (Exception ignored) {
            return payload.toString();
        }
    }

    private static void appendRxNormInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No RxNorm data found._\n"); return; }
        JSONObject properties = bundle.optJSONObject("properties");
        JSONObject prop = properties == null ? null : properties.optJSONObject("properties");
        if (prop != null) {
            text.append("**Standard Name:** ").append(prop.optString("name", "N/A")).append("\n");
            text.append("**Concept ID (RxCUI):** `").append(prop.optString("rxcui", "N/A")).append("`\n");
            String syn = prop.optString("synonym", "");
            if (!syn.isEmpty()) text.append("**Synonyms:** ").append(syn).append("\n");
        }
    }

    private static void appendOpenFdaLabelInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No FDA label data found._\n"); return; }
        JSONObject root = bundle.optJSONObject("results");
        JSONArray results = root == null ? null : root.optJSONArray("results");
        if (results == null || results.length() == 0) return;
        JSONObject first = results.optJSONObject(0);
        text.append("**Indications & Usage:**\n").append(getFdaField(first, "indications_and_usage")).append("\n\n");
        text.append("**Warnings:**\n").append(getFdaField(first, "warnings")).append("\n");
    }

    private static String getFdaField(JSONObject obj, String key) {
        JSONArray arr = obj.optJSONArray(key);
        if (arr == null || arr.length() == 0) return "_Information not available._";
        String val = arr.optString(0, "");
        if (val.length() > 500) val = val.substring(0, 497) + "...";
        return val;
    }

    private static void appendOpenFdaEventInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No adverse event reports found._\n"); return; }
        JSONObject root = bundle.optJSONObject("results");
        JSONArray results = root == null ? null : root.optJSONArray("results");
        if (results == null) return;
        text.append("**Found ").append(results.length()).append(" recent adverse event reports.**\n\n");
    }

    private static void appendChemblInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No ChEMBL data found._\n"); return; }
        JSONObject mol = bundle.optJSONObject("molecule");
        if (mol != null) {
            text.append("**Preferred Name:** ").append(mol.optString("pref_name", "N/A")).append("\n");
            text.append("**Max Clinical Phase:** ").append(mol.optString("max_phase", "0")).append("\n");
        }
    }

    private static void appendWikipediaInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No Wikipedia summary available._\n"); return; }
        JSONObject summary = bundle.optJSONObject("summary");
        if (summary != null) {
            text.append(summary.optString("extract", "")).append("\n\n");
        }
    }

    private static void appendPubChemInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No PubChem data found._\n"); return; }
        JSONObject props = bundle.optJSONObject("properties");
        if (props != null) {
            text.append("**Formula:** `").append(props.optString("MolecularFormula", "N/A")).append("`\n");
            text.append("**Weight:** ").append(props.optString("MolecularWeight", "N/A")).append(" g/mol\n");
        }
    }

    private static void appendDailyMedInterpreted(StringBuilder text, JSONObject bundle) {
        if (!isAvailable(bundle)) { text.append("_No DailyMed data found._\n"); return; }
        JSONObject spls = bundle.optJSONObject("spls");
        JSONObject data = spls == null ? null : spls.optJSONObject("data");
        JSONArray rows = data == null ? null : data.optJSONArray("spl");
        if (rows != null && rows.length() > 0) {
            text.append("**Available Labels:**\n");
            for (int i = 0; i < Math.min(2, rows.length()); i++) {
                text.append("- ").append(rows.optJSONObject(i).optString("title")).append("\n");
            }
        }
    }

    private static void appendAggregateInterpreted(StringBuilder text, JSONObject bundle) {
        text.append("### Comprehensive Data Analysis\n\n");
    }

    private static boolean isAvailable(JSONObject obj) {
        return obj != null && obj.optBoolean("available", true) && !obj.has("error_code");
    }

    private static JSONObject fetchRxNorm(String query) throws Exception {
        String encoded = encode(query);
        JSONObject result = new JSONObject();
        result.put("source", "RxNorm/RxNav");
        result.put("query", query);
        result.put("approximate_matches", readJson(RXNAV_BASE + "approximateTerm.json?term=" + encoded + "&maxEntries=8"));
        result.put("drug_groups", readJson(RXNAV_BASE + "drugs.json?name=" + encoded));
        String rxcui = firstRxCui(result);
        if (!rxcui.isEmpty()) {
            result.put("rxcui", rxcui);
            putOptional(result, "properties", RXNAV_BASE + "rxcui/" + encode(rxcui) + "/properties.json");
        }
        return result;
    }

    private static JSONObject fetchOpenFda(String type, String query) throws Exception {
        JSONObject result = new JSONObject();
        result.put("source", "label".equals(type) ? "openFDA drug labels" : "openFDA adverse events");
        result.put("query", query);
        String search = "label".equals(type) ? "openfda.brand_name:\"" + query + "\"+openfda.generic_name:\"" + query + "\"" : "patient.drug.medicinalproduct:\"" + query + "\"";
        result.put("results", readJson(OPENFDA_BASE + ("label".equals(type) ? "label" : "event") + ".json?search=" + encode(search) + "&limit=5"));
        return result;
    }

    private static JSONObject fetchChembl(String query) throws Exception {
        JSONObject result = new JSONObject();
        result.put("source", "ChEMBL");
        result.put("query", query);
        JSONObject search = readJson(CHEMBL_BASE + "molecule/search.json?q=" + encode(query) + "&limit=8");
        result.put("search", search);
        String chemblId = firstChemblId(search);
        if (!chemblId.isEmpty()) {
            result.put("molecule_chembl_id", chemblId);
            putOptional(result, "molecule", CHEMBL_BASE + "molecule/" + encode(chemblId) + ".json");
        }
        return result;
    }
    
    private static JSONObject fetchWikipedia(String query) throws Exception {
        JSONObject result = new JSONObject();
        result.put("source", "Wikipedia");
        result.put("query", query);
        result.put("summary", readJson(WIKI_BASE + encode(query)));
        return result;
    }

    private static JSONObject fetchDailyMed(String query) throws Exception {
        JSONObject result = new JSONObject();
        result.put("source", "DailyMed");
        result.put("query", query);
        result.put("spls", readJson(DAILYMED_BASE + "spls.json?drug_name=" + encode(query) + "&pagesize=8"));
        return result;
    }

    private static void putLookup(JSONObject result, String key, JsonFetcher fetcher) {
        try { JSONObject data = fetcher.fetch(); result.put(key, data != null ? data : new JSONObject().put("available", false)); } catch (Exception ignored) {}
    }

    private static void putOptional(JSONObject result, String key, String url) {
        try { result.put(key, readJson(url)); } catch (Exception ignored) {}
    }

    private static JSONObject fetchWithCandidates(Context context, String query, CandidateFetcher fetcher) throws Exception {
        JSONArray candidates = candidatesFor(context, query);
        for (int i = 0; i < candidates.length(); i++) {
            String candidate = candidates.optString(i, "").trim();
            if (candidate.isEmpty()) continue;
            try {
                JSONObject result = fetcher.fetch(candidate);
                if (hasUsefulData(result)) { result.put("lookup_query", candidate); return result; }
            } catch (Exception ignored) {}
        }
        JSONObject fallback = fetcher.fetch(query);
        fallback.put("lookup_query", query);
        return fallback;
    }

    private static JSONArray candidatesFor(Context context, String query) {
        JSONArray candidates = new JSONArray();
        for (String c : MedicationQueryResolver.candidatesFor(context, query)) putUnique(candidates, c);
        putUnique(candidates, query);
        return candidates;
    }

    private static void putUnique(JSONArray array, String value) {
        if (value == null || value.trim().isEmpty()) return;
        String v = value.trim();
        for (int i = 0; i < array.length(); i++) { if (v.equalsIgnoreCase(array.optString(i))) return; }
        array.put(v);
    }

    private static boolean hasUsefulData(JSONObject result) {
        if (result == null || result.has("error_code")) return false;
        String source = result.optString("source", "");
        if ("Wikipedia".equals(source)) return result.optJSONObject("summary") != null;
        if ("RxNorm/RxNav".equals(source)) return !result.optString("rxcui", "").isEmpty();
        if ("ChEMBL".equals(source)) return !result.optString("molecule_chembl_id", "").isEmpty();
        JSONObject results = result.optJSONObject("results");
        JSONArray rows = results == null ? null : results.optJSONArray("results");
        return rows != null && rows.length() > 0;
    }

    private static JSONObject readJson(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        if (conn.getResponseCode() == 404) return new JSONObject().put("error_code", 404);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString());
        }
    }

    private static String firstRxCui(JSONObject result) {
        JSONObject dg = result.optJSONObject("drug_groups");
        JSONObject group = dg == null ? null : dg.optJSONObject("drugGroup");
        JSONArray conceptGroups = group == null ? null : group.optJSONArray("conceptGroup");
        if (conceptGroups == null) return "";
        for (int i = 0; i < conceptGroups.length(); i++) {
            JSONArray cp = conceptGroups.optJSONObject(i).optJSONArray("conceptProperties");
            if (cp != null && cp.length() > 0) return cp.optJSONObject(0).optString("rxcui", "");
        }
        return "";
    }

    private static String firstChemblId(JSONObject search) {
        JSONArray mols = search == null ? null : search.optJSONArray("molecules");
        return (mols != null && mols.length() > 0) ? mols.optJSONObject(0).optString("molecule_chembl_id", "") : "";
    }

    private static String requireQuery(String query) {
        if (query == null || query.trim().isEmpty()) throw new IllegalArgumentException("Query required");
        return query.trim();
    }

    private static String encode(String val) throws Exception { return URLEncoder.encode(val, "UTF-8").replace("+", "%20"); }
    private static String normalizeKey(String query) { return query.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "_"); }
    private interface JsonFetcher { JSONObject fetch() throws Exception; }
    private interface CandidateFetcher { JSONObject fetch(String query) throws Exception; }
}
