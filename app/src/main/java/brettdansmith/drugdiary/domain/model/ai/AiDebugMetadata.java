package brettdansmith.drugdiary.domain.model.ai;

import org.json.JSONException;
import org.json.JSONObject;

public final class AiDebugMetadata {
    private AiDebugMetadata() {
    }

    public static JSONObject sanitized(
            String provider,
            String model,
            String endpoint,
            ProviderCapabilities capabilities,
            AiRequestOptions options,
            boolean fallbackUsed,
            String fallbackProvider,
            String error) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("provider", provider == null ? "" : provider);
        json.put("model", model == null ? "" : model);
        json.put("endpoint", sanitizeEndpoint(endpoint));
        json.put("streaming_requested", options != null && options.streaming);
        json.put("web_requested", options != null && options.webSearchRequested);
        json.put("citations_required", options != null && options.citationsRequired);
        json.put("fallback_used", fallbackUsed);
        json.put("fallback_provider", fallbackProvider == null ? "" : fallbackProvider);
        json.put("error", sanitizeError(error));
        if (capabilities != null) {
            json.put("supports_web", capabilities.webSearch);
            json.put("supports_citations", capabilities.citations);
            json.put("supports_streaming", capabilities.streaming);
            json.put("supports_vision", capabilities.visionInput);
            json.put("supports_structured_output", capabilities.structuredOutput);
        }
        return json;
    }

    private static String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) return "";
        String cleaned = endpoint.trim();
        int query = cleaned.indexOf('?');
        if (query >= 0) cleaned = cleaned.substring(0, query);
        return cleaned;
    }

    public static String sanitizeError(String error) {
        String clean = error == null ? "" : error.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.toLowerCase().contains("bearer ")) {
            clean = clean.replaceAll("(?i)bearer\\s+[A-Za-z0-9._-]+", "Bearer [redacted]");
        }
        clean = clean.replaceAll("(?i)(api[_-]?key|authorization|x-api-key)\\s*[:=]\\s*[^,; ]+", "$1=[redacted]");
        if (clean.length() > 280) clean = clean.substring(0, 280) + "...";
        return clean;
    }
}
