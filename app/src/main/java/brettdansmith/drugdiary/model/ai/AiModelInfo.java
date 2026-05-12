package brettdansmith.drugdiary.model.ai;

public final class AiModelInfo {
    public final String provider;
    public final String modelId;
    public final String displayName;
    public final ProviderCapabilities capabilities;

    public AiModelInfo(String provider, String modelId, String displayName, ProviderCapabilities capabilities) {
        this.provider = provider == null ? "" : provider;
        this.modelId = modelId == null ? "" : modelId;
        this.displayName = displayName == null || displayName.trim().isEmpty() ? this.modelId : displayName.trim();
        this.capabilities = capabilities;
    }
}

