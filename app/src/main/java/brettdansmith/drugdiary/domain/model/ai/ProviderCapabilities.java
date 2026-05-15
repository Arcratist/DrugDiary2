package brettdansmith.drugdiary.domain.model.ai;

public final class ProviderCapabilities {
    public final boolean chat;
    public final boolean streaming;
    public final boolean modelListing;
    public final boolean visionInput;
    public final boolean fileInput;
    public final boolean audioInput;
    public final boolean audioOutput;
    public final boolean imageGeneration;
    public final boolean structuredOutput;
    public final boolean jsonMode;
    public final boolean functionCalling;
    public final boolean serverSideTools;
    public final boolean webSearch;
    public final boolean webGrounding;
    public final boolean webFetch;
    public final boolean citations;
    public final boolean reasoningControls;
    public final boolean reasoningEffort;
    public final boolean fallbackRouting;
    public final boolean supportsTemperature;
    public final boolean supportsTopP;
    public final boolean supportsMaxTokens;
    public final boolean supportsSystemPrompt;
    public final boolean supportsAttachments;
    public final boolean supportsRequestCancellation;
    public final boolean supportsUsageMetadata;

    public ProviderCapabilities(
            boolean chat,
            boolean streaming,
            boolean modelListing,
            boolean visionInput,
            boolean fileInput,
            boolean audioInput,
            boolean audioOutput,
            boolean imageGeneration,
            boolean structuredOutput,
            boolean jsonMode,
            boolean functionCalling,
            boolean serverSideTools,
            boolean webSearch,
            boolean webGrounding,
            boolean webFetch,
            boolean citations,
            boolean reasoningControls,
            boolean reasoningEffort,
            boolean fallbackRouting,
            boolean supportsTemperature,
            boolean supportsTopP,
            boolean supportsMaxTokens,
            boolean supportsSystemPrompt,
            boolean supportsAttachments,
            boolean supportsRequestCancellation,
            boolean supportsUsageMetadata) {
        this.chat = chat;
        this.streaming = streaming;
        this.modelListing = modelListing;
        this.visionInput = visionInput;
        this.fileInput = fileInput;
        this.audioInput = audioInput;
        this.audioOutput = audioOutput;
        this.imageGeneration = imageGeneration;
        this.structuredOutput = structuredOutput;
        this.jsonMode = jsonMode;
        this.functionCalling = functionCalling;
        this.serverSideTools = serverSideTools;
        this.webSearch = webSearch;
        this.webGrounding = webGrounding;
        this.webFetch = webFetch;
        this.citations = citations;
        this.reasoningControls = reasoningControls;
        this.reasoningEffort = reasoningEffort;
        this.fallbackRouting = fallbackRouting;
        this.supportsTemperature = supportsTemperature;
        this.supportsTopP = supportsTopP;
        this.supportsMaxTokens = supportsMaxTokens;
        this.supportsSystemPrompt = supportsSystemPrompt;
        this.supportsAttachments = supportsAttachments;
        this.supportsRequestCancellation = supportsRequestCancellation;
        this.supportsUsageMetadata = supportsUsageMetadata;
    }
}
