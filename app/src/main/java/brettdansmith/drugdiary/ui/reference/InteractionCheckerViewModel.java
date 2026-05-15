package brettdansmith.drugdiary.ui.reference;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import brettdansmith.drugdiary.domain.model.interaction.InteractionCheckResult;
import brettdansmith.drugdiary.domain.repository.DrugReferenceRepository;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

/**
 * ViewModel for drug interaction checking.
 */
public class InteractionCheckerViewModel extends ViewModel {
    private final DrugReferenceRepository drugRefRepository;

    private final MutableLiveData<InteractionCheckResult> resultLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> analysisLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public InteractionCheckerViewModel(ServiceLocator serviceLocator) {
        this.drugRefRepository = serviceLocator.getDrugReferenceRepository();
    }

    /**
     * Gets the live data for interaction result.
     */
    public LiveData<InteractionCheckResult> getResult() {
        return resultLiveData;
    }

    /**
     * Gets the live data for analysis text.
     */
    public LiveData<String> getAnalysis() {
        return analysisLiveData;
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
     * Checks interaction between two drugs.
     */
    public void checkInteraction(String firstDrug, String secondDrug) {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                InteractionCheckResult result =
                    drugRefRepository.checkInteraction(firstDrug, secondDrug);
                resultLiveData.postValue(result);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                resultLiveData.postValue(null);
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Checks interactions between multiple drugs.
     */
    public void checkMultipleInteractions(String[] drugs) {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                String analysis =
                    drugRefRepository.checkMultipleInteractions(drugs);
                analysisLiveData.postValue(analysis);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                analysisLiveData.postValue(null);
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Resolves drug name to canonical form.
     */
    public void resolveDrugName(String drugName) {
        new Thread(() -> {
            try {
                String resolved = drugRefRepository.resolveDrugName(drugName);
                // Could emit via LiveData if needed
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }
}

