package brettdansmith.drugdiary.ui.resources;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import brettdansmith.drugdiary.databinding.FragmentResourcesBinding;
import brettdansmith.drugdiary.domain.model.resources.ResourceCategory;
import brettdansmith.drugdiary.domain.model.resources.SupportResource;
import brettdansmith.drugdiary.domain.model.resources.SupportResourceProvider;

public class ResourcesFragment extends Fragment {
    private FragmentResourcesBinding binding;
    private SupportResourceAdapter adapter;
    private List<SupportResource> allResources;

    private String currentSearchQuery = "";
    private String currentFilter = null; // Can be region or category

    private View root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (root == null) {
            binding = FragmentResourcesBinding.inflate(inflater, container, false);
            root = binding.getRoot();
        }
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (adapter != null) {
            return; // Already initialized
        }

        adapter = new SupportResourceAdapter();
        binding.recyclerResources.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResources.setHasFixedSize(true);
        binding.recyclerResources.setAdapter(adapter);

        // Defer heavy list/UI operations using postDelayed to guarantee the Fragment 
        // enter animation finishes completely before we freeze the main thread with RecyclerView inflation.
        view.postDelayed(() -> {
            if (binding == null) return;
            allResources = SupportResourceProvider.getResources();
            setupSearch();
            setupFilters();
            setupQuickActions();
            filterList();
        }, 150); // 150ms delay for UI transition to complete
    }

    private void setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = null;
            } else {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    String text = chip.getText().toString();
                    currentFilter = text.equals("All") ? null : text;
                }
            }
            filterList();
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().toLowerCase();
                filterList();
            }
        });
    }

    private void setupQuickActions() {
        binding.cardActionOverdose.setOnClickListener(v -> setFilter("Poisoning/Overdose"));
        binding.cardActionSupport.setOnClickListener(v -> setFilter("Drug & Alcohol"));
        binding.cardActionMental.setOnClickListener(v -> setFilter("Mental Health Crisis"));
        binding.cardActionRecovery.setOnClickListener(v -> setFilter("Recovery"));
    }

    private void setFilter(String filterText) {
        for (int i = 0; i < binding.chipGroupFilters.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupFilters.getChildAt(i);
            if (chip.getText().toString().equals(filterText)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void filterList() {
        List<SupportResource> filtered = allResources.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilter)
                .sorted((r1, r2) -> {
                    if (r1.getCategories().isEmpty() && r2.getCategories().isEmpty()) return 0;
                    if (r1.getCategories().isEmpty()) return 1;
                    if (r2.getCategories().isEmpty()) return -1;
                    return r1.getCategories().get(0).compareTo(r2.getCategories().get(0));
                })
                .collect(Collectors.toList());
        adapter.submitList(filtered);
    }

    private boolean matchesSearch(SupportResource resource) {
        if (currentSearchQuery.isEmpty()) return true;
        
        return (resource.getName() != null && resource.getName().toLowerCase().contains(currentSearchQuery)) ||
               (resource.getDescription() != null && resource.getDescription().toLowerCase().contains(currentSearchQuery)) ||
               (resource.getRegion() != null && resource.getRegion().toLowerCase().contains(currentSearchQuery));
    }

    private boolean matchesFilter(SupportResource resource) {
        if (currentFilter == null) return true;

        if (currentFilter.equals("Global") || currentFilter.equals("Australia") || 
            currentFilter.equals("United States") || currentFilter.equals("United Kingdom")) {
            return resource.getRegion() != null && resource.getRegion().contains(currentFilter);
        }

        switch (currentFilter) {
            case "Emergency":
                return resource.getCategories().contains(ResourceCategory.EMERGENCY);
            case "Poisoning/Overdose":
                return resource.getCategories().contains(ResourceCategory.POISONING_OVERDOSE);
            case "Drug & Alcohol":
                return resource.getCategories().contains(ResourceCategory.DRUG_ALCOHOL);
            case "Mental Health Crisis":
                return resource.getCategories().contains(ResourceCategory.MENTAL_HEALTH_CRISIS);
            case "Recovery":
                return resource.getCategories().contains(ResourceCategory.RECOVERY_MEETINGS);
            case "Family/Friends":
                return resource.getCategories().contains(ResourceCategory.FAMILY_FRIENDS);
            default:
                return true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Do not set binding to null, because we are caching the root view!
    }
}
