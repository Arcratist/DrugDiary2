package brettdansmith.drugdiary.ui.medications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.databinding.FragmentMedicationListBinding;
import brettdansmith.drugdiary.domain.model.medication.MedicationDoseLog;
import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.ui.assistant.AssistantIntegration;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;

public class MedicationListFragment extends Fragment {

    private static final String ARG_FILTER_MODE = "filter_mode";

    private FragmentMedicationListBinding binding;
    private MedicationListViewModel viewModel;
    private MedicationAdapter adapter;
    private String filterMode;

    public static MedicationListFragment newInstance(String filterMode) {
        MedicationListFragment fragment = new MedicationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER_MODE, filterMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filterMode = getArguments().getString(ARG_FILTER_MODE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMedicationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory(requireContext()))
                .get(MedicationListViewModel.class);

        setupRecyclerView();

        viewModel.getFilteredMedications().observe(getViewLifecycleOwner(), medicationMap -> {
            if (medicationMap.containsKey(filterMode)) {
                adapter.submitList(medicationMap.get(filterMode));
                boolean empty = medicationMap.get(filterMode) == null || medicationMap.get(filterMode).isEmpty();
                binding.textEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MedicationAdapter(new MedicationAdapter.OnMedicationClickListener() {
            @Override
            public void onMedicationClick(MedicationRecord medication) {
                MedicationOverviewDialog.show(requireContext(), medication, viewModel.getNetworkExecutor(), () -> viewModel.loadMedications());
            }

            @Override
            public void onAskAssistant(String medicationName) {
                String prompt = "Medication context request: /drugdata " + medicationName
                        + "\nPlease summarize key safety points, interactions, common adverse effects, and practical monitoring steps."
                        + "\nKeep this as harm-minimisation guidance, not diagnosis.";
                AssistantIntegration.askAboutPrefill(MedicationListFragment.this, prompt, false);
            }

            @Override
            public void onLogDose(MedicationRecord medication) {
                try {
                    double amount = parseDoseAmount(medication.strength);
                    String unit = parseDoseUnit(medication.strength);
                    new MedicationRepository(requireContext()).addDoseLog(
                            new MedicationDoseLog("", medication.id, amount, unit, medication.route, "taken", "Quick dose log", System.currentTimeMillis()));
                    Toast.makeText(requireContext(), getString(R.string.medication_dose_logged, medication.name), Toast.LENGTH_SHORT).show();
                    viewModel.loadMedications();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.medication_log_failed, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onEditMedication(MedicationRecord medication) {
                MedicationEditorDialog.showEdit(requireContext(), viewModel.getNetworkExecutor(), medication, () -> viewModel.loadMedications());
            }

            @Override
            public void onDeleteMedication(MedicationRecord medication) {
                showDeleteMedicationDialog(medication);
            }
        });
        binding.recyclerMedications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMedications.setAdapter(adapter);
    }

    private void showDeleteMedicationDialog(MedicationRecord medication) {
        if (medication == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.medication_delete_confirm_title)
                .setMessage(getString(R.string.medication_delete_confirm_body, medication.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        new MedicationRepository(requireContext()).delete(medication.id);
                        viewModel.loadMedications();
                        Toast.makeText(requireContext(), getString(R.string.medication_deleted, medication.name), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), R.string.medication_delete_failed, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private double parseDoseAmount(String strength) {
        if (strength == null) return 0;
        String clean = strength.trim().toLowerCase(java.util.Locale.US);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(clean);
        if (!matcher.find()) return 0;
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String parseDoseUnit(String strength) {
        if (strength == null) return "mg";
        String clean = strength.toLowerCase(java.util.Locale.US);
        if (clean.contains("mcg")) return "mcg";
        if (clean.contains(" g")) return "g";
        if (clean.contains("ml")) return "ml";
        return "mg";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
