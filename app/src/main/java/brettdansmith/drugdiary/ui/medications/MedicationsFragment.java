package brettdansmith.drugdiary.ui.medications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.tabs.TabLayoutMediator;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.databinding.FragmentMedicationsBinding;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;
import brettdansmith.drugdiary.ui.medications.MedicationListViewModel;
import android.widget.Toast;

public class MedicationsFragment extends Fragment {

    private FragmentMedicationsBinding binding;
    private MedicationListViewModel viewModel;
    private MedicationsViewPagerAdapter viewPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMedicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory(requireContext()))
                .get(MedicationListViewModel.class);

        setupViewPager();
        setupTabs();
        setupToolbar();
        observeViewModel();

        binding.fabAddMedication.setOnClickListener(v ->
                MedicationEditorDialog.showAdd(requireContext(), null, () -> viewModel.loadMedications()));

        viewModel.loadMedications();
    }

    private void observeViewModel() {
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Could show/hide loading indicator here if needed
        });
    }

    private void setupViewPager() {
        viewPagerAdapter = new MedicationsViewPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(viewPagerAdapter);
    }

    private void setupTabs() {
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) ->
                tab.setText(getString(MedicationListViewModel.FilterMode.values()[position].titleRes))).attach();
    }

    private void setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        viewModel.search(query);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        viewModel.search(newText);
                        return true;
                    }
                });
                return true;
            }
            if (item.getItemId() == R.id.action_sort) {
                viewModel.loadMedications();
                return true;
            }
            if (item.getItemId() == R.id.action_import_list) {
                showImportListDialog();
                return true;
            }
            return false;
        });
    }

    private void showImportListDialog() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_import_medication_list, null, false);
        TextInputEditText input = content.findViewById(R.id.input_import_list);
        MaterialSwitch markActiveSwitch = content.findViewById(R.id.switch_import_active);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.medication_import_title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.medication_import_list, (dialog, which) -> {
                    String raw = input.getText() == null ? "" : input.getText().toString();
                    MedicationListImportParser.Result result = MedicationListImportParser.parse(raw, markActiveSwitch.isChecked());
                    if (result.records().isEmpty()) {
                        Toast.makeText(requireContext(), R.string.medication_import_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    MedicationRepository repository = new MedicationRepository(requireContext());
                    int imported = 0;
                    for (brettdansmith.drugdiary.domain.model.medication.MedicationRecord record : result.records()) {
                        try {
                            repository.upsertRecord(record);
                            imported++;
                        } catch (Exception ignored) {
                        }
                    }
                    viewModel.loadMedications();
                    Toast.makeText(requireContext(), getString(R.string.medication_import_result, imported, result.skipped()), Toast.LENGTH_LONG).show();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
