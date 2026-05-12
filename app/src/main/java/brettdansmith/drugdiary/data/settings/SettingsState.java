package brettdansmith.drugdiary.data.settings;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Immutable snapshot of global app preferences. Profile vault data is intentionally
 * absent here: settings can be read before login, but encrypted profile data cannot.
 */
public final class SettingsState {
    public final int themeMode;
    public final LanguageOption language;
    public final UnitSystem unitSystem;
    public final TimeFormat timeFormat;
    public final boolean notificationsEnabled;
    public final boolean stealthNotifications;
    public final boolean biometricUnlock;
    public final boolean autoLock;
    public final int autoLockMinutes;
    public final boolean defaultSixDigitPin;
    public final boolean showProfileSetupGuidance;
    public final boolean assistantMemory;
    public final boolean assistantProfileContext;
    public final boolean assistantMedicationContext;
    public final boolean assistantLogContext;
    public final boolean assistantResponseNotifications;
    public final AiProvider assistantProvider;
    public final int referenceCacheDays;

    public SettingsState(
            int themeMode,
            LanguageOption language,
            UnitSystem unitSystem,
            TimeFormat timeFormat,
            boolean notificationsEnabled,
            boolean stealthNotifications,
            boolean biometricUnlock,
            boolean autoLock,
            int autoLockMinutes,
            boolean defaultSixDigitPin,
            boolean showProfileSetupGuidance,
            boolean assistantMemory,
            boolean assistantProfileContext,
            boolean assistantMedicationContext,
            boolean assistantLogContext,
            boolean assistantResponseNotifications,
            AiProvider assistantProvider,
            int referenceCacheDays) {
        this.themeMode = themeMode;
        this.language = language == null ? LanguageOption.SYSTEM : language;
        this.unitSystem = unitSystem == null ? UnitSystem.METRIC : unitSystem;
        this.timeFormat = timeFormat == null ? TimeFormat.TWENTY_FOUR_HOUR : timeFormat;
        this.notificationsEnabled = notificationsEnabled;
        this.stealthNotifications = stealthNotifications;
        this.biometricUnlock = biometricUnlock;
        this.autoLock = autoLock;
        this.autoLockMinutes = Math.max(1, autoLockMinutes);
        this.defaultSixDigitPin = defaultSixDigitPin;
        this.showProfileSetupGuidance = showProfileSetupGuidance;
        this.assistantMemory = assistantMemory;
        this.assistantProfileContext = assistantProfileContext;
        this.assistantMedicationContext = assistantMedicationContext;
        this.assistantLogContext = assistantLogContext;
        this.assistantResponseNotifications = assistantResponseNotifications;
        this.assistantProvider = assistantProvider == null ? AiProvider.OPENAI : assistantProvider;
        this.referenceCacheDays = Math.max(1, referenceCacheDays);
    }

    public static SettingsState defaults() {
        return new SettingsState(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                LanguageOption.SYSTEM,
                UnitSystem.METRIC,
                TimeFormat.TWENTY_FOUR_HOUR,
                true,
                false,
                false,
                true,
                5,
                false,
                true,
                true,
                false,
                true,
                false,
                true,
                AiProvider.OPENAI,
                7);
    }
}

