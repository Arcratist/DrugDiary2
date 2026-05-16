package brettdansmith.drugdiary.data.settings;

import androidx.annotation.NonNull;

/**
 * Computed runtime settings used by the app.
 * Derived from GlobalAppSettings + active UserSpecificSettings overrides.
 */
public final class EffectiveSettings {
    public final int themeMode;
    public final LanguageOption language;
    public final UnitSystem unitSystem;
    public final boolean privateMode;
    public final AiProvider aiProvider;
    public final boolean aiProfileContext;
    public final boolean aiMedicationContext;
    public final boolean aiLogContext;

    public EffectiveSettings(
            int themeMode,
            @NonNull LanguageOption language,
            @NonNull UnitSystem unitSystem,
            boolean privateMode) {
        this(themeMode, language, unitSystem, privateMode, AiProvider.OPENAI, true, true, true);
    }

    public EffectiveSettings(
            int themeMode,
            @NonNull LanguageOption language,
            @NonNull UnitSystem unitSystem,
            boolean privateMode,
            @NonNull AiProvider aiProvider,
            boolean aiProfileContext,
            boolean aiMedicationContext,
            boolean aiLogContext) {
        this.themeMode = themeMode;
        this.language = language;
        this.unitSystem = unitSystem;
        this.privateMode = privateMode;
        this.aiProvider = aiProvider;
        this.aiProfileContext = aiProfileContext;
        this.aiMedicationContext = aiMedicationContext;
        this.aiLogContext = aiLogContext;
    }

    public static EffectiveSettings resolve(
            @NonNull SettingsState global,
            @NonNull UserSpecificSettings user) {
        
        int theme = (user.themeOverride != null) ? user.themeOverride : global.themeMode;
        
        LanguageOption lang = (user.languageOverride != null) ? user.languageOverride : global.language;
        
        UnitSystem resolvedUnits = (user.unitsOverride != null) ? user.unitsOverride : global.unitSystem;
        if (resolvedUnits == UnitSystem.SYSTEM) {
            resolvedUnits = UnitSystem.getSystemDefault();
        }

        boolean priv = (user.privateModeOverride != null) ? user.privateModeOverride : global.privateMode;
        AiProvider ai = (user.preferredAiOverride != null) ? user.preferredAiOverride : global.assistantProvider;

        // AI context settings are now direct in user settings
        boolean aiProfile = user.aiProfileContext;
        boolean aiMedication = user.aiMedicationContext;
        boolean aiLog = user.aiLogContext;

        return new EffectiveSettings(theme, lang, resolvedUnits, priv, ai, aiProfile, aiMedication, aiLog);
    }
}
