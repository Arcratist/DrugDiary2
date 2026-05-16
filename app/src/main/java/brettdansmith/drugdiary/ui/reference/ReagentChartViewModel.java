package brettdansmith.drugdiary.ui.reference;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import brettdansmith.drugdiary.domain.service.ServiceLocator;
import brettdansmith.drugdiary.reference.ReagentReference;

public class ReagentChartViewModel extends ViewModel {
    private final MutableLiveData<String> guideLiveData = new MutableLiveData<>();

    public ReagentChartViewModel(ServiceLocator serviceLocator) {
        loadGuide();
    }

    public LiveData<String> getGuide() {
        return guideLiveData;
    }

    public void loadGuide() {
        guideLiveData.setValue(ReagentReference.quickGuide());
    }
}
