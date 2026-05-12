package brettdansmith.drugdiary.network.ai;

import android.content.Context;

import java.util.List;

import brettdansmith.drugdiary.ui.assistant.ChatMessage;

public final class OpenAiCompatibleProviderClient implements AiProviderClient {
    private final String apiKey;
    private final String model;
    private final String endpointUrl;
    private final String providerName;
    private final AssistantApiClient.AttachmentMode attachmentMode;

    public OpenAiCompatibleProviderClient(String apiKey, String model, String endpointUrl, String providerName, AssistantApiClient.AttachmentMode attachmentMode) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpointUrl = endpointUrl;
        this.providerName = providerName;
        this.attachmentMode = attachmentMode == null ? AssistantApiClient.AttachmentMode.PORTABLE_TEXT_ONLY : attachmentMode;
    }

    @Override
    public void stream(Context context, List<ChatMessage> messages, String profileContextText, AssistantApiClient.StreamCallback callback) throws Exception {
        AssistantApiClient.streamOpenAiCompatibleResponse(context, messages, profileContextText, callback, apiKey, model, endpointUrl, providerName, attachmentMode);
    }
}

