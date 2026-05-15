package brettdansmith.drugdiary.domain.model.ai;

public final class AiRequestOptions {
    public final boolean streaming;
    public final boolean webSearchRequested;
    public final boolean citationsRequired;
    public final double temperature;
    public final double topP;
    public final int maxTokens;

    public AiRequestOptions(boolean streaming, boolean webSearchRequested, boolean citationsRequired, double temperature, double topP, int maxTokens) {
        this.streaming = streaming;
        this.webSearchRequested = webSearchRequested;
        this.citationsRequired = citationsRequired;
        this.temperature = temperature;
        this.topP = topP;
        this.maxTokens = maxTokens;
    }

    public static AiRequestOptions defaults() {
        return new AiRequestOptions(true, false, false, 0.35, 1.0, 2400);
    }
}
