package brettdansmith.drugdiary.data.settings;

public final class ProviderSettings {
    public final AiProvider provider;
    public final String apiKey;
    public final String model;
    public final String baseUrl;
    public final boolean enabled;
    public final boolean allowWebSearch;
    public final boolean requireCitations;
    public final boolean allowStreaming;
    public final int timeoutSeconds;
    public final int maxRetries;

    public ProviderSettings(AiProvider provider, String apiKey, String model) {
        this(provider, apiKey, model, "", true, false, false, true, 45, 1);
    }

    public ProviderSettings(
            AiProvider provider,
            String apiKey,
            String model,
            String baseUrl,
            boolean enabled,
            boolean allowWebSearch,
            boolean requireCitations,
            boolean allowStreaming,
            int timeoutSeconds,
            int maxRetries) {
        this.provider = provider;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null || model.trim().isEmpty() ? provider.defaultModel() : model.trim();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.enabled = enabled;
        this.allowWebSearch = allowWebSearch;
        this.requireCitations = requireCitations;
        this.allowStreaming = allowStreaming;
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
        this.maxRetries = Math.max(0, maxRetries);
    }
}

