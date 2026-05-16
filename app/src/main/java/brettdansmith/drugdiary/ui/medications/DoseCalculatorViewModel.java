package brettdansmith.drugdiary.ui.medications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import brettdansmith.drugdiary.domain.service.ServiceLocator;
import brettdansmith.drugdiary.logic.DoseCalculator;

public class DoseCalculatorViewModel extends ViewModel {
    private final MutableLiveData<String> resultLiveData = new MutableLiveData<>();

    public DoseCalculatorViewModel(ServiceLocator serviceLocator) {
    }

    public LiveData<String> getResult() {
        return resultLiveData;
    }

    public void calculate(double substanceMg, double solventMl, double targetMg) {
        double concentration = DoseCalculator.milligramsPerMilliliter(substanceMg, solventMl);
        double volume = DoseCalculator.millilitersForDose(targetMg, concentration);

        if (concentration <= 0 || volume <= 0) {
            resultLiveData.setValue("Enter substance amount, liquid volume, and target dose.");
            return;
        }

        resultLiveData.setValue(String.format(java.util.Locale.getDefault(),
                "Concentration: %.3f mg/ml\nDose volume: %.3f ml",
                concentration,
                volume));
    }
}
