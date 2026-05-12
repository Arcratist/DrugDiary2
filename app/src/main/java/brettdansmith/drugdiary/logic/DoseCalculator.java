package brettdansmith.drugdiary.logic;

public final class DoseCalculator {
    private DoseCalculator() {
    }

    public static double milligramsPerMilliliter(double substanceMg, double solventMl) {
        if (substanceMg <= 0 || solventMl <= 0) {
            return 0;
        }
        return substanceMg / solventMl;
    }

    public static double millilitersForDose(double targetMg, double concentrationMgPerMl) {
        if (targetMg <= 0 || concentrationMgPerMl <= 0) {
            return 0;
        }
        return targetMg / concentrationMgPerMl;
    }

    public static double weightAdjustedDoseMg(double mgPerKg, double weightKg) {
        if (mgPerKg <= 0 || weightKg <= 0) {
            return 0;
        }
        return mgPerKg * weightKg;
    }
}

