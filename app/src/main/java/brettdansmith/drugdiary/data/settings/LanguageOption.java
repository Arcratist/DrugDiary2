package brettdansmith.drugdiary.data.settings;

public enum LanguageOption {
    SYSTEM("system", "System"),
    ENGLISH("en", "English"),
    SPANISH("es", "Spanish");

    private final String languageTag;
    private final String displayName;

    LanguageOption(String languageTag, String displayName) {
        this.languageTag = languageTag;
        this.displayName = displayName;
    }

    public String languageTag() {
        return languageTag;
    }

    public String displayName() {
        return displayName;
    }

    public static LanguageOption fromTag(String tag) {
        for (LanguageOption option : values()) {
            if (option.languageTag.equals(tag)) {
                return option;
            }
        }
        return SYSTEM;
    }
}
