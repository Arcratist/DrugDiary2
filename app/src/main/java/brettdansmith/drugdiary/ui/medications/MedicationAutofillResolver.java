package brettdansmith.drugdiary.ui.medications;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.model.medication.MedicationCategory;

final class MedicationAutofillResolver {
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?\\s*(?:mcg|mg|g|ml|iu|units?))", Pattern.CASE_INSENSITIVE);

    private MedicationAutofillResolver() {
    }

    static Suggestion resolve(JSONObject payload, String fallbackName) {
        String initialName = fallbackName == null ? "" : fallbackName.trim();
        String name = initialName;
        String strength = "";
        String form = "";
        String route = "";
        List<String> sources = new ArrayList<>();

        JSONObject rxnorm = payload == null ? null : payload.optJSONObject("rxnorm");
        if (isAvailable(rxnorm)) {
            JSONObject properties = rxnorm.optJSONObject("properties");
            JSONObject prop = properties == null ? null : properties.optJSONObject("properties");
            if (prop != null) {
                String rxName = prop.optString("name", "").trim();
                if (!rxName.isEmpty()) {
                    name = chooseBetterName(name, rxName);
                }
            }
            String formulation = firstRxNormFormulation(rxnorm);
            if (!formulation.isEmpty()) {
                if (strength.isEmpty()) strength = extractStrength(formulation);
                if (form.isEmpty()) form = extractForm(formulation);
                if (route.isEmpty()) route = extractRoute(formulation);
            }
            sources.add("RxNorm");
        }

        JSONObject fda = payload == null ? null : payload.optJSONObject("openfda_label");
        if (isAvailable(fda)) {
            JSONObject first = firstOpenFdaResult(fda);
            JSONObject openfda = first == null ? null : first.optJSONObject("openfda");
            if (openfda != null) {
                String generic = firstArrayValue(openfda.optJSONArray("generic_name"));
                String brand = firstArrayValue(openfda.optJSONArray("brand_name"));
                String dosageForm = firstArrayValue(openfda.optJSONArray("dosage_form"));
                String fdaRoute = firstArrayValue(openfda.optJSONArray("route"));
                if (!generic.isEmpty()) name = chooseBetterName(name, generic);
                else if (!brand.isEmpty()) name = chooseBetterName(name, brand);
                if (form.isEmpty() && !dosageForm.isEmpty()) form = dosageForm;
                if (route.isEmpty() && !fdaRoute.isEmpty()) route = fdaRoute;
            }
            String dosage = first == null ? "" : firstArrayValue(first.optJSONArray("dosage_and_administration"));
            if (strength.isEmpty() && !dosage.isEmpty()) strength = extractStrength(dosage);
            if (form.isEmpty() && !dosage.isEmpty()) form = extractForm(dosage);
            if (route.isEmpty() && !dosage.isEmpty()) route = extractRoute(dosage);
            sources.add("openFDA");
        }

        JSONObject dailymed = payload == null ? null : payload.optJSONObject("dailymed");
        if (isAvailable(dailymed)) {
            String title = firstDailyMedTitle(dailymed);
            if (!title.isEmpty()) {
                if (strength.isEmpty()) strength = extractStrength(title);
                if (form.isEmpty()) form = extractForm(title);
                if (route.isEmpty()) route = extractRoute(title);
            }
            sources.add("DailyMed");
        }

        if (name.isEmpty()) name = initialName;
        if (form.isEmpty()) form = defaultFormFor(name);
        if (route.isEmpty()) route = defaultRouteFor(form);

        MedicationCategory category = categoryFor(name);
        String sourceLabel = sources.isEmpty() ? "local catalog" : String.join(", ", sources);
        return new Suggestion(name, strength, normalizeWord(form), normalizeWord(route), category, sourceLabel);
    }

    private static JSONObject firstOpenFdaResult(JSONObject fda) {
        JSONObject root = fda == null ? null : fda.optJSONObject("results");
        JSONArray rows = root == null ? null : root.optJSONArray("results");
        return rows == null || rows.length() == 0 ? null : rows.optJSONObject(0);
    }

    private static String firstRxNormFormulation(JSONObject rxnorm) {
        JSONObject groups = rxnorm == null ? null : rxnorm.optJSONObject("drug_groups");
        JSONObject group = groups == null ? null : groups.optJSONObject("drugGroup");
        JSONArray conceptGroups = group == null ? null : group.optJSONArray("conceptGroup");
        if (conceptGroups == null) return "";
        for (int i = 0; i < conceptGroups.length(); i++) {
            JSONObject concept = conceptGroups.optJSONObject(i);
            if (concept == null) continue;
            JSONArray properties = concept.optJSONArray("conceptProperties");
            if (properties == null || properties.length() == 0) continue;
            String name = properties.optJSONObject(0).optString("name", "");
            if (!name.isEmpty()) return name;
        }
        return "";
    }

    private static String firstDailyMedTitle(JSONObject dailyMed) {
        JSONObject spls = dailyMed.optJSONObject("spls");
        JSONObject data = spls == null ? null : spls.optJSONObject("data");
        JSONArray rows = data == null ? null : data.optJSONArray("spl");
        if (rows == null || rows.length() == 0) return "";
        return rows.optJSONObject(0).optString("title", "");
    }

    private static String firstArrayValue(JSONArray values) {
        if (values == null || values.length() == 0) return "";
        String value = values.optString(0, "");
        return value == null ? "" : value.trim();
    }

    private static String chooseBetterName(String original, String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) return original == null ? "" : original;
        String cleanCandidate = candidate.trim();
        if (original == null || original.trim().isEmpty()) return cleanCandidate;
        if (cleanCandidate.length() < original.trim().length()) return cleanCandidate;
        return original.trim();
    }

    private static String extractStrength(String text) {
        if (text == null) return "";
        Matcher matcher = STRENGTH_PATTERN.matcher(text);
        if (!matcher.find()) return "";
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private static String extractForm(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("tablet")) return "Tablet";
        if (lower.contains("capsule")) return "Capsule";
        if (lower.contains("solution")) return "Solution";
        if (lower.contains("suspension")) return "Suspension";
        if (lower.contains("injection") || lower.contains("inject")) return "Injection";
        if (lower.contains("patch")) return "Patch";
        if (lower.contains("spray")) return "Spray";
        if (lower.contains("cream")) return "Cream";
        if (lower.contains("ointment")) return "Ointment";
        if (lower.contains("gel")) return "Gel";
        if (lower.contains("drops")) return "Drops";
        return "";
    }

    private static String extractRoute(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.US);
        if (lower.contains("oral") || lower.contains("po")) return "Oral";
        if (lower.contains("topical")) return "Topical";
        if (lower.contains("intravenous") || lower.contains("iv")) return "Intravenous";
        if (lower.contains("intramuscular") || lower.contains("im")) return "Intramuscular";
        if (lower.contains("subcutaneous") || lower.contains("sc")) return "Subcutaneous";
        if (lower.contains("nasal")) return "Nasal";
        if (lower.contains("inhal")) return "Inhaled";
        return "";
    }

    private static String defaultFormFor(String name) {
        String clean = name == null ? "" : name.toLowerCase(Locale.US);
        if (clean.contains("oil")) return "Liquid";
        if (clean.contains("spray")) return "Spray";
        return "";
    }

    private static String defaultRouteFor(String form) {
        String clean = form == null ? "" : form.toLowerCase(Locale.US);
        if (clean.isEmpty()) return "";
        if (clean.contains("cream") || clean.contains("ointment") || clean.contains("gel") || clean.contains("patch")) return "Topical";
        if (clean.contains("spray")) return "Nasal";
        return "Oral";
    }

    private static MedicationCategory categoryFor(String name) {
        String value = MedicationCatalog.categoryFor(name);
        if (value == null) return MedicationCategory.SAVED;
        String lower = value.toLowerCase(Locale.US);
        if (lower.contains("recreat")) return MedicationCategory.RECREATIONAL;
        if (lower.contains("supplement")) return MedicationCategory.SUPPLEMENT;
        if (lower.contains("analgesic") || lower.contains("antidepressant") || lower.contains("antipsychotic")
                || lower.contains("mood") || lower.contains("antihistamine") || lower.contains("nsaid")) {
            return MedicationCategory.MEDICAL;
        }
        return MedicationCategory.SAVED;
    }

    private static String normalizeWord(String value) {
        if (value == null) return "";
        String clean = value.trim();
        if (clean.isEmpty()) return "";
        return clean.substring(0, 1).toUpperCase(Locale.US) + clean.substring(1);
    }

    private static boolean isAvailable(JSONObject source) {
        if (source == null) return false;
        if (source.has("error_code")) return false;
        return source.optBoolean("available", true);
    }

    static final class Suggestion {
        final String name;
        final String strength;
        final String form;
        final String route;
        final MedicationCategory category;
        final String sources;

        Suggestion(String name, String strength, String form, String route, MedicationCategory category, String sources) {
            this.name = name == null ? "" : name.trim();
            this.strength = strength == null ? "" : strength.trim();
            this.form = form == null ? "" : form.trim();
            this.route = route == null ? "" : route.trim();
            this.category = category == null ? MedicationCategory.SAVED : category;
            this.sources = sources == null ? "" : sources.trim();
        }
    }
}
