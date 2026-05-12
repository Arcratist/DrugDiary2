package brettdansmith.drugdiary.domain.units;

import java.util.Locale;

/**
 * Formats dose mass values stored as milligrams. Medication free-text doses are
 * preserved, but structured calculators should route through this class.
 */
public final class DoseFormatter {
    private DoseFormatter() {
    }

    public static String formatMilligrams(double milligrams) {
        if (Double.isNaN(milligrams) || milligrams <= 0) {
            return "--";
        }
        if (milligrams >= 1_000) {
            return String.format(Locale.getDefault(), "%.2f g", milligrams / 1_000.0);
        }
        if (milligrams < 1) {
            return String.format(Locale.getDefault(), "%.0f mcg", milligrams * 1_000.0);
        }
        return String.format(Locale.getDefault(), "%.1f mg", milligrams);
    }
}

