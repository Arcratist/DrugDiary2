package brettdansmith.drugdiary.domain.medication;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.profile.EncryptedDrugCacheStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;

public final class MedicationQueryResolver {
    private MedicationQueryResolver() {
    }

    public static List<String> candidatesFor(Context context, String query) {
        List<String> candidates = new ArrayList<>();
        String clean = clean(query);
        addUnique(candidates, clean);
        if (clean.isEmpty()) return candidates;

        addCatalogCandidates(candidates, clean);
        addSavedMedicationCandidates(context, candidates, clean);
        addCachedReferenceCandidates(context, candidates, clean);
        return candidates;
    }

    public static List<String> suggestionsFor(Context context) {
        List<String> suggestions = new ArrayList<>();
        for (String value : MedicationCatalog.nameSuggestions()) addUnique(suggestions, value);
        addSavedMedicationCandidates(context, suggestions, "");
        addCachedReferenceCandidates(context, suggestions, "");
        return suggestions;
    }

    public static String bestNameFor(Context context, String query) {
        List<String> candidates = candidatesFor(context, query);
        return candidates.isEmpty() ? clean(query) : candidates.get(0);
    }

    public static String describeResolution(Context context, String query) {
        List<String> candidates = candidatesFor(context, query);
        if (candidates.isEmpty()) return "";
        String first = candidates.get(0);
        if (first.equalsIgnoreCase(clean(query)) && candidates.size() == 1) return "";
        StringBuilder text = new StringBuilder("Resolved lookup names: ");
        for (int i = 0; i < Math.min(6, candidates.size()); i++) {
            if (i > 0) text.append(", ");
            text.append(candidates.get(i));
        }
        if (candidates.size() > 6) text.append(", +").append(candidates.size() - 6).append(" more");
        return text.toString();
    }

    private static void addSavedMedicationCandidates(Context context, List<String> candidates, String query) {
        if (context == null) return;
        try {
            JSONArray medications = new MedicationRepository(context).list();
            String normalizedQuery = normalize(query);
            for (int i = 0; i < medications.length(); i++) {
                JSONObject medication = medications.optJSONObject(i);
                if (medication == null || (!normalizedQuery.isEmpty() && !matchesMedication(medication, normalizedQuery))) continue;

                addUnique(candidates, medication.optString("canonical_name", ""));
                addUnique(candidates, medication.optString("alias", ""));
                addUnique(candidates, MedicationCatalog.canonicalNameFor(medication.optString("name", "")));
                addUnique(candidates, medication.optString("name", ""));
                addJsonArray(candidates, medication.optJSONArray("known_aliases"));
                addCatalogCandidates(candidates, medication.optString("canonical_name", medication.optString("name", "")));
            }
        } catch (Exception ignored) {
        }
    }

    private static void addCachedReferenceCandidates(Context context, List<String> candidates, String query) {
        if (context == null) return;
        try {
            JSONObject data = EncryptedDrugCacheStore.loadDrugCache(context);
            String normalizedQuery = normalize(query);
            collectCachedNames(candidates, data.optJSONObject(ProfileJson.KEY_DRUG_DATABASE_CACHE), normalizedQuery, 0);
            collectCachedNames(candidates, data.optJSONObject(ProfileJson.KEY_PUBCHEM_CACHE), normalizedQuery, 0);
        } catch (Exception ignored) {
        }
    }

    private static void collectCachedNames(List<String> candidates, Object value, String normalizedQuery, int depth) {
        if (value == null || depth > 8) return;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String[] keys = {
                    "query", "original_query", "lookup_query", "name", "title", "pref_name",
                    "IUPACName", "MolecularFormula", "brand_name", "generic_name", "substance_name"
            };
            for (String key : keys) {
                Object child = object.opt(key);
                if (child instanceof String) addIfMatches(candidates, (String) child, normalizedQuery);
                else collectCachedNames(candidates, child, normalizedQuery, depth + 1);
            }
            JSONArray names = object.names();
            if (names == null) return;
            for (int i = 0; i < names.length(); i++) {
                collectCachedNames(candidates, object.opt(names.optString(i)), normalizedQuery, depth + 1);
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < Math.min(array.length(), 160); i++) {
                Object child = array.opt(i);
                if (child instanceof String) addIfMatches(candidates, (String) child, normalizedQuery);
                else collectCachedNames(candidates, child, normalizedQuery, depth + 1);
            }
        }
    }

    private static void addIfMatches(List<String> candidates, String value, String normalizedQuery) {
        String clean = clean(value);
        if (clean.length() < 2 || clean.length() > 90) return;
        if (clean.startsWith("http://") || clean.startsWith("https://")) return;
        if (!clean.matches(".*[A-Za-z].*")) return;
        if (!normalizedQuery.isEmpty() && !normalize(clean).contains(normalizedQuery) && !normalizedQuery.contains(normalize(clean))) return;
        addUnique(candidates, clean);
    }

    private static boolean matchesMedication(JSONObject medication, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) return false;
        if (normalize(medication.optString("name", "")).contains(normalizedQuery)) return true;
        if (normalize(medication.optString("canonical_name", "")).contains(normalizedQuery)) return true;
        if (normalize(medication.optString("alias", "")).contains(normalizedQuery)) return true;
        if (normalize(medication.optString("category", "")).contains(normalizedQuery)) return true;
        JSONArray aliases = medication.optJSONArray("known_aliases");
        if (aliases != null) {
            for (int i = 0; i < aliases.length(); i++) {
                if (normalize(aliases.optString(i, "")).contains(normalizedQuery)) return true;
                if (normalizedQuery.contains(normalize(aliases.optString(i, "")))) return true;
            }
        }
        String catalogCanonical = MedicationCatalog.canonicalNameFor(normalizedQuery);
        return normalize(catalogCanonical).equals(normalize(medication.optString("canonical_name", "")));
    }

    private static void addCatalogCandidates(List<String> candidates, String query) {
        String canonical = MedicationCatalog.canonicalNameFor(query);
        addUnique(candidates, canonical);
        addJsonArray(candidates, MedicationCatalog.aliasesFor(query));
        addJsonArray(candidates, MedicationCatalog.aliasesFor(canonical));
    }

    private static void addJsonArray(List<String> candidates, JSONArray values) {
        if (values == null) return;
        for (int i = 0; i < values.length(); i++) {
            addUnique(candidates, values.optString(i, ""));
        }
    }

    private static void addUnique(List<String> candidates, String value) {
        String clean = clean(value);
        if (clean.isEmpty()) return;
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(clean)) return;
            if (normalize(candidate).equals(normalize(clean))) return;
        }
        candidates.add(clean);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
}
