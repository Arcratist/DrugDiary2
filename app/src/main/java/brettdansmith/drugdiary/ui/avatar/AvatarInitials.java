package brettdansmith.drugdiary.ui.avatar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class AvatarInitials {
    private AvatarInitials() {
    }

    @NonNull
    public static String fromName(@Nullable String name) {
        if (name == null) {
            return "?";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "?";
        }
        StringBuilder result = new StringBuilder();
        result.append(parts[0].charAt(0));
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) {
            result.append(parts[parts.length - 1].charAt(0));
        }
        return result.toString().toUpperCase(Locale.getDefault());
    }
}
