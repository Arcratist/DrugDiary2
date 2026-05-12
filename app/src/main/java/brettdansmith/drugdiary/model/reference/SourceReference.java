package brettdansmith.drugdiary.model.reference;

public final class SourceReference {
    public final String source;
    public final String section;
    public final String url;
    public final long fetchedAt;

    public SourceReference(String source, String section, String url, long fetchedAt) {
        this.source = source == null ? "" : source;
        this.section = section == null ? "" : section;
        this.url = url == null ? "" : url;
        this.fetchedAt = Math.max(0, fetchedAt);
    }
}

