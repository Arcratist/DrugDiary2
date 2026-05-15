package brettdansmith.drugdiary.ui.diary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.repository.DiaryRepository;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

/**
 * ViewModel for managing diary entries.
 */
public class DiaryListViewModel extends ViewModel {
    private final DiaryRepository diaryRepository;

    private final MutableLiveData<List<DiaryEntry>> entriesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public DiaryListViewModel(ServiceLocator serviceLocator) {
        this.diaryRepository = serviceLocator.getDiaryRepository();
    }

    /**
     * Gets the live data for diary entries.
     */
    public LiveData<List<DiaryEntry>> getEntries() {
        return entriesLiveData;
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
     * Loads all diary entries.
     */
    public void loadEntries() {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                List<DiaryEntry> entries = diaryRepository.listEntries();
                entriesLiveData.postValue(entries);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                entriesLiveData.postValue(new ArrayList<>());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Saves a diary entry.
     */
    public void saveEntry(DiaryEntry entry) {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                diaryRepository.saveEntry(entry);
                loadEntries(); // Reload list
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }

    /**
     * Gets entries within a date range.
     */
    public void loadEntriesBetween(long startTime, long endTime) {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                List<DiaryEntry> entries = diaryRepository.entriesBetween(startTime, endTime);
                entriesLiveData.postValue(entries);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
                entriesLiveData.postValue(new ArrayList<>());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }
}

