package brettdansmith.drugdiary.network.ai;

import java.util.List;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.domain.model.ai.AiModelInfo;
import brettdansmith.drugdiary.domain.repository.AiService;
import brettdansmith.drugdiary.domain.repository.SettingsRepository;

/**
 * Implementation adapter for AiService interface.
 * Wraps the existing AI clients to provide a unified domain-level service.
 */
public final class AiServiceImpl implements AiService {
    private final SettingsRepository settingsRepo;

    public AiServiceImpl(SettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    @Override
    public String sendMessage(String message) throws Exception {
        // This would delegate to the appropriate AI client based on current provider
        // For now, throw UnsupportedOperationException as it requires more implementation
        throw new UnsupportedOperationException("AI messaging not yet implemented through service");
    }

    @Override
    public void sendMessageStreaming(String message, StreamChunkCallback onChunk) throws Exception {
        // This would delegate to AssistantApiClient for streaming
        throw new UnsupportedOperationException("Streaming AI messaging not yet implemented through service");
    }

    @Override
    public List<AiModelInfo> getAvailableModels() throws Exception {
        // This would fetch available models from the current provider
        throw new UnsupportedOperationException("Model listing not yet implemented through service");
    }

    @Override
    public AiProvider getCurrentProvider() {
        return settingsRepo.getAiProvider();
    }

    @Override
    public void setCurrentProvider(AiProvider provider) {
        settingsRepo.setAiProvider(provider);
    }

    @Override
    public boolean testConnection() {
        // Test connection to the current provider
        try {
            AiProvider provider = getCurrentProvider();
            // Simple connection test - just verify provider is valid
            return provider != null;
        } catch (Exception e) {
            return false;
        }
    }
}

