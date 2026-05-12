package brettdansmith.drugdiary.data.settings;

public enum LanguageOption {
    SYSTEM("system"),
    ENGLISH("en"),
    SPANISH("es");

    private final String languageTag;

    LanguageOption(String languageTag) {
        this.languageTag = languageTag;
    }

    public String languageTag() {
        return languageTag;
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

