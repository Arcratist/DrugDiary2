package brettdansmith.drugdiary.data.medication;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.model.medication.MedicationCategory;
import brettdansmith.drugdiary.model.medication.MedicationDoseLog;
import brettdansmith.drugdiary.model.medication.MedicationRecord;

public final class MedicationRepository {
    private final Context appContext;

    public MedicationRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public JSONArray list() {
        JSONArray out = new JSONArray();
        for (MedicationRecord record : listRecords()) {
            try {
                out.put(record.toJson());
            } catch (JSONException ignored) {
            }
        }
        return out;
    }

    public List<MedicationRecord> listRecords() {
        List<MedicationRecord> records = new ArrayList<>();
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONObject trackers = data.optJSONObject(ProfileJson.KEY_TRACKERS);
        JSONArray medications = trackers == null ? null : trackers.optJSONArray(ProfileJson.KEY_MEDICATIONS);
        if (medications == null) return records;

        for (int i = 0; i < medications.length(); i++) {
            MedicationRecord record = MedicationRecord.fromJson(medications.optJSONObject(i));
            if (record.name.isEmpty()) continue;
            records.add(record);
        }

        Collections.sort(records, (a, b) -> {
            if (a.favorite != b.favorite) return a.favorite ? -1 : 1;
            if (a.active != b.active) return a.active ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        return records;
    }

    public List<MedicationDoseLog> listDoseLogsForMedication(String medicationId) {
        List<MedicationDoseLog> logs = new ArrayList<>();
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONObject trackers = data.optJSONObject(ProfileJson.KEY_TRACKERS);
        JSONArray array = trackers == null ? null : trackers.optJSONArray(ProfileJson.KEY_MEDICATION_DOSE_LOGS);
        if (array == null) return logs;
        for (int i = 0; i < array.length(); i++) {
            MedicationDoseLog log = MedicationDoseLog.fromJson(array.optJSONObject(i));
            if (medicationId == null || medicationId.trim().isEmpty() || medicationId.equals(log.medicationId)) {
                logs.add(log);
            }
        }
        logs.sort((a, b) -> Long.compare(b.loggedAt, a.loggedAt));
        return logs;
    }

    public void addDoseLog(MedicationDoseLog doseLog) throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONObject trackers = ProfileJson.object(data, ProfileJson.KEY_TRACKERS);
        JSONArray logs = ProfileJson.array(trackers, ProfileJson.KEY_MEDICATION_DOSE_LOGS);
        logs.put(doseLog.toJson());

        JSONArray medications = ProfileJson.array(trackers, ProfileJson.KEY_MEDICATIONS);
        for (int i = 0; i < medications.length(); i++) {
            JSONObject existing = medications.optJSONObject(i);
            if (existing != null && doseLog.medicationId.equals(existing.optString("id"))) {
                existing.put("updated_at", System.currentTimeMillis());
                JSONObject schedule = existing.optJSONObject("schedule");
                if (schedule == null) schedule = new JSONObject();
                schedule.put("next_dose_at", 0);
                existing.put("schedule", schedule);
                break;
            }
        }

        EncryptedProfileStore.saveProfileData(appContext, data);
    }

    public void add(JSONObject medication) throws JSONException {
        upsert(medication);
    }

    public void upsertRecord(MedicationRecord medication) throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONArray medications = medications(data);
        MedicationRecord normalized = normalize(medication);
        JSONObject payload = normalized.toJson();
        String id = normalized.id;

        for (int i = 0; i < medications.length(); i++) {
            JSONObject existing = medications.optJSONObject(i);
            if (existing != null && id.equals(existing.optString("id"))) {
                payload.put("created_at", existing.optLong("created_at", normalized.createdAt));
                payload.put("updated_at", System.currentTimeMillis());
                medications.put(i, payload);
                EncryptedProfileStore.saveProfileData(appContext, data);
                return;
            }
        }

        payload.put("created_at", System.currentTimeMillis());
        payload.put("updated_at", System.currentTimeMillis());
        medications.put(payload);
        EncryptedProfileStore.saveProfileData(appContext, data);
    }

    public void upsert(JSONObject medication) throws JSONException {
        upsertRecord(normalize(MedicationRecord.fromJson(medication)));
    }

    public void delete(String id) throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONArray medications = medications(data);
        for (int i = 0; i < medications.length(); i++) {
            JSONObject existing = medications.optJSONObject(i);
            if (existing != null && id.equals(existing.optString("id"))) {
                medications.remove(i);
                EncryptedProfileStore.saveProfileData(appContext, data);
                return;
            }
        }
    }

    public void setFavorite(String id, boolean favorite) throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONArray medications = medications(data);
        for (int i = 0; i < medications.length(); i++) {
            JSONObject existing = medications.optJSONObject(i);
            if (existing != null && id.equals(existing.optString("id"))) {
                MedicationRecord source = MedicationRecord.fromJson(existing);
                LinkedHashSet<MedicationCategory> categories = new LinkedHashSet<>(source.categories);
                if (favorite) categories.add(MedicationCategory.FAVORITE); else categories.remove(MedicationCategory.FAVORITE);
                MedicationRecord updated = new MedicationRecord(
                        source.id,
                        source.name,
                        source.canonicalName,
                        source.strength,
                        source.form,
                        source.route,
                        source.category,
                        categories,
                        source.active,
                        favorite,
                        source.saved,
                        source.prn,
                        source.notes,
                        source.schedule,
                        source.inventory,
                        source.aliases,
                        source.createdAt,
                        System.currentTimeMillis());
                medications.put(i, updated.toJson());
                EncryptedProfileStore.saveProfileData(appContext, data);
                return;
            }
        }
    }

    public void removeLast() throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONArray medications = medications(data);
        if (medications.length() > 0) {
            medications.remove(medications.length() - 1);
            EncryptedProfileStore.saveProfileData(appContext, data);
        }
    }

    private MedicationRecord normalize(MedicationRecord medication) {
        MedicationRecord source = medication == null
                ? new MedicationRecord("", "", "", "", "", "", MedicationCategory.SAVED, null, true, false, true, false, "", null, null, null, 0, 0)
                : medication;
        String canonical = MedicationCatalog.canonicalNameFor(source.name.isEmpty() ? source.canonicalName : source.name);
        MedicationCategory category = source.category == null ? MedicationCategory.SAVED : source.category;
        LinkedHashSet<MedicationCategory> categories = new LinkedHashSet<>(source.categories);
        categories.add(category);
        boolean archived = categories.contains(MedicationCategory.ARCHIVED);
        boolean wishlist = categories.contains(MedicationCategory.WISHLIST);
        boolean active = source.active && !archived && !wishlist;
        if (active) categories.add(MedicationCategory.ACTIVE); else categories.remove(MedicationCategory.ACTIVE);
        if (source.favorite) categories.add(MedicationCategory.FAVORITE); else categories.remove(MedicationCategory.FAVORITE);
        if (source.saved) categories.add(MedicationCategory.SAVED); else categories.remove(MedicationCategory.SAVED);
        return new MedicationRecord(
                source.id,
                source.name.isEmpty() ? canonical : source.name,
                canonical,
                source.strength,
                source.form,
                source.route,
                category,
                categories,
                active,
                source.favorite,
                source.saved,
                source.prn,
                source.notes,
                source.schedule,
                source.inventory,
                source.aliases,
                source.createdAt,
                System.currentTimeMillis());
    }

    private JSONArray medications(JSONObject data) throws JSONException {
        JSONObject trackers = ProfileJson.object(data, ProfileJson.KEY_TRACKERS);
        return ProfileJson.array(trackers, ProfileJson.KEY_MEDICATIONS);
    }
}

