package brettdansmith.drugdiary.domain.repository;

import java.util.List;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.domain.model.ai.AiModelInfo;

/**
 * Service interface for AI operations.
 * Defines the contract for all AI-related operations.
 */
public interface AiService {
    /**
     * Sends a message to the AI and gets a response.
     *
     * @param message the message to send
     * @return the AI response
     * @throws Exception if API call fails
     */
    String sendMessage(String message) throws Exception;

    /**
     * Sends a streaming message request (for streaming responses).
     *
     * @param message the message to send
     * @param onChunk callback for each response chunk
     * @throws Exception if API call fails
     */
    void sendMessageStreaming(String message, StreamChunkCallback onChunk) throws Exception;

    /**
     * Gets available models for the current provider.
     *
     * @return list of available models
     * @throws Exception if API call fails
     */
    List<AiModelInfo> getAvailableModels() throws Exception;

    /**
     * Gets the current AI provider.
     *
     * @return the current provider
     */
    AiProvider getCurrentProvider();

    /**
     * Sets the active AI provider.
     *
     * @param provider the provider to activate
     */
    void setCurrentProvider(AiProvider provider);

    /**
     * Tests connectivity with the current provider.
     *
     * @return true if connection successful
     */
    boolean testConnection();

    /**
     * Callback interface for streaming responses.
     */
    interface StreamChunkCallback {
        /**
         * Called when a chunk of data is received.
         *
         * @param chunk the response chunk
         */
        void onChunk(String chunk);

        /**
         * Called when an error occurs.
         *
         * @param error the error message
         */
        void onError(String error);

        /**
         * Called when streaming is complete.
         */
        void onComplete();
    }
}

