package brettdansmith.drugdiary.ui.reference;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import brettdansmith.drugdiary.databinding.FragmentReagentChartBinding;
import brettdansmith.drugdiary.reference.ReagentReference;

public class ReagentChartFragment extends Fragment {
    private FragmentReagentChartBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReagentChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textReagentGuide.setText(ReagentReference.quickGuide());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



