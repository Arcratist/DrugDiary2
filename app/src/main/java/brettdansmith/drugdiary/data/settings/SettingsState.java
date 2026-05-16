package brettdansmith.drugdiary.data.settings;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Immutable snapshot of global app preferences.
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
    public final boolean assistantResponseNotifications;
    public final boolean assistantEntryFromShareEnabled;
    public final boolean assistantEntryFromTextSelectionEnabled;
    public final AiProvider assistantProvider;
    public final int referenceCacheDays;
    public final boolean privateMode;
    public final boolean globalPrivacyCollection;

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
            boolean assistantResponseNotifications,
            boolean assistantEntryFromShareEnabled,
            boolean assistantEntryFromTextSelectionEnabled,
            AiProvider assistantProvider,
            int referenceCacheDays,
            boolean privateMode,
            boolean globalPrivacyCollection) {
        this.themeMode = themeMode;
        this.language = language == null ? LanguageOption.SYSTEM : language;
        this.unitSystem = unitSystem == null ? UnitSystem.SYSTEM : unitSystem;
        this.timeFormat = timeFormat == null ? TimeFormat.TWENTY_FOUR_HOUR : timeFormat;
        this.notificationsEnabled = notificationsEnabled;
        this.stealthNotifications = stealthNotifications;
        this.biometricUnlock = biometricUnlock;
        this.autoLock = autoLock;
        this.autoLockMinutes = Math.max(1, autoLockMinutes);
        this.defaultSixDigitPin = defaultSixDigitPin;
        this.showProfileSetupGuidance = showProfileSetupGuidance;
        this.assistantMemory = assistantMemory;
        this.assistantResponseNotifications = assistantResponseNotifications;
        this.assistantEntryFromShareEnabled = assistantEntryFromShareEnabled;
        this.assistantEntryFromTextSelectionEnabled = assistantEntryFromTextSelectionEnabled;
        this.assistantProvider = assistantProvider == null ? AiProvider.OPENAI : assistantProvider;
        this.referenceCacheDays = Math.max(1, referenceCacheDays);
        this.privateMode = privateMode;
        this.globalPrivacyCollection = globalPrivacyCollection;
    }

    public static SettingsState defaults() {
        return new SettingsState(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                LanguageOption.SYSTEM,
                UnitSystem.SYSTEM,
                TimeFormat.TWENTY_FOUR_HOUR,
                true,
                false,
                false,
                true,
                5,
                false,
                true,
                true,
                true,
                true,
                true,
                AiProvider.OPENAI,
                7,
                false,
                false);
    }
}
