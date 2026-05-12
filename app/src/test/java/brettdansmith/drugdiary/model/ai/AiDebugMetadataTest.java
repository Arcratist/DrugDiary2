package brettdansmith.drugdiary.model.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AiDebugMetadataTest {
    @Test
    public void redactsBearerAndApiKeyLikeErrors() throws Exception {
        String raw = "authorization: Bearer sk-secret-token; api_key=abc123";
        String sanitized = AiDebugMetadata.sanitizeError(raw);
        assertFalse(sanitized.contains("sk-secret-token"));
        assertFalse(sanitized.contains("abc123"));
        assertFalse(sanitized.toLowerCase().contains("bearer sk"));
    }

    @Test
    public void keepsReadableNonSensitiveErrors() {
        String sanitized = AiDebugMetadata.sanitizeError("HTTP 429 Too Many Requests");
        assertTrue(sanitized.contains("429"));
    }
}

