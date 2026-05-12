package brettdansmith.drugdiary.model.medication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MedicationRecord {
    public final String id;
    public final String name;
    public final String canonicalName;
    public final String strength;
    public final String form;
    public final String route;
    public final MedicationCategory category;
    public final LinkedHashSet<MedicationCategory> categories;
    public final boolean active;
    public final boolean favorite;
    public final boolean saved;
    public final boolean prn;
    public final String notes;
    public final MedicationSchedule schedule;
    public final MedicationInventory inventory;
    public final JSONArray aliases;
    public final long createdAt;
    public final long updatedAt;

    public MedicationRecord(
            String id,
            String name,
            String canonicalName,
            String strength,
            String form,
            String route,
            MedicationCategory category,
            boolean active,
            boolean favorite,
            boolean saved,
            boolean prn,
            String notes,
            MedicationSchedule schedule,
            MedicationInventory inventory,
            JSONArray aliases,
            long createdAt,
            long updatedAt) {
        this(id, name, canonicalName, strength, form, route, category, null, active, favorite, saved, prn, notes, schedule, inventory, aliases, createdAt, updatedAt);
    }

    public MedicationRecord(
            String id,
            String name,
            String canonicalName,
            String strength,
            String form,
            String route,
            MedicationCategory category,
            Set<MedicationCategory> categories,
            boolean active,
            boolean favorite,
            boolean saved,
            boolean prn,
            String notes,
            MedicationSchedule schedule,
            MedicationInventory inventory,
            JSONArray aliases,
            long createdAt,
            long updatedAt) {
        this.id = id == null || id.trim().isEmpty() ? "med_" + System.currentTimeMillis() : id.trim();
        this.name = name == null ? "" : name.trim();
        this.canonicalName = canonicalName == null || canonicalName.trim().isEmpty() ? this.name : canonicalName.trim();
        this.strength = strength == null ? "" : strength.trim();
        this.form = form == null ? "" : form.trim();
        this.route = route == null ? "" : route.trim();
        this.category = category == null ? MedicationCategory.SAVED : category;
        this.categories = normalizeCategories(this.category, categories);
        this.active = active;
        this.favorite = favorite;
        this.saved = saved;
        this.prn = prn;
        this.notes = notes == null ? "" : notes.trim();
        this.schedule = schedule == null ? new MedicationSchedule("scheduled", "", 0) : schedule;
        this.inventory = inventory == null ? new MedicationInventory(0, "units", 0, 0) : inventory;
        this.aliases = aliases == null ? new JSONArray() : aliases;
        this.createdAt = createdAt <= 0 ? System.currentTimeMillis() : createdAt;
        this.updatedAt = updatedAt <= 0 ? this.createdAt : updatedAt;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("canonical_name", canonicalName)
                .put("strength", strength)
                .put("form", form)
                .put("route", route)
                .put("category", category.name())
                .put("categories", MedicationCategory.toJsonArray(categories))
                .put("active", active)
                .put("favorite", favorite)
                .put("saved", saved)
                .put("prn", prn)
                .put("notes", notes)
                .put("schedule", schedule.toJson())
                .put("inventory", inventory.toJson())
                .put("known_aliases", aliases)
                .put("created_at", createdAt)
                .put("updated_at", updatedAt);
    }

    public static MedicationRecord fromJson(JSONObject json) {
        if (json == null) {
            return new MedicationRecord("", "", "", "", "", "", MedicationCategory.SAVED, null, true, false, true, false, "", null, null, null, 0, 0);
        }
        JSONArray aliases = json.optJSONArray("known_aliases");
        if (aliases == null) aliases = new JSONArray();
        MedicationCategory primary = MedicationCategory.from(json.optString("category", json.optString("group", "Saved")));
        LinkedHashSet<MedicationCategory> categories = MedicationCategory.fromJsonArray(json.optJSONArray("categories"));
        if (categories.isEmpty()) {
            categories = MedicationCategory.parseMany(json.optString("groups", ""));
        }
        categories.add(primary);
        return new MedicationRecord(
                json.optString("id", ""),
                json.optString("name", ""),
                json.optString("canonical_name", ""),
                json.optString("strength", ""),
                json.optString("form", ""),
                json.optString("route", ""),
                primary,
                categories,
                json.optBoolean("active", true),
                json.optBoolean("favorite", false),
                json.optBoolean("saved", true),
                json.optBoolean("prn", "prn".equalsIgnoreCase(json.optString("schedule_type", ""))),
                json.optString("notes", ""),
                MedicationSchedule.fromJson(json.optJSONObject("schedule")),
                MedicationInventory.fromJson(json.optJSONObject("inventory")),
                aliases,
                json.optLong("created_at", 0),
                json.optLong("updated_at", 0));
    }

    public boolean hasCategory(MedicationCategory category) {
        if (category == null) return false;
        return categories.contains(category);
    }

    public String categoryLabels() {
        return MedicationCategory.labels(categories);
    }

    private static LinkedHashSet<MedicationCategory> normalizeCategories(MedicationCategory primary, Set<MedicationCategory> input) {
        LinkedHashSet<MedicationCategory> out = new LinkedHashSet<>();
        if (input != null) {
            for (MedicationCategory category : input) {
                if (category != null) out.add(category);
            }
        }
        MedicationCategory safePrimary = primary == null ? MedicationCategory.SAVED : primary;
        if (out.isEmpty()) {
            out.add(safePrimary);
        } else if (!out.contains(safePrimary)) {
            out.add(safePrimary);
        }

        if (out.contains(MedicationCategory.ARCHIVED)) out.remove(MedicationCategory.ACTIVE);
        if (out.contains(MedicationCategory.WISHLIST)) out.remove(MedicationCategory.HAVE_ACCESS);
        return out;
    }
}

