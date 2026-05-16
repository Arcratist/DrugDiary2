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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import brettdansmith.drugdiary.databinding.FragmentResourcesBinding;
import brettdansmith.drugdiary.domain.model.resources.ResourceRegion;
import brettdansmith.drugdiary.domain.model.resources.SupportResource;
import brettdansmith.drugdiary.domain.model.resources.SupportResourceRegistry;
import brettdansmith.drugdiary.ui.assistant.AssistantIntegration;

public class ResourcesFragment extends Fragment {
    private FragmentResourcesBinding binding;
    private SupportResourceAdapter adapter;

    private String query = "";
    private ResourceRegion selectedRegion = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResourcesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new SupportResourceAdapter((resource, privateChat) -> {
            String prompt = SupportResourceRegistry.buildAssistantSuggestionContext(query, selectedRegion)
                    + " Selected resource: "
                    + SupportResourceRegistry.buildAssistantContext(resource);
            AssistantIntegration.askAbout(this, prompt, privateChat);
        });
        binding.recyclerResources.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResources.setAdapter(adapter);

        bindSearch();
        bindFilters();
        applyFilters();
    }

    private void bindSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString();
                applyFilters();
            }
        });
    }

    private void bindFilters() {
        binding.chipGroupRegionFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedRegion = parseRegionFromChip(group, checkedIds);
            applyFilters();
        });
    }

    private ResourceRegion parseRegionFromChip(com.google.android.material.chip.ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds == null || checkedIds.isEmpty()) return null;
        Chip chip = group.findViewById(checkedIds.get(0));
        if (chip == null || chip.getTag() == null) return null;
        String tag = chip.getTag().toString().trim();
        if (tag.isEmpty()) return null;
        try {
            return ResourceRegion.valueOf(tag);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void applyFilters() {
        List<SupportResource> filtered = new ArrayList<>(
                SupportResourceRegistry.search(query, selectedRegion, null));
        Collections.sort(filtered, resourceComparator());
        adapter.submitList(filtered);
    }

    private Comparator<SupportResource> resourceComparator() {
        final String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
        return (a, b) -> {
            int c = Integer.compare(queryScore(b, q), queryScore(a, q));
            if (c != 0) return c;

            c = Integer.compare(regionRank(a), regionRank(b));
            if (c != 0) return c;

            return a.getName().compareToIgnoreCase(b.getName());
        };
    }

    private int queryScore(SupportResource resource, String q) {
        if (q.isEmpty()) return 0;
        String name = resource.getName() == null ? "" : resource.getName().toLowerCase(Locale.US);
        String description = resource.getDescription() == null ? "" : resource.getDescription().toLowerCase(Locale.US);
        if (name.startsWith(q)) return 3;
        if (name.contains(q)) return 2;
        if (description.contains(q)) return 1;
        return 0;
    }

    private int regionRank(SupportResource resource) {
        if (resource == null || resource.getRegion() == null) return 99;
        switch (resource.getRegion()) {
            case WORLDWIDE: return 0;
            case AU: return 1;
            case US: return 2;
            case UK: return 3;
            default: return 99;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
