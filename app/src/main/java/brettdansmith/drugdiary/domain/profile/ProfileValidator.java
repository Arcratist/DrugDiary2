package brettdansmith.drugdiary.domain.profile;

import brettdansmith.drugdiary.domain.units.UnitConverter;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.domain.validation.ProfileValidationResult;

public final class ProfileValidator {
    private ProfileValidator() {
    }

    public static ProfileValidationResult validateName(String name) {
        return name == null || name.trim().isEmpty()
                ? ProfileValidationResult.error("A name or nickname is required")
                : ProfileValidationResult.ok();
    }

    public static ProfileValidationResult validatePin(String pin, boolean preferSixDigits) {
        if (pin == null || !(pin.matches("\\d{4}") || pin.matches("\\d{6}"))) {
            return ProfileValidationResult.error(preferSixDigits ? "Use a 6 digit PIN" : "Use a 4 or 6 digit PIN");
        }
        return ProfileValidationResult.ok();
    }

    public static ProfileValidationResult validateAge(String ageText) {
        if (ageText == null || ageText.trim().isEmpty()) {
            return ProfileValidationResult.ok();
        }
        double age = parseDouble(ageText);
        return age < 0 || age > 130 ? ProfileValidationResult.error("Use 0-130") : ProfileValidationResult.ok();
    }

    public static ProfileValidationResult validateDisplayWeight(double displayWeight, UnitPreferences preferences) {
        if (displayWeight <= 0) {
            return ProfileValidationResult.ok();
        }
        double weightKg = UnitConverter.normalizeDisplayWeight(displayWeight, preferences);
        if (weightKg < 2 || weightKg > 700) {
            return ProfileValidationResult.error(preferences.isMetric() ? "Use 2-700 kg" : "Use 4-1540 lb");
        }
        return ProfileValidationResult.ok();
    }

    public static ProfileValidationResult validateDisplayHeight(double displayHeight, UnitPreferences preferences) {
        if (displayHeight <= 0) {
            return ProfileValidationResult.ok();
        }
        double heightCm = UnitConverter.normalizeDisplayHeight(displayHeight, preferences);
        if (heightCm < 30 || heightCm > 260) {
            return ProfileValidationResult.error(preferences.isMetric() ? "Use 30-260 cm" : "Use 12-102 in");
        }
        return ProfileValidationResult.ok();
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }
}

