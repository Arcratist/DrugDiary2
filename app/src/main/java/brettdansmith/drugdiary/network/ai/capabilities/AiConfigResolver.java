package brettdansmith.drugdiary.network.ai.capabilities;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.model.ai.AiRequestOptions;
import brettdansmith.drugdiary.model.ai.AiResolvedConfig;
import brettdansmith.drugdiary.model.ai.ProviderCapabilities;
import brettdansmith.drugdiary.network.ai.AssistantApiClient;

public final class AiConfigResolver {
    private AiConfigResolver() {
    }

    public static AiResolvedConfig resolve(AiProvider provider, ProviderSettings settings, boolean globalWebSearch, boolean globalRequireCitations) {
        ProviderCapabilities capabilities = AiCapabilityRegistry.forProvider(provider);
        String endpoint = endpointFor(provider, settings);
        boolean webSearchRequested = globalWebSearch && settings.allowWebSearch && capabilities.webSearch;
        boolean citationsRequired = globalRequireCitations && settings.requireCitations && capabilities.citations;
        AiRequestOptions options = new AiRequestOptions(
                settings.allowStreaming && capabilities.streaming,
                webSearchRequested,
                citationsRequired,
                0.35,
                1.0,
                2400);
        return new AiResolvedConfig(provider, endpoint, settings.apiKey, settings.model, capabilities, options);
    }

    private static String endpointFor(AiProvider provider, ProviderSettings settings) {
        if (settings != null && settings.baseUrl != null && !settings.baseUrl.trim().isEmpty()) {
            return settings.baseUrl.trim();
        }
        switch (provider) {
            case OPENAI: return AssistantApiClient.OPENAI_CHAT_URL;
            case DEEPSEEK: return AssistantApiClient.DEEPSEEK_CHAT_URL;
            case OPENROUTER: return AssistantApiClient.OPENROUTER_CHAT_URL;
            case XAI: return AssistantApiClient.XAI_CHAT_URL;
            case GROQ: return AssistantApiClient.GROQ_CHAT_URL;
            case MISTRAL: return AssistantApiClient.MISTRAL_CHAT_URL;
            case PERPLEXITY: return AssistantApiClient.PERPLEXITY_CHAT_URL;
            default: return "";
        }
    }
}

