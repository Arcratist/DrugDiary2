package brettdansmith.drugdiary.model.medication;

import org.json.JSONException;
import org.json.JSONObject;

public final class MedicationDoseLog {
    public final String id;
    public final String medicationId;
    public final double amount;
    public final String unit;
    public final String route;
    public final String status;
    public final String notes;
    public final long loggedAt;

    public MedicationDoseLog(String id, String medicationId, double amount, String unit, String route, String status, String notes, long loggedAt) {
        this.id = id == null || id.trim().isEmpty() ? "dose_" + System.currentTimeMillis() : id.trim();
        this.medicationId = medicationId == null ? "" : medicationId.trim();
        this.amount = Math.max(0, amount);
        this.unit = unit == null || unit.trim().isEmpty() ? "mg" : unit.trim();
        this.route = route == null ? "" : route.trim();
        this.status = status == null || status.trim().isEmpty() ? "taken" : status.trim();
        this.notes = notes == null ? "" : notes.trim();
        this.loggedAt = loggedAt <= 0 ? System.currentTimeMillis() : loggedAt;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("medication_id", medicationId)
                .put("amount", amount)
                .put("unit", unit)
                .put("route", route)
                .put("status", status)
                .put("notes", notes)
                .put("logged_at", loggedAt);
    }

    public static MedicationDoseLog fromJson(JSONObject json) {
        if (json == null) return new MedicationDoseLog("", "", 0, "mg", "", "taken", "", 0);
        return new MedicationDoseLog(
                json.optString("id", ""),
                json.optString("medication_id", ""),
                json.optDouble("amount", 0),
                json.optString("unit", "mg"),
                json.optString("route", ""),
                json.optString("status", "taken"),
                json.optString("notes", ""),
                json.optLong("logged_at", 0));
    }
}

