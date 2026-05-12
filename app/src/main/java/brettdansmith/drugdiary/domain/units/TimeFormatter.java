package brettdansmith.drugdiary.domain.units;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.TimeFormat;

public final class TimeFormatter {
    private TimeFormatter() {
    }

    public static String formatDateTime(Context context, long epochMillis) {
        long now = System.currentTimeMillis();
        long diffMinutes = Math.abs(TimeUnit.MILLISECONDS.toMinutes(now - epochMillis));
        if (diffMinutes < 1) {
            return "just now";
        }
        if (diffMinutes < 60) {
            return diffMinutes + "m ago";
        }
        TimeFormat format = new SettingsRepository(context).getState().timeFormat;
        if (format == TimeFormat.TWENTY_FOUR_HOUR) {
            return new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(new Date(epochMillis));
        }
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(epochMillis));
    }
}

