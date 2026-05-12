package brettdansmith.drugdiary.domain.validation;

public final class ProfileValidationResult {
    public final boolean valid;
    public final String message;

    private ProfileValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ProfileValidationResult ok() {
        return new ProfileValidationResult(true, "");
    }

    public static ProfileValidationResult error(String message) {
        return new ProfileValidationResult(false, message);
    }
}

