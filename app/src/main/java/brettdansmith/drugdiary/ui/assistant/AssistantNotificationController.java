package brettdansmith.drugdiary.ui.assistant;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import brettdansmith.drugdiary.app.MainActivity;
import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsState;

public final class AssistantNotificationController {
    private static volatile boolean appInFocus;

    private AssistantNotificationController() {
    }

    public static void setAppInFocus(boolean inFocus) {
        appInFocus = inFocus;
    }

    public static boolean isAppInFocus() {
        return appInFocus;
    }

    public static void notifyReplyFinished(Context context, String chatId) {
        if (context == null || appInFocus) return;
        Context appContext = context.getApplicationContext();
        SettingsState settings = new SettingsRepository(appContext).getState();
        if (!settings.notificationsEnabled || !settings.assistantResponseNotifications) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationRegistry.ensureAllChannels(appContext);

        Intent intent = new Intent(appContext, MainActivity.class)
                .setAction(MainActivity.ACTION_OPEN_ASSISTANT_CHAT)
                .putExtra(MainActivity.EXTRA_ASSISTANT_CHAT_ID, chatId == null ? "" : chatId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                appContext,
                (chatId == null ? "" : chatId).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = settings.stealthNotifications
                ? appContext.getString(R.string.assistant_reply_ready_stealth_title)
                : appContext.getString(R.string.assistant_reply_ready_title);
        String body = settings.stealthNotifications
                ? appContext.getString(R.string.assistant_reply_ready_stealth_body)
                : appContext.getString(R.string.assistant_reply_ready_body);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, NotificationRegistry.CHANNEL_ASSISTANT_REPLIES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(appContext).notify(notificationId(chatId), builder.build());
    }

    private static int notificationId(String chatId) {
        String id = chatId == null || chatId.trim().isEmpty() ? "assistant" : chatId.trim();
        return 0xA5510000 | (id.hashCode() & 0x0000FFFF);
    }
}
