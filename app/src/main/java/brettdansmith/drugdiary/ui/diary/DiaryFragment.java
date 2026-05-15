package brettdansmith.drugdiary.ui.diary;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.diary.DiaryRepository;
import brettdansmith.drugdiary.databinding.FragmentDiaryBinding;
import brettdansmith.drugdiary.domain.units.TimeFormatter;
import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.model.diary.MoodCheckIn;
import brettdansmith.drugdiary.domain.model.diary.SleepLog;
import brettdansmith.drugdiary.domain.model.diary.SymptomLog;

public class DiaryFragment extends Fragment {
    private FragmentDiaryBinding binding;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
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
        binding.buttonQuickMood.setOnClickListener(v -> showEntryDialog("Mood check-in", "How are you feeling now?", 6, 4, 4, 6, "", ""));
        binding.buttonQuickMedication.setOnClickListener(v -> showEntryDialog("Medication effect", "Dose/effect notes", 5, 3, 3, 5, "", ""));
        binding.buttonQuickIssue.setOnClickListener(v -> showEntryDialog("Symptom note", "What symptoms are present?", 4, 5, 5, 4, "", ""));
        refreshSummary();
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
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
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
                new DiaryRepository(appContext).addEntry(entry);
                refreshSummary();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (binding == null) return;
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.diary_save_failed_title)
                            .setMessage(R.string.diary_save_failed_body)
                            .setPositiveButton(R.string.continue_button, null)
                            .show();
                });
            }
        });
    }

    private void refreshSummary() {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            List<DiaryEntry> entries = new DiaryRepository(appContext).listEntries();
            int count = entries.size();
            String last = getString(R.string.no_logs_yet);
            if (count > 0) {
                DiaryEntry item = entries.get(0);
                last = item.title + "\n" + TimeFormatter.formatDateTime(appContext, item.createdAt);
            }
            String summary = getString(R.string.diary_summary, count, last);
            mainHandler.post(() -> {
                if (binding != null) binding.textDiarySummary.setText(summary);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        diskExecutor.shutdownNow();
    }
}
