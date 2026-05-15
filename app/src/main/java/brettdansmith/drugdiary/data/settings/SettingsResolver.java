package brettdansmith.drugdiary.data.settings;

import android.content.Context;
import org.json.JSONObject;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileRepository;
import brettdansmith.drugdiary.security.UserSession;

public final class SettingsResolver {

    public static EffectiveSettings getEffectiveSettings(Context context) {
        SettingsRepository globalRepo = new SettingsRepository(context);
        SettingsState globalState = globalRepo.getState();

        if (!UserSession.getInstance().isActive()) {
            UnitSystem resolvedUnits = globalState.unitSystem;
            if (resolvedUnits == UnitSystem.SYSTEM) {
                resolvedUnits = UnitSystem.getSystemDefault();
            }
            return new EffectiveSettings(
                    globalState.themeMode,
                    globalState.language,
                    resolvedUnits,
                    globalState.privateMode,
                    globalState.hideDashboardSensitive
            );
        }

        UserSpecificSettings userSettings = getUserSpecificSettings(context);
        return EffectiveSettings.resolve(globalState, userSettings);
    }

    public static UserSpecificSettings getUserSpecificSettings(Context context) {
        if (!UserSession.getInstance().isActive()) {
            return UserSpecificSettings.empty();
        }
        try {
            JSONObject data = new ProfileRepository(context).load();
            JSONObject userSettingsJson = data.optJSONObject(ProfileJson.KEY_USER_SETTINGS);
            return UserSpecificSettings.fromJson(userSettingsJson);
        } catch (Exception e) {
            return UserSpecificSettings.empty();
        }
    }

    public static void saveUserSpecificSettings(Context context, UserSpecificSettings settings) {
        if (!UserSession.getInstance().isActive()) return;
        try {
            ProfileRepository repo = new ProfileRepository(context);
            JSONObject data = repo.load();
            data.put(ProfileJson.KEY_USER_SETTINGS, settings.toJson());
            repo.save(data);
        } catch (Exception ignored) {}
    }
}