package brettdansmith.drugdiary.network.ai;

import android.content.Context;

import java.util.List;

import brettdansmith.drugdiary.ui.assistant.ChatMessage;

public final class AnthropicProviderClient implements AiProviderClient {
    @Override
    public void stream(Context context, List<ChatMessage> messages, String profileContextText, AssistantApiClient.StreamCallback callback) throws Exception {
        AssistantApiClient.streamAnthropicResponse(context, messages, profileContextText, callback);
    }
}

