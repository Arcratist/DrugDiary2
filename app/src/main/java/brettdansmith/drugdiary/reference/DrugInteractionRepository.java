package brettdansmith.drugdiary.reference;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.model.interaction.InteractionCheckResult;
import brettdansmith.drugdiary.model.medication.MedicationRecord;

public final class DrugInteractionRepository {
    private DrugInteractionRepository() {
    }

    public static String checkMultiple(Context context, String[] drugs) throws Exception {
        List<String> valid = new ArrayList<>();
        for (String d : drugs) {
            String resolved = MedicationQueryResolver.bestNameFor(context, d == null ? "" : d.trim());
            if (!resolved.isEmpty()) valid.add(resolved);
        }

        if (valid.size() < 2) {
            return "### Interaction Check\n- Status: Please provide at least two substances.\n- Example: `/interact Aspirin | Ibuprofen`";
        }

        StringBuilder out = new StringBuilder();
        out.append("### Interaction Analysis\n");
        out.append("Substances: ").append(String.join(", ", valid)).append("\n\n");

        int cards = 0;
        for (int i = 0; i < valid.size(); i++) {
            for (int j = i + 1; j < valid.size(); j++) {
                InteractionCheckResult result = buildPair(context, valid.get(i), valid.get(j));
                out.append(formatCard(result)).append("\n\n");
                cards++;
            }
        }

        if (cards == 0) {
            return "No specific interaction data found between these substances in local or public databases.";
        }

        out.append("---\n");
        out.append("Confidence model: local rule guidance + source evidence + explicit uncertainty.\n");
        out.append("This does not replace clinical advice for emergencies.");
        return out.toString().trim();
    }

    public static String checkSaved(Context context) throws Exception {
        List<String> names = new ArrayList<>();
        for (MedicationRecord record : new MedicationRepository(context).listRecords()) {
            if (!record.name.isEmpty()) names.add(record.name);
        }
        if (names.size() < 2) {
            return "### Saved Interaction Check\nAdd at least two medications to your profile to check interactions.";
        }
        return checkMultiple(context, names.toArray(new String[0]));
    }

    public static InteractionCheckResult buildPair(Context context, String first, String second) throws Exception {
        String resA = MedicationQueryResolver.bestNameFor(context, first);
        String resB = MedicationQueryResolver.bestNameFor(context, second);

        InteractionReference.Result local = InteractionReference.check(resA, resB);
        JSONObject firstLabel = tryLookup(() -> DrugDatabaseRepository.lookupOpenFdaLabel(context, resA));
        JSONObject secondLabel = tryLookup(() -> DrugDatabaseRepository.lookupOpenFdaLabel(context, resB));
        JSONObject firstChembl = tryLookup(() -> DrugDatabaseRepository.lookupChembl(context, resA));
        JSONObject secondChembl = tryLookup(() -> DrugDatabaseRepository.lookupChembl(context, resB));

        String overlap = mechanismOverlap(firstChembl, secondChembl);
        String labelMentions = labelMentions(firstLabel, secondLabel, resA, resB);
        String uncertainty = buildUncertainty(firstLabel, secondLabel, firstChembl, secondChembl, local);
        JSONArray sources = new JSONArray();
        sources.put("local_rules");
        if (firstLabel != null || secondLabel != null) sources.put("openfda");
        if (firstChembl != null || secondChembl != null) sources.put("chembl");

        return new InteractionCheckResult(
                resA,
                resB,
                local.risk.name(),
                overlap.isEmpty() ? labelMentions : overlap,
                local.summary,
                uncertainty,
                sources,
                System.currentTimeMillis());
    }

    private static JSONObject tryLookup(JsonSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatCard(InteractionCheckResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### ").append(result.first).append(" + ").append(result.second).append("\n");
        sb.append("- Severity: **").append(result.severity).append("**\n");
        if (!result.localGuidance.isEmpty()) sb.append("- Local rule guidance: ").append(result.localGuidance).append("\n");
        if (!result.mechanism.isEmpty()) sb.append("- Source-supported signal: ").append(result.mechanism).append("\n");
        sb.append("- Sources: ").append(result.sources.toString()).append("\n");
        sb.append("- Uncertainty: ").append(result.uncertainty.isEmpty() ? "No explicit source contradiction detected." : result.uncertainty).append("\n");
        sb.append("- Checked at: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(result.timestamp)));
        return sb.toString();
    }

    private static String mechanismOverlap(JSONObject firstChembl, JSONObject secondChembl) {
        String c1 = firstChembl == null ? "" : firstChembl.toString().toLowerCase(Locale.US);
        String c2 = secondChembl == null ? "" : secondChembl.toString().toLowerCase(Locale.US);
        if (c1.isEmpty() || c2.isEmpty()) return "";

        String[] keywords = {"serotonin", "dopamine", "gaba", "opioid", "cyp3a4", "reuptake", "monoamine", "adrenergic"};
        List<String> found = new ArrayList<>();
        for (String k : keywords) {
            if (c1.contains(k) && c2.contains(k)) found.add(k);
        }
        return found.isEmpty() ? "" : "Shared mechanism keywords: " + String.join(", ", found);
    }

    private static String labelMentions(JSONObject firstLabel, JSONObject secondLabel, String first, String second) {
        boolean firstInSecond = contains(secondLabel, first);
        boolean secondInFirst = contains(firstLabel, second);

        if (firstInSecond && secondInFirst) return "Both FDA labels reference the other substance.";
        if (firstInSecond) return second + " label references " + first + ".";
        if (secondInFirst) return first + " label references " + second + ".";
        return "";
    }

    private static String buildUncertainty(JSONObject firstLabel, JSONObject secondLabel, JSONObject firstChembl, JSONObject secondChembl, InteractionReference.Result local) {
        boolean hasSourceEvidence = firstLabel != null || secondLabel != null || firstChembl != null || secondChembl != null;
        if (!hasSourceEvidence && local.risk == InteractionReference.Risk.UNKNOWN) {
            return "No local rule match and no source evidence found for this pair.";
        }
        if (!hasSourceEvidence && local.risk != InteractionReference.Risk.UNKNOWN) {
            return "Local guidance available, but no current external evidence payload was retrieved.";
        }
        if (hasSourceEvidence && local.risk == InteractionReference.Risk.UNKNOWN) {
            return "Source snippets found but no local harm-reduction rule classification matched.";
        }
        return "";
    }

    private static boolean contains(JSONObject obj, String query) {
        if (obj == null || query == null || query.length() < 3) return false;
        return obj.toString().toLowerCase(Locale.US).contains(query.toLowerCase(Locale.US));
    }

    private interface JsonSupplier {
        JSONObject get() throws Exception;
    }
}

