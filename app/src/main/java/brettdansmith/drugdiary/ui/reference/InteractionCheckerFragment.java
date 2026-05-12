package brettdansmith.drugdiary.ui.reference;

import brettdansmith.drugdiary.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import brettdansmith.drugdiary.databinding.FragmentInteractionCheckerBinding;
import brettdansmith.drugdiary.domain.medication.MedicationQueryResolver;
import brettdansmith.drugdiary.reference.DrugInteractionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InteractionCheckerFragment extends Fragment {
    private FragmentInteractionCheckerBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

    private void checkInteraction() {
        List<String> entries = interactionEntries();
        if (entries.size() < 2) {
            binding.textInteractionResult.setText("Enter at least two substances, medications, aliases, or saved medication names.");
            return;
        }

        binding.textInteractionResult.setText("Checking " + entries.size() + " entries against local rules and public reference APIs...");
        executor.execute(() -> {
            String output;
            try {
                output = DrugInteractionRepository.checkMultiple(requireContext().getApplicationContext(), entries.toArray(new String[0]));
            } catch (Exception e) {
                output = "Public database interaction check failed.\n\n" + e.getMessage();
            }
            String finalOutput = output;
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) binding.textInteractionResult.setText(finalOutput);
                });
            }
        });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}



