package brettdansmith.drugdiary.network.ai;

import android.content.Context;

import java.util.List;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.model.ai.AiResolvedConfig;
import brettdansmith.drugdiary.network.ai.capabilities.AiConfigResolver;
import brettdansmith.drugdiary.ui.assistant.ChatMessage;

public final class OpenAiProviderClient implements AiProviderClient {
    @Override
    public void stream(Context context, List<ChatMessage> messages, String profileContextText, AssistantApiClient.StreamCallback callback) throws Exception {
        SettingsRepository repository = new SettingsRepository(context);
        ProviderSettings providerSettings = repository.getProviderSettings(AiProvider.OPENAI);
        AiResolvedConfig resolved = AiConfigResolver.resolve(
                AiProvider.OPENAI,
                providerSettings,
                repository.isAiWebSearchEnabled(),
                repository.isAiCitationsRequired());
        AssistantApiClient.streamOpenAiCompatibleResponse(
                context,
                messages,
                profileContextText,
                callback,
                resolved.apiKey,
                resolved.model,
                resolved.endpointUrl,
                "OpenAI",
                AssistantApiClient.openAiCompatibleAttachmentMode("OpenAI", resolved.model));
    }
}

