package brettdansmith.drugdiary.network.ai.capabilities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.domain.model.ai.AiResolvedConfig;

public class AiConfigResolverTest {
    @Test
    public void openAiWebSearchOnlyEnabledWhenBothGlobalAndProviderEnabled() {
        ProviderSettings disabledProviderWeb = new ProviderSettings(AiProvider.OPENAI, "k", "m", "", true, false, true, true, 45, 1);
        AiResolvedConfig resolvedA = AiConfigResolver.resolve(AiProvider.OPENAI, disabledProviderWeb, true, true);
        assertFalse(resolvedA.options.webSearchRequested);

        ProviderSettings enabledProviderWeb = new ProviderSettings(AiProvider.OPENAI, "k", "m", "", true, true, true, true, 45, 1);
        AiResolvedConfig resolvedB = AiConfigResolver.resolve(AiProvider.OPENAI, enabledProviderWeb, true, true);
        assertTrue(resolvedB.options.webSearchRequested);

        AiResolvedConfig resolvedC = AiConfigResolver.resolve(AiProvider.OPENAI, enabledProviderWeb, false, true);
        assertFalse(resolvedC.options.webSearchRequested);
    }

    @Test
    public void deepSeekDoesNotExposeNativeWebSearch() {
        ProviderSettings settings = new ProviderSettings(AiProvider.DEEPSEEK, "k", "deepseek-chat", "", true, true, true, true, 45, 1);
        AiResolvedConfig resolved = AiConfigResolver.resolve(AiProvider.DEEPSEEK, settings, true, true);
        assertFalse(resolved.capabilities.webSearch);
        assertFalse(resolved.options.webSearchRequested);
    }

    @Test
    public void perplexityCanUseWebAndCitations() {
        ProviderSettings settings = new ProviderSettings(AiProvider.PERPLEXITY, "k", "sonar", "", true, true, true, true, 45, 1);
        AiResolvedConfig resolved = AiConfigResolver.resolve(AiProvider.PERPLEXITY, settings, true, true);
        assertTrue(resolved.capabilities.webSearch);
        assertTrue(resolved.capabilities.citations);
        assertTrue(resolved.options.webSearchRequested);
        assertTrue(resolved.options.citationsRequired);
    }
}

