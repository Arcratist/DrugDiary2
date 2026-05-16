package brettdansmith.drugdiary.ui.assistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import brettdansmith.drugdiary.R;

public final class NotificationRegistry {
    public static final String CHANNEL_ASSISTANT_REPLIES = "assistant_replies";

    private NotificationRegistry() {
    }

    public static void ensureAllChannels(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        ensureAssistantRepliesChannel(context, manager);
    }

    private static void ensureAssistantRepliesChannel(Context context, NotificationManager manager) {
        if (manager.getNotificationChannel(CHANNEL_ASSISTANT_REPLIES) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ASSISTANT_REPLIES,
                context.getString(R.string.assistant_response_notifications),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.assistant_reply_ready_body));
        manager.createNotificationChannel(channel);
    }
}

