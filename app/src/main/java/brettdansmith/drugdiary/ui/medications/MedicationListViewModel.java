package brettdansmith.drugdiary.ui.medications;

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
import brettdansmith.drugdiary.domain.model.medication.MedicationCategory;
import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.domain.repository.MedicationRepository;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

/**
 * ViewModel for managing medication list data and operations.
 * Handles loading, saving, and deleting medications.
 */
public class MedicationListViewModel extends ViewModel {
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

    private final MedicationRepository medicationRepository;

    private final MutableLiveData<List<MedicationRecord>> medicationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, List<MedicationRecord>>> filteredMedications = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String currentQuery = "";

    public MedicationListViewModel(ServiceLocator serviceLocator) {
        this.medicationRepository = serviceLocator.getMedicationRepository();
    }

    /**
     * Gets the live data for medications list.
     */
    public LiveData<List<MedicationRecord>> getMedications() {
        return medicationsLiveData;
    }

    /**
     * Gets the live data for filtered medications.
     */
    public LiveData<Map<String, List<MedicationRecord>>> getFilteredMedications() {
        return filteredMedications;
    }

    /**
     * Gets the live data for error messages.
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }

    /**
     * Gets the live data for loading state.
     */
    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    /**
     * Gets the network executor.
     */
    public ExecutorService getNetworkExecutor() {
        return executor;
    }

    /**
     * Loads all medications from the repository.
     */
    public void loadMedications() {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                List<MedicationRecord> medications = medicationRepository.listRecords();
                medicationsLiveData.postValue(medications);
                filterAndGroup(medications, currentQuery);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                medicationsLiveData.postValue(new ArrayList<>());
                filteredMedications.postValue(new HashMap<>());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    /**
     * Performs a search on the medication list.
     */
    public void search(String query) {
        this.currentQuery = query;
        filterAndGroup(medicationsLiveData.getValue(), query);
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
        return (med.name != null && med.name.toLowerCase(Locale.US).contains(query))
                || (med.canonicalName != null && med.canonicalName.toLowerCase(Locale.US).contains(query))
                || (med.categoryLabels() != null && med.categoryLabels().toLowerCase(Locale.US).contains(query))
                || (med.strength != null && med.strength.toLowerCase(Locale.US).contains(query))
                || (med.form != null && med.form.toLowerCase(Locale.US).contains(query))
                || (med.notes != null && med.notes.toLowerCase(Locale.US).contains(query));
    }

    private boolean isOnHand(MedicationRecord med) {
        if (med == null) return false;
        if (med.hasCategory(MedicationCategory.HAVE_ACCESS)) return true;
        if (med.hasCategory(MedicationCategory.WISHLIST) || med.hasCategory(MedicationCategory.ARCHIVED)) return false;
        if (med.inventory != null && med.inventory.quantity > 0) return true;
        return !med.active;
    }

    /**
     * Saves a medication record.
     */
    public void saveMedication(MedicationRecord record) {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                medicationRepository.saveRecord(record);
                loadMedications(); // Reload list
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    /**
     * Deletes a medication record.
     */
    public void deleteMedication(String medicationId) {
        loadingLiveData.setValue(true);
        executor.execute(() -> {
            try {
                medicationRepository.deleteRecord(medicationId);
                loadMedications(); // Reload list
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    /**
     * Gets medication count.
     */
    public void getMedicationCount() {
        executor.execute(() -> {
            try {
                int count = medicationRepository.count();
                // Could emit this via LiveData if needed
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}

