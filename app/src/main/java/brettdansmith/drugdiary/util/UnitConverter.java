package brettdansmith.drugdiary.util;

import brettdansmith.drugdiary.domain.units.UnitPreferences;

/**
 * Compatibility wrapper while callers migrate to domain.units.UnitConverter.
 */
@Deprecated
public final class UnitConverter {
    private UnitConverter() {
    }

    public static double poundsToKilograms(double pounds) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.poundsToKilograms(pounds);
    }

    public static double kilogramsToPounds(double kilograms) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.kilogramsToPounds(kilograms);
    }

    public static double inchesToCentimeters(double inches) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.inchesToCentimeters(inches);
    }

    public static double centimetersToInches(double centimeters) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.centimetersToInches(centimeters);
    }

    public static double fluidOuncesToMilliliters(double fluidOunces) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.fluidOuncesToMilliliters(fluidOunces);
    }

    public static double millilitersToFluidOunces(double milliliters) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.millilitersToFluidOunces(milliliters);
    }

    public static String formatWeight(double kilograms, boolean metric) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.formatWeight(kilograms,
                new UnitPreferences(metric
                        ? brettdansmith.drugdiary.data.settings.UnitSystem.METRIC
                        : brettdansmith.drugdiary.data.settings.UnitSystem.IMPERIAL));
    }

    public static String formatHeight(double centimeters, boolean metric) {
        return brettdansmith.drugdiary.domain.units.UnitConverter.formatHeight(centimeters,
                new UnitPreferences(metric
                        ? brettdansmith.drugdiary.data.settings.UnitSystem.METRIC
                        : brettdansmith.drugdiary.data.settings.UnitSystem.IMPERIAL));
    }
}

