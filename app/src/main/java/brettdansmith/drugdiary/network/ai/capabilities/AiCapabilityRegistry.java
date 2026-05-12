package brettdansmith.drugdiary.network.ai.capabilities;

import java.util.EnumMap;
import java.util.Map;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.model.ai.ProviderCapabilities;

public final class AiCapabilityRegistry {
    private static final Map<AiProvider, ProviderCapabilities> CAPS = new EnumMap<>(AiProvider.class);

    static {
        CAPS.put(AiProvider.OPENAI, new ProviderCapabilities(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.ANTHROPIC, new ProviderCapabilities(true, true, true, true, true, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.GEMINI, new ProviderCapabilities(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.DEEPSEEK, new ProviderCapabilities(true, true, true, true, true, false, false, false, true, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.XAI, new ProviderCapabilities(true, true, true, true, true, false, false, false, true, true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.GROQ, new ProviderCapabilities(true, true, true, true, true, false, false, false, true, true, true, true, true, false, false, false, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.MISTRAL, new ProviderCapabilities(true, true, true, true, true, false, false, true, true, true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.PERPLEXITY, new ProviderCapabilities(true, true, true, false, true, false, false, false, true, true, true, true, true, true, true, true, false, false, true, true, true, true, true, true, true, true));
        CAPS.put(AiProvider.OPENROUTER, new ProviderCapabilities(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true));
    }

    private AiCapabilityRegistry() {
    }

    public static ProviderCapabilities forProvider(AiProvider provider) {
        ProviderCapabilities caps = CAPS.get(provider);
        if (caps != null) return caps;
        return new ProviderCapabilities(true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, false, false, false);
    }
}

