package brettdansmith.drugdiary.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SettingsResolverTest {

    @Test
    public void effectiveSettingsResolvesOverridesCorrectly() {
        SettingsState global = SettingsState.defaults();
        // Global defaults: theme=FOLLOW_SYSTEM, language=SYSTEM, units=METRIC, assistantMemory=true

        UserSpecificSettings user = new UserSpecificSettings(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES, // themeOverride
                LanguageOption.SPANISH, // languageOverride
                UnitSystem.IMPERIAL, // unitsOverride
                true, // privateModeOverride
                AiProvider.GEMINI, // preferredAiOverride
                true, // aiProfileContext
                false, // aiMedicationContext
                true // aiLogContext
        );

        EffectiveSettings effective = EffectiveSettings.resolve(global, user);

        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES, effective.themeMode);
        assertEquals(LanguageOption.SPANISH, effective.language);
        assertEquals(UnitSystem.IMPERIAL, effective.unitSystem);
        assertTrue(effective.privateMode);
        assertEquals(AiProvider.GEMINI, effective.aiProvider);
        assertTrue(effective.aiProfileContext);
        assertFalse(effective.aiMedicationContext);
        assertTrue(effective.aiLogContext);
    }

    @Test
    public void effectiveSettingsFallsBackToGlobalWhenNoOverride() {
        SettingsState global = SettingsState.defaults();
        UserSpecificSettings user = UserSpecificSettings.empty(); // Defaults: aiProfileContext=true, aiMedicationContext=true, aiLogContext=true

        EffectiveSettings effective = EffectiveSettings.resolve(global, user);

        assertEquals(global.themeMode, effective.themeMode);
        assertEquals(global.language, effective.language);
        UnitSystem expectedUnits = global.unitSystem == UnitSystem.SYSTEM ? UnitSystem.getSystemDefault() : global.unitSystem;
        assertEquals(expectedUnits, effective.unitSystem);
        assertEquals(global.privateMode, effective.privateMode);
        assertEquals(global.assistantProvider, effective.aiProvider);
        
        // AI context settings are now exclusively in user settings and default to true
        assertTrue(effective.aiProfileContext);
        assertTrue(effective.aiMedicationContext);
        assertTrue(effective.aiLogContext);
    }
}
