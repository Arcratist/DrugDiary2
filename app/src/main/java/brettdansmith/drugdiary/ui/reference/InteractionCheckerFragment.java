package brettdansmith.drugdiary.ui.reference;

import brettdansmith.drugdiary.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import brettdansmith.drugdiary.databinding.FragmentInteractionCheckerBinding;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.reference.DrugInteractionRepository;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;

import java.util.ArrayList;
import java.util.List;

public class InteractionCheckerFragment extends Fragment {
    private FragmentInteractionCheckerBinding binding;
    private InteractionCheckerViewModel viewModel;

    private static final String[] SUBSTANCE_GROUPS = {
            "Alcohol",
            "Benzodiazepine",
            "Opioid",
            "SSRI",
            "MAOI",
            "MDMA",
            "Amphetamine",
            "Cannabis",
            "Ketamine",
            "Unknown"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInteractionCheckerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(InteractionCheckerViewModel.class);

        observeViewModel();

        List<String> suggestions = new ArrayList<>();
        for (String group : SUBSTANCE_GROUPS) addUnique(suggestions, group);
        for (String value : MedicationQueryResolver.suggestionsFor(requireContext().getApplicationContext())) addUnique(suggestions, value);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions);
        binding.inputFirst.setAdapter(adapter);
        binding.inputSecond.setAdapter(adapter);
        binding.inputFirst.setThreshold(0);
        binding.inputSecond.setThreshold(0);
        binding.buttonCheckInteraction.setOnClickListener(v -> checkInteraction());
    }

    private void observeViewModel() {
        viewModel.getAnalysis().observe(getViewLifecycleOwner(), analysis -> {
            if (analysis != null) {
                binding.textInteractionResult.setText(analysis);
            }
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                binding.textInteractionResult.setText(result.toString());
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Could show/hide loading indicator here if needed
        });
    }

    private void checkInteraction() {
        List<String> entries = interactionEntries();
        if (entries.size() < 2) {
            binding.textInteractionResult.setText("Enter at least two substances, medications, aliases, or saved medication names.");
            return;
        }

        binding.textInteractionResult.setText("Checking " + entries.size() + " entries against local rules and public reference APIs...");
        viewModel.checkMultipleInteractions(entries.toArray(new String[0]));
    }

    private List<String> interactionEntries() {
        List<String> entries = new ArrayList<>();
        addEntry(entries, binding.inputFirst.getText().toString());
        addEntry(entries, binding.inputSecond.getText().toString());
        splitEntries(entries, binding.inputAdditional.getText() == null ? "" : binding.inputAdditional.getText().toString());
        return entries;
    }

    private void splitEntries(List<String> entries, String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        String normalized = raw.replace("\n", "|").replace(" and ", "|");
        String[] parts = normalized.split("\\||,|;");
        for (String part : parts) addEntry(entries, part);
    }

    private void addEntry(List<String> entries, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) return;
        for (String entry : entries) {
            if (entry.equalsIgnoreCase(clean)) return;
        }
        entries.add(clean);
    }

    private void addUnique(List<String> entries, String value) {
        addEntry(entries, value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



