package brettdansmith.drugdiary.domain.model.medication;

import org.json.JSONArray;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public enum MedicationCategory {
    MEDICAL("Medical"),
    RECREATIONAL("Recreational"),
    SUPPLEMENT("Supplement"),
    HAVE_ACCESS("Have Access"),
    WISHLIST("Yet To Acquire"),
    SAVED("Saved"),
    ACTIVE("Active"),
    ARCHIVED("Archived"),
    FAVORITE("Favourite");

    private final String label;

    MedicationCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static JSONArray toJsonArray(Set<MedicationCategory> categories) {
        JSONArray array = new JSONArray();
        if (categories == null || categories.isEmpty()) return array;
        for (MedicationCategory category : categories) {
            if (category != null) array.put(category.name());
        }
        return array;
    }

    public static LinkedHashSet<MedicationCategory> parseMany(String value) {
        LinkedHashSet<MedicationCategory> out = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty()) return out;
        String[] parts = value.split("[,|/]");
        for (String part : parts) {
            MedicationCategory category = from(part);
            if (category != null) out.add(category);
        }
        return out;
    }

    public static LinkedHashSet<MedicationCategory> fromJsonArray(JSONArray array) {
        LinkedHashSet<MedicationCategory> out = new LinkedHashSet<>();
        if (array == null) return out;
        for (int i = 0; i < array.length(); i++) {
            String raw = array.optString(i, "");
            if (raw.trim().isEmpty()) continue;
            out.add(from(raw));
        }
        return out;
    }

    public static String labels(Set<MedicationCategory> categories) {
        if (categories == null || categories.isEmpty()) return SAVED.label();
        StringBuilder out = new StringBuilder();
        for (MedicationCategory category : categories) {
            if (category == null) continue;
            if (out.length() > 0) out.append(", ");
            out.append(category.label());
        }
        return out.length() == 0 ? SAVED.label() : out.toString();
    }

    public static MedicationCategory from(String value) {
        if (value == null) return SAVED;
        String clean = value.trim().toLowerCase(Locale.US);
        if (clean.isEmpty()) return SAVED;
        if ("supplements".equals(clean)) return SUPPLEMENT;
        if ("favorite".equals(clean) || "favourites".equals(clean) || "favorites".equals(clean)) return FAVORITE;
        if ("on hand".equals(clean) || "on_hand".equals(clean)) return HAVE_ACCESS;
        if ("wish list".equals(clean) || "yet to acquire".equals(clean) || "still relevant".equals(clean)) return WISHLIST;
        for (MedicationCategory category : values()) {
            if (category.name().toLowerCase(Locale.US).equals(clean)
                    || category.label.toLowerCase(Locale.US).equals(clean)) {
                return category;
            }
        }
        return SAVED;
    }
}
