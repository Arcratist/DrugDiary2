package brettdansmith.drugdiary.ui.medications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.domain.repository.MedicationRepository;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

/**
 * ViewModel for managing medication list data and operations.
 * Handles loading, saving, and deleting medications.
 */
public class MedicationListViewModel extends ViewModel {
    private final MedicationRepository medicationRepository;

    private final MutableLiveData<List<MedicationRecord>> medicationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

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
     * Loads all medications from the repository.
     */
    public void loadMedications() {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                List<MedicationRecord> medications = medicationRepository.listRecords();
                medicationsLiveData.postValue(medications);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                medicationsLiveData.postValue(new ArrayList<>());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Saves a medication record.
     */
    public void saveMedication(MedicationRecord record) {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                medicationRepository.saveRecord(record);
                loadMedications(); // Reload list
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Deletes a medication record.
     */
    public void deleteMedication(String medicationId) {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                medicationRepository.deleteRecord(medicationId);
                loadMedications(); // Reload list
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Gets medication count.
     */
    public void getMedicationCount() {
        new Thread(() -> {
            try {
                int count = medicationRepository.count();
                // Could emit this via LiveData if needed
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }
}

