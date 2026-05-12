package brettdansmith.drugdiary.data.settings;

public enum TimeFormat {
    TWENTY_FOUR_HOUR("24h"),
    TWELVE_HOUR("12h");

    private final String preferenceValue;

    TimeFormat(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public static TimeFormat fromPreference(String value) {
        return TWELVE_HOUR.preferenceValue.equals(value) ? TWELVE_HOUR : TWENTY_FOUR_HOUR;
    }
}

