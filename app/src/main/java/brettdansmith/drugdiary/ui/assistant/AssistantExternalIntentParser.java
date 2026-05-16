package brettdansmith.drugdiary.ui.assistant;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AssistantExternalIntentParser {
    public static final class ParsedRequest {
        public final String prompt;

        public ParsedRequest(String prompt) {
            this.prompt = prompt == null ? "" : prompt.trim();
        }
    }

    private AssistantExternalIntentParser() {
    }

    @Nullable
    public static ParsedRequest parse(@Nullable Intent intent) {
        if (intent == null) return null;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) return parseSend(intent);
        if (Intent.ACTION_PROCESS_TEXT.equals(action)) return parseProcessText(intent);
        return null;
    }

    @Nullable
    private static ParsedRequest parseSend(@NonNull Intent intent) {
        StringBuilder builder = new StringBuilder();
        builder.append("Please help with this shared content.\n");

        String subject = safe(intent.getStringExtra(Intent.EXTRA_SUBJECT));
        String text = safeCharSequence(intent.getCharSequenceExtra(Intent.EXTRA_TEXT));
        String title = safeCharSequence(intent.getCharSequenceExtra(Intent.EXTRA_TITLE));
        String mimeType = safe(intent.getType());

        if (!title.isEmpty()) builder.append("Title: ").append(title).append("\n");
        if (!subject.isEmpty()) builder.append("Subject: ").append(subject).append("\n");
        if (!mimeType.isEmpty()) builder.append("MimeType: ").append(mimeType).append("\n");
        if (!text.isEmpty()) builder.append("Body:\n").append(text).append("\n");

        List<String> uris = extractUris(intent);
        if (!uris.isEmpty()) {
            builder.append("Shared attachment URIs:\n");
            for (String uri : uris) builder.append("- ").append(uri).append("\n");
        }

        String packageName = safe(intent.getStringExtra(Intent.EXTRA_REFERRER_NAME));
        if (!packageName.isEmpty()) builder.append("Source: ").append(packageName).append("\n");

        String prompt = builder.toString().trim();
        if (prompt.equals("Please help with this shared content.")) return null;
        return new ParsedRequest(prompt);
    }

    @Nullable
    private static ParsedRequest parseProcessText(@NonNull Intent intent) {
        String selectedText = safeCharSequence(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT));
        if (selectedText.isEmpty()) return null;
        String prompt = "Please help with this selected text:\n" + selectedText;
        return new ParsedRequest(prompt);
    }

    @NonNull
    private static List<String> extractUris(@NonNull Intent intent) {
        ArrayList<String> items = new ArrayList<>();
        Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (stream != null) items.add(stream.toString());
        ArrayList<Uri> streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (streams != null) {
            for (Uri uri : streams) {
                if (uri != null) items.add(uri.toString());
            }
        }
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                if (item == null || item.getUri() == null) continue;
                items.add(item.getUri().toString());
            }
        }
        return items;
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return TextUtils.isEmpty(value) ? "" : value.trim();
    }

    @NonNull
    private static String safeCharSequence(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
