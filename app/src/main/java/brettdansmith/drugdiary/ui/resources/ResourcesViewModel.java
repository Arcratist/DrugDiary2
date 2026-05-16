package brettdansmith.drugdiary.ui.resources;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.domain.model.resources.SupportResource;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

/**
 * ViewModel for managing support resources.
 */
public class ResourcesViewModel extends ViewModel {
    private final MutableLiveData<List<SupportResource>> resourcesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public ResourcesViewModel(ServiceLocator serviceLocator) {
        loadResources();
    }

    /**
     * Gets the live data for support resources.
     */
    public LiveData<List<SupportResource>> getResources() {
        return resourcesLiveData;
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
     * Loads all support resources.
     */
    public void loadResources() {
        loadResourcesInternal();
    }

    /**
     * Internal method to load resources.
     */
    private void loadResourcesInternal() {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                // Load from SupportResourceProvider
                List<SupportResource> resources =
                    brettdansmith.drugdiary.domain.model.resources.SupportResourceProvider.getResources();
                resourcesLiveData.postValue(resources);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                resourcesLiveData.postValue(new ArrayList<>());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }
}

