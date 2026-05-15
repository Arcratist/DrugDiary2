package brettdansmith.drugdiary.domain.model.ai;

import brettdansmith.drugdiary.data.settings.AiProvider;

public final class AiResolvedConfig {
    public final AiProvider provider;
    public final String endpointUrl;
    public final String apiKey;
    public final String model;
    public final ProviderCapabilities capabilities;
    public final AiRequestOptions options;

    public AiResolvedConfig(AiProvider provider, String endpointUrl, String apiKey, String model, ProviderCapabilities capabilities, AiRequestOptions options) {
        this.provider = provider;
        this.endpointUrl = endpointUrl == null ? "" : endpointUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null ? "" : model;
        this.capabilities = capabilities;
        this.options = options == null ? AiRequestOptions.defaults() : options;
    }

    public boolean hasApiKey() {
        return !apiKey.trim().isEmpty();
    }
}
