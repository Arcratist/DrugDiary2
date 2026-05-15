package brettdansmith.drugdiary.domain.model.medication;

import org.json.JSONException;
import org.json.JSONObject;

public final class MedicationInventory {
    public final double quantity;
    public final String unit;
    public final double lowStockThreshold;
    public final long expiryAt;

    public MedicationInventory(double quantity, String unit, double lowStockThreshold, long expiryAt) {
        this.quantity = Math.max(0, quantity);
        this.unit = unit == null || unit.trim().isEmpty() ? "units" : unit.trim();
        this.lowStockThreshold = Math.max(0, lowStockThreshold);
        this.expiryAt = Math.max(0, expiryAt);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("quantity", quantity)
                .put("unit", unit)
                .put("low_stock_threshold", lowStockThreshold)
                .put("expiry_at", expiryAt);
    }

    public static MedicationInventory fromJson(JSONObject json) {
        if (json == null) return new MedicationInventory(0, "units", 0, 0);
        return new MedicationInventory(
                json.optDouble("quantity", 0),
                json.optString("unit", "units"),
                json.optDouble("low_stock_threshold", 0),
                json.optLong("expiry_at", 0));
    }
}
