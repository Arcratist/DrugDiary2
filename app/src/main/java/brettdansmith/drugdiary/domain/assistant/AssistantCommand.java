package brettdansmith.drugdiary.domain.assistant;

import android.content.Context;

/**
 * Interface for assistant commands.
 */
public interface AssistantCommand {
    String execute(Context context, String command, AssistantCommandRegistry.UiActions actions) throws Exception;
}
