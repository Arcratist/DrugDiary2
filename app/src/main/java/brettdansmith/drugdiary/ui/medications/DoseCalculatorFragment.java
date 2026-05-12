package brettdansmith.drugdiary.ui.medications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import brettdansmith.drugdiary.databinding.FragmentDoseCalculatorBinding;
import brettdansmith.drugdiary.logic.DoseCalculator;

public class DoseCalculatorFragment extends Fragment {
    private FragmentDoseCalculatorBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDoseCalculatorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonCalculateDose.setOnClickListener(v -> calculate());
    }

    private void calculate() {
        double substanceMg = readDouble(binding.inputSubstanceMg.getText());
        double solventMl = readDouble(binding.inputSolventMl.getText());
        double targetMg = readDouble(binding.inputTargetMg.getText());
        double concentration = DoseCalculator.milligramsPerMilliliter(substanceMg, solventMl);
        double volume = DoseCalculator.millilitersForDose(targetMg, concentration);

        if (concentration <= 0 || volume <= 0) {
            binding.textDoseResult.setText("Enter substance amount, liquid volume, and target dose.");
            return;
        }

        binding.textDoseResult.setText(String.format(Locale.getDefault(),
                "Concentration: %.3f mg/ml\nDose volume: %.3f ml",
                concentration,
                volume));
    }

    private double readDouble(CharSequence value) {
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



