package brettdansmith.drugdiary.ui.reference;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import brettdansmith.drugdiary.databinding.FragmentReagentChartBinding;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;

public class ReagentChartFragment extends Fragment {
    private FragmentReagentChartBinding binding;
    private ReagentChartViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReagentChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(ReagentChartViewModel.class);

        viewModel.getGuide().observe(getViewLifecycleOwner(), guide -> {
            binding.textReagentGuide.setText(guide);
        });

        viewModel.loadGuide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



