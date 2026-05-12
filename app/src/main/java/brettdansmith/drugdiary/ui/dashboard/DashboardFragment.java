package brettdansmith.drugdiary.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.diary.DiaryRepository;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.databinding.FragmentDashboardBinding;
import brettdansmith.drugdiary.domain.units.TimeFormatter;
import brettdansmith.drugdiary.domain.units.UnitConverter;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.model.diary.DiaryEntry;
import brettdansmith.drugdiary.model.medication.MedicationRecord;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadDashboardData();

        binding.buttonAddEntry.setOnClickListener(v -> {
            if (getParentFragmentManager().findFragmentByTag("diary") == null) {
                androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.diaryFragment);
            }
        });
    }

    private void loadDashboardData() {
        SharedPreferences prefs = requireContext().getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        String currentProfile = prefs.getString(ProfileSelectorFragment.KEY_LAST_PROFILE, "Guest");
        binding.textGreeting.setText(getString(R.string.dashboard_greeting, currentProfile));

        Context appContext = requireContext().getApplicationContext();
        UnitPreferences preferences = UnitPreferences.from(requireContext());
        boolean privateMode = AppSettings.privateModeEnabled(requireContext());
        boolean hideSensitive = privateMode || AppSettings.hideDashboardSensitive(requireContext());
        diskExecutor.execute(() -> {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            JSONObject profile = data.optJSONObject(ProfileJson.KEY_PROFILE);
            JSONObject privacy = data.optJSONObject(ProfileJson.KEY_PRIVACY);
            if (profile == null) profile = new JSONObject();
            if (privacy == null) privacy = new JSONObject();

            boolean show = privacy.optBoolean("show_on_dashboard", true) && !hideSensitive;
            String age = profile.optString("age", "--");
            String weight = UnitConverter.formatWeight(profile.optDouble("weight_kg", 0), preferences);
            String height = UnitConverter.formatHeight(profile.optDouble("height_cm", 0), preferences);

            List<MedicationRecord> meds = new MedicationRepository(appContext).listRecords();
            int activeCount = 0;
            int prnCount = 0;
            int favouriteCount = 0;
            for (MedicationRecord med : meds) {
                if (med.active) activeCount++;
                if (med.prn) prnCount++;
                if (med.favorite) favouriteCount++;
            }

            List<DiaryEntry> diaryEntries = new DiaryRepository(appContext).listEntries();
            String recentDiary = diaryEntries.isEmpty()
                    ? getString(R.string.no_logs_yet)
                    : diaryEntries.get(0).title + "\n" + TimeFormatter.formatDateTime(appContext, diaryEntries.get(0).createdAt);

            String medSummary = hideSensitive
                    ? getString(R.string.dashboard_private_mode_summary)
                    : getString(R.string.dashboard_medication_summary, activeCount, prnCount, favouriteCount);

            mainHandler.post(() -> {
                if (binding == null) return;
                binding.cardProfileSummary.setVisibility(show ? View.VISIBLE : View.GONE);
                if (show) {
                    binding.textVitalsSummary.setText(getString(R.string.dashboard_vitals, age, weight, height));
                }
                binding.textTodayMedications.setText(medSummary);
                binding.textRecentLogs.setText(hideSensitive ? getString(R.string.dashboard_private_mode_summary) : recentDiary);
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

