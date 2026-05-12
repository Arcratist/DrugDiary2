package brettdansmith.drugdiary.domain.medication;

import org.json.JSONObject;

import brettdansmith.drugdiary.reference.DrugDatabaseClient;

public final class DrugReferenceFormatter {
    private DrugReferenceFormatter() {
    }

    public static String summary(JSONObject payload) {
        return DrugDatabaseClient.formatSummary(payload);
    }

    public static String raw(JSONObject payload) {
        return DrugDatabaseClient.formatRaw(payload);
    }
}

