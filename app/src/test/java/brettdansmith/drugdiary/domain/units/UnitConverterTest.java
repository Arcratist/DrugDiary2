package brettdansmith.drugdiary.domain.units;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import brettdansmith.drugdiary.data.settings.UnitSystem;

public class UnitConverterTest {
    @Test
    public void imperialWeightInputNormalizesToKilograms() {
        UnitPreferences imperial = new UnitPreferences(UnitSystem.IMPERIAL);
        assertEquals(90.718, UnitConverter.normalizeDisplayWeight(200, imperial), 0.001);
    }

    @Test
    public void imperialHeightInputNormalizesToCentimeters() {
        UnitPreferences imperial = new UnitPreferences(UnitSystem.IMPERIAL);
        assertEquals(177.8, UnitConverter.normalizeDisplayHeight(70, imperial), 0.001);
    }
}
