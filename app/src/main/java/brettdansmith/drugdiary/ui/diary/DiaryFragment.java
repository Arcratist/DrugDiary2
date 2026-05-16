package brettdansmith.drugdiary.ui.diary;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;

import java.util.List;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.diary.DiaryRepository;
import brettdansmith.drugdiary.databinding.FragmentDiaryBinding;
import brettdansmith.drugdiary.domain.units.TimeFormatter;
import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.model.diary.MoodCheckIn;
import brettdansmith.drugdiary.domain.model.diary.SleepLog;
import brettdansmith.drugdiary.domain.model.diary.SymptomLog;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;
import brettdansmith.drugdiary.ui.diary.DiaryListViewModel;

public class DiaryFragment extends Fragment {
    private FragmentDiaryBinding binding;
    private DiaryListViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDiaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(DiaryListViewModel.class);

        observeViewModel();

        binding.buttonQuickMood.setOnClickListener(v -> showEntryDialog("Mood check-in", "How are you feeling now?", 6, 4, 4, 6, "", ""));
        binding.buttonQuickMedication.setOnClickListener(v -> showEntryDialog("Medication effect", "Dose/effect notes", 5, 3, 3, 5, "", ""));
        binding.buttonQuickIssue.setOnClickListener(v -> showEntryDialog("Symptom note", "What symptoms are present?", 4, 5, 5, 4, "", ""));

        viewModel.loadEntries();
    }

    private void observeViewModel() {
        viewModel.getEntries().observe(getViewLifecycleOwner(), entries -> {
            refreshSummaryDisplay(entries);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshSummaryDisplay(List<DiaryEntry> entries) {
        int count = entries.size();
        String last = getString(R.string.no_logs_yet);
        if (count > 0) {
            DiaryEntry item = entries.get(0);
            last = item.title + "\n" + TimeFormatter.formatDateTime(requireContext(), item.createdAt);
        }
        String summary = getString(R.string.diary_summary, count, last);
        binding.textDiarySummary.setText(summary);
    }

    private void showEntryDialog(String title, String hint, int mood, int anxiety, int stress, int energy, String symptomName, String notes) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_diary_entry, null, false);
        EditText input = content.findViewById(R.id.input_diary_notes);
        input.setHint(hint);
        input.setText(notes);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> addEntry(title, input.getText() == null ? "" : input.getText().toString(), mood, anxiety, stress, energy, symptomName))
                .show();
    }

    private void addEntry(String title, String notes, int mood, int anxiety, int stress, int energy, String symptomName) {
        DiaryEntry entry = new DiaryEntry(
                "",
                title,
                notes,
                new MoodCheckIn(mood, anxiety, stress, energy),
                new SleepLog(0, 0),
                new SymptomLog(symptomName, stress),
                "",
                new JSONArray(),
                System.currentTimeMillis());
        viewModel.saveEntry(entry);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
