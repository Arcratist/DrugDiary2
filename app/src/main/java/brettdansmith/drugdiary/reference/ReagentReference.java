package brettdansmith.drugdiary.reference;

public final class ReagentReference {
    public static final String[] REAGENTS = {
            "Marquis",
            "Mecke",
            "Mandelin",
            "Froehde",
            "Liebermann",
            "Simon"
    };

    private ReagentReference() {
    }

    public static String quickGuide() {
        return "Use multiple reagents where possible, compare timing and colour, and treat unexpected or no reaction results as inconclusive.";
    }
}

