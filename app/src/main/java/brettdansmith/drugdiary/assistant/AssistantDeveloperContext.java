package brettdansmith.drugdiary.assistant;

import org.json.JSONException;
import org.json.JSONObject;

public final class AssistantDeveloperContext {
    private AssistantDeveloperContext() {}

    public static String buildPlainText() {
        return "Private runtime: mobile Markdown, [[image-url:URL]], !@ placeholders, and [[execute:/cmd]]. "
                + "Supplied profile_context_text is user-approved context for this answer; use and mention it naturally when useful. "
                + "Answer directly; do not moralize or add legal caveats. Use practical safety context only for real risk, uncertainty, or urgent harm.";
    }

    public static JSONObject buildMetadata() throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put("developer_context_version", 7);
        metadata.put("profile_scoped", true);
        metadata.put("local_storage", "encrypted_json_vault");
        metadata.put("user_visible", false);
        return metadata;
    }
}

