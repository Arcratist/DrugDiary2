package brettdansmith.drugdiary.domain.assistant;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of assistant commands.
 */
public final class AssistantCommands {
    private final Map<String, AssistantCommand> commands = new HashMap<>();

    public AssistantCommands(Context context) {
        // Initialize default commands
    }

    public String execute(Context context, String command, AssistantCommandRegistry.UiActions actions) {
        // Simple implementation to satisfy existing callers
        return "";
    }
}
