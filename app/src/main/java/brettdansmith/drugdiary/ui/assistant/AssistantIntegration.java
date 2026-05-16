package brettdansmith.drugdiary.ui.assistant;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import brettdansmith.drugdiary.R;

public final class AssistantIntegration {
    public static final String ARG_INITIAL_PROMPT = "initial_prompt";
    public static final String ARG_START_PRIVATE_CHAT = "start_private_chat";
    public static final String ARG_AUTO_SEND_PROMPT = "auto_send_prompt";
    
    /**
     * Bridge for other fragments to "Ask Assistant" about something.
     */
    public static void askAbout(Fragment caller, String query) {
        askAbout(caller, query, false, true);
    }

    public static void askAbout(Fragment caller, String query, boolean startPrivateChat) {
        askAbout(caller, query, startPrivateChat, true);
    }

    public static void askAboutPrefill(Fragment caller, String query, boolean startPrivateChat) {
        askAbout(caller, query, startPrivateChat, false);
    }

    public static void askAbout(Fragment caller, String query, boolean startPrivateChat, boolean autoSendPrompt) {
        if (caller == null || caller.getActivity() == null) return;

        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_PROMPT, query);
        args.putBoolean(ARG_START_PRIVATE_CHAT, startPrivateChat);
        args.putBoolean(ARG_AUTO_SEND_PROMPT, autoSendPrompt);
        NavHostFragment.findNavController(caller).navigate(R.id.assistantFragment, args);
    }
}
