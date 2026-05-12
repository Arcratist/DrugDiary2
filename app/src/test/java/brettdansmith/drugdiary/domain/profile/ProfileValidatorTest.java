package brettdansmith.drugdiary.domain.profile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.domain.units.UnitPreferences;

public class ProfileValidatorTest {
    @Test
    public void rejectsInvalidPinLengths() {
        assertFalse(ProfileValidator.validatePin("123", false).valid);
        assertTrue(ProfileValidator.validatePin("1234", false).valid);
        assertTrue(ProfileValidator.validatePin("123456", true).valid);
    }

    @Test
    public void validatesImperialDisplayRangesAfterMetricNormalization() {
        UnitPreferences imperial = new UnitPreferences(UnitSystem.IMPERIAL);
        assertTrue(ProfileValidator.validateDisplayWeight(150, imperial).valid);
        assertFalse(ProfileValidator.validateDisplayWeight(2, imperial).valid);
        assertTrue(ProfileValidator.validateDisplayHeight(70, imperial).valid);
        assertFalse(ProfileValidator.validateDisplayHeight(8, imperial).valid);
    }
}
