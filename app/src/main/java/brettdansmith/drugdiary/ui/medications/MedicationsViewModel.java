package brettdansmith.drugdiary.ui.medications;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.domain.model.medication.MedicationCategory;
import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;

public class MedicationsViewModel extends ViewModel {

    public enum FilterMode {
        ALL(R.string.medication_menu_all),
        ACTIVE(R.string.medication_menu_active),
        ON_HAND(R.string.medication_group_on_hand),
        AS_NEEDED(R.string.medication_status_prn),
        MEDICAL(R.string.medication_group_medical),
        RECREATIONAL(R.string.medication_group_recreational),
        SUPPLEMENTS(R.string.medication_group_supplements),
        WISHLIST(R.string.medication_group_wishlist),
        INACTIVE(R.string.medication_group_archived);

        public final int titleRes;

        FilterMode(int titleRes) {
            this.titleRes = titleRes;
        }
    }

    private final MutableLiveData<List<MedicationRecord>> allMedications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, List<MedicationRecord>>> filteredMedications = new MutableLiveData<>(new HashMap<>());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Context appContext;

    public void setApplicationContext(Context context) {
        if (this.appContext == null && context != null) {
            this.appContext = context.getApplicationContext();
        }
    }

    public LiveData<Map<String, List<MedicationRecord>>> getFilteredMedications() {
        return filteredMedications;
    }

    public void loadMedications() {
        executor.execute(() -> {
            if (appContext == null) return;
            List<MedicationRecord> meds = new MedicationRepository(appContext).listRecords();
            allMedications.postValue(meds);
            filterAndGroup(meds, "");
        });
    }

    public void search(String query) {
        filterAndGroup(allMedications.getValue(), query);
    }

    private void filterAndGroup(List<MedicationRecord> medications, String query) {
        Map<String, List<MedicationRecord>> map = new HashMap<>();
        for (FilterMode mode : FilterMode.values()) {
            map.put(mode.name(), new ArrayList<>());
        }

        String filter = query == null ? "" : query.trim().toLowerCase(Locale.US);
        if (medications != null) {
            for (MedicationRecord med : medications) {
                if (med == null) continue;
                if (!filter.isEmpty() && !matches(med, filter)) continue;
                map.get(FilterMode.ALL.name()).add(med);
                if (med.active) {
                    map.get(FilterMode.ACTIVE.name()).add(med);
                }
                if (isOnHand(med)) {
                    map.get(FilterMode.ON_HAND.name()).add(med);
                }
                if (!med.active) {
                    map.get(FilterMode.INACTIVE.name()).add(med);
                }
                if (med.prn) {
                    map.get(FilterMode.AS_NEEDED.name()).add(med);
                }
                if (med.hasCategory(MedicationCategory.MEDICAL)) {
                    map.get(FilterMode.MEDICAL.name()).add(med);
                }
                if (med.hasCategory(MedicationCategory.RECREATIONAL)) {
                    map.get(FilterMode.RECREATIONAL.name()).add(med);
                }
                if (med.hasCategory(MedicationCategory.SUPPLEMENT)) {
                    map.get(FilterMode.SUPPLEMENTS.name()).add(med);
                }
                if (med.hasCategory(MedicationCategory.WISHLIST)) {
                    map.get(FilterMode.WISHLIST.name()).add(med);
                }
            }
        }

        filteredMedications.postValue(map);
    }

    private boolean matches(MedicationRecord med, String query) {
        return med.name.toLowerCase(Locale.US).contains(query)
                || med.canonicalName.toLowerCase(Locale.US).contains(query)
                || med.categoryLabels().toLowerCase(Locale.US).contains(query)
                || med.strength.toLowerCase(Locale.US).contains(query)
                || med.form.toLowerCase(Locale.US).contains(query)
                || med.notes.toLowerCase(Locale.US).contains(query);
    }

    private boolean isOnHand(MedicationRecord med) {
        if (med == null) return false;
        if (med.hasCategory(MedicationCategory.HAVE_ACCESS)) return true;
        if (med.hasCategory(MedicationCategory.WISHLIST) || med.hasCategory(MedicationCategory.ARCHIVED)) return false;
        if (med.inventory != null && med.inventory.quantity > 0) return true;
        return !med.active;
    }

    public ExecutorService getNetworkExecutor() {
        return executor;
    }

    public void postToMain(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
