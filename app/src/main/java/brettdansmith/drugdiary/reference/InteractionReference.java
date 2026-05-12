package brettdansmith.drugdiary.reference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class InteractionReference {
    public enum Risk {
        UNKNOWN,
        CAUTION,
        UNSAFE
    }

    public static final class Result {
        public final Risk risk;
        public final String summary;

        private Result(Risk risk, String summary) {
            this.risk = risk;
            this.summary = summary;
        }
    }

    private InteractionReference() {
    }

    public static Result check(String first, String second) {
        Set<String> pair = new HashSet<>(Arrays.asList(normalize(first), normalize(second)));
        if (pair.contains("alcohol") && pair.contains("benzodiazepine")) {
            return new Result(Risk.UNSAFE, "High respiratory depression and blackout risk. Avoid combining.");
        }
        if (pair.contains("opioid") && pair.contains("benzodiazepine")) {
            return new Result(Risk.UNSAFE, "High overdose risk from additive sedation and respiratory depression.");
        }
        if (pair.contains("ssri") && pair.contains("mdma")) {
            return new Result(Risk.CAUTION, "Possible serotonin toxicity risk and altered subjective effects.");
        }
        if (pair.contains("maoi") && (pair.contains("mdma") || pair.contains("amphetamine"))) {
            return new Result(Risk.UNSAFE, "Potentially dangerous hypertensive or serotonin toxicity reaction.");
        }
        return new Result(Risk.UNKNOWN, "No local reference match. Treat as unknown and verify with a clinician or poison information service.");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}

