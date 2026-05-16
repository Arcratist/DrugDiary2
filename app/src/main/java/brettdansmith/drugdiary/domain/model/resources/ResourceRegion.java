package brettdansmith.drugdiary.domain.model.resources;

public enum ResourceRegion {
    WORLDWIDE("Worldwide"),
    AU("AU"),
    US("US"),
    UK("UK");

    private final String label;

    ResourceRegion(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
