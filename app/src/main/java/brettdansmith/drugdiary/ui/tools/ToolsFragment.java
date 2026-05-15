package brettdansmith.drugdiary.ui.tools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import brettdansmith.drugdiary.R;

public class ToolsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.card_interaction_checker).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.interactionCheckerFragment));

        view.findViewById(R.id.card_dose_calculator).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.doseCalculatorFragment));

        view.findViewById(R.id.card_reagent_charts).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.reagentChartFragment));
    }
}
