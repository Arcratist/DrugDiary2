package brettdansmith.drugdiary.data.profile;

import androidx.annotation.NonNull;

import java.util.Locale;

public enum AvatarType {
    INITIALS,
    BUILT_IN_ICON,
    CUSTOM_IMAGE;

    @NonNull
    public static AvatarType fromStorage(String rawValue) {
        if (rawValue == null) {
            return INITIALS;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.US);
        if (normalized.isEmpty()) {
            return INITIALS;
        }
        try {
            return AvatarType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return INITIALS;
        }
    }
}
