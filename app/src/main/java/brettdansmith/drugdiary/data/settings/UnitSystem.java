package brettdansmith.drugdiary.data.settings;

public enum UnitSystem {
    SYSTEM("system"),
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
        if (SYSTEM.preferenceValue.equals(value)) return SYSTEM;
        if (IMPERIAL.preferenceValue.equals(value)) return IMPERIAL;
        return METRIC;
    }
    
    public static UnitSystem getSystemDefault() {
        String country = java.util.Locale.getDefault().getCountry().toUpperCase();
        // US, Liberia, Myanmar still officially use Imperial
        if ("US".equals(country) || "LR".equals(country) || "MM".equals(country)) {
            return IMPERIAL;
        }
        return METRIC;
    }
}
