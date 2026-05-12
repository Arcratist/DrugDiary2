package brettdansmith.drugdiary.domain.assistant;

import android.content.Context;

public interface AssistantCommand {
    String name();
    boolean runsAsync();
    String execute(Context context, String command, AssistantCommandRegistry.UiActions actions) throws Exception;
}

