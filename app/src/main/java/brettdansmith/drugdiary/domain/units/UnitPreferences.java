package brettdansmith.drugdiary.domain.units;

import android.content.Context;

import brettdansmith.drugdiary.data.settings.SettingsResolver;
import brettdansmith.drugdiary.data.settings.UnitSystem;

public final class UnitPreferences {
    private final UnitSystem unitSystem;

    public UnitPreferences(UnitSystem unitSystem) {
        this.unitSystem = unitSystem == null ? UnitSystem.METRIC : unitSystem;
    }

    public static UnitPreferences from(Context context) {
        return new UnitPreferences(SettingsResolver.getEffectiveSettings(context).unitSystem);
    }

    public UnitSystem unitSystem() {
        return unitSystem;
    }

    public boolean isMetric() {
        return unitSystem == UnitSystem.METRIC;
    }

    public String weightLabel() {
        return isMetric() ? "kg" : "lb";
    }

    public String heightLabel() {
        return isMetric() ? "cm" : "in";
    }

    public String volumeLabel() {
        return isMetric() ? "ml" : "fl oz";
    }
}
