package brettdansmith.drugdiary.ui.medications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;

import brettdansmith.drugdiary.databinding.FragmentDoseCalculatorBinding;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;

public class DoseCalculatorFragment extends Fragment {
    private FragmentDoseCalculatorBinding binding;
    private DoseCalculatorViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDoseCalculatorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(DoseCalculatorViewModel.class);

        observeViewModel();

        binding.buttonCalculateDose.setOnClickListener(v -> calculate());
    }

    private void observeViewModel() {
        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            binding.textDoseResult.setText(result);
        });
    }

    private void calculate() {
        double substanceMg = readDouble(binding.inputSubstanceMg.getText());
        double solventMl = readDouble(binding.inputSolventMl.getText());
        double targetMg = readDouble(binding.inputTargetMg.getText());
        viewModel.calculate(substanceMg, solventMl, targetMg);
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



