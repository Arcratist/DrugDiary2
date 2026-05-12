package brettdansmith.drugdiary.data.settings;

public enum UnitSystem {
    METRIC("metric"),
    IMPERIAL("imperial");

    private final String preferenceValue;

    UnitSystem(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public static UnitSystem fromPreference(String value) {
        return IMPERIAL.preferenceValue.equals(value) ? IMPERIAL : METRIC;
    }
}

