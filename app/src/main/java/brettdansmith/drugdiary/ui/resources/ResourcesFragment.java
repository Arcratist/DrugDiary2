package brettdansmith.drugdiary.ui.resources;

import brettdansmith.drugdiary.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import brettdansmith.drugdiary.databinding.FragmentResourcesBinding;

public class ResourcesFragment extends Fragment {
    private FragmentResourcesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResourcesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnEmergencyContacts.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.supportResourcesFragment));
        binding.btnViewCharts.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.reagentChartFragment));
        binding.btnVolumetricCalc.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.doseCalculatorFragment));
        binding.btnCheckInteractions.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.interactionCheckerFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



