package brettdansmith.drugdiary.domain.units;

import java.util.Locale;

/**
 * Central conversion point for normalized storage units.
 *
 * Storage invariants:
 * weight is kilograms, height is centimeters, volume is milliliters,
 * dose mass is milligrams, and timestamps are epoch millis.
 */
public final class UnitConverter {
    private static final double KG_TO_LB = 2.2046226218;
    private static final double CM_TO_IN = 0.3937007874;
    private static final double ML_TO_FL_OZ = 0.0338140227;
    private static final double G_TO_MG = 1_000.0;
    private static final double MCG_TO_MG = 0.001;

    private UnitConverter() {
    }

    public static double poundsToKilograms(double pounds) {
        return pounds / KG_TO_LB;
    }

    public static double kilogramsToPounds(double kilograms) {
        return kilograms * KG_TO_LB;
    }

    public static double inchesToCentimeters(double inches) {
        return inches / CM_TO_IN;
    }

    public static double centimetersToInches(double centimeters) {
        return centimeters * CM_TO_IN;
    }

    public static double fluidOuncesToMilliliters(double fluidOunces) {
        return fluidOunces / ML_TO_FL_OZ;
    }

    public static double millilitersToFluidOunces(double milliliters) {
        return milliliters * ML_TO_FL_OZ;
    }

    public static double gramsToMilligrams(double grams) {
        return grams * G_TO_MG;
    }

    public static double microgramsToMilligrams(double micrograms) {
        return micrograms * MCG_TO_MG;
    }

    public static double normalizeDisplayWeight(double displayedWeight, UnitPreferences preferences) {
        return preferences.isMetric() ? displayedWeight : poundsToKilograms(displayedWeight);
    }

    public static double normalizeDisplayHeight(double displayedHeight, UnitPreferences preferences) {
        return preferences.isMetric() ? displayedHeight : inchesToCentimeters(displayedHeight);
    }

    public static String formatWeight(double kilograms, UnitPreferences preferences) {
        if (Double.isNaN(kilograms) || kilograms <= 0) {
            return "--";
        }
        if (preferences.isMetric()) {
            return String.format(Locale.getDefault(), "%.1f kg", kilograms);
        }
        return String.format(Locale.getDefault(), "%.1f lb", kilogramsToPounds(kilograms));
    }

    public static String formatHeight(double centimeters, UnitPreferences preferences) {
        if (Double.isNaN(centimeters) || centimeters <= 0) {
            return "--";
        }
        if (preferences.isMetric()) {
            return String.format(Locale.getDefault(), "%.0f cm", centimeters);
        }
        double inches = centimetersToInches(centimeters);
        int feet = (int) Math.floor(inches / 12.0);
        int remainingInches = (int) Math.round(inches - feet * 12.0);
        if (remainingInches == 12) {
            feet++;
            remainingInches = 0;
        }
        return feet + " ft " + remainingInches + " in";
    }

    public static String formatVolume(double milliliters, UnitPreferences preferences) {
        if (Double.isNaN(milliliters) || milliliters <= 0) {
            return "--";
        }
        if (preferences.isMetric()) {
            return String.format(Locale.getDefault(), "%.1f ml", milliliters);
        }
        return String.format(Locale.getDefault(), "%.2f fl oz", millilitersToFluidOunces(milliliters));
    }
}

