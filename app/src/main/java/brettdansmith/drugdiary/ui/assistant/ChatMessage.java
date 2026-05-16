package brettdansmith.drugdiary.ui.assistant;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatMessage {
    private final String id;
    private String content;
    private boolean isSent;
    private long createdAt;
    private final List<Attachment> attachments = new ArrayList<>();

    public ChatMessage(String content, boolean isSent) {
        this(UUID.randomUUID().toString(), content, isSent, System.currentTimeMillis());
    }

    public ChatMessage(String id, String content, boolean isSent, long createdAt) {
        this(id, content, isSent, createdAt, "", "", "");
    }

    public ChatMessage(String content, boolean isSent, String attachmentName, String attachmentMimeType, String attachmentBase64) {
        this(UUID.randomUUID().toString(), content, isSent, System.currentTimeMillis(), attachmentName, attachmentMimeType, attachmentBase64);
    }

    public ChatMessage(String id, String content, boolean isSent, long createdAt, String attachmentName, String attachmentMimeType, String attachmentBase64) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id;
        this.content = content;
        this.isSent = isSent;
        this.createdAt = createdAt <= 0 ? System.currentTimeMillis() : createdAt;
        setAttachment(attachmentName, attachmentMimeType, attachmentBase64);
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSent() {
        return isSent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getAttachmentName() {
        return attachments.isEmpty() ? "" : attachments.get(0).name;
    }

    public String getAttachmentMimeType() {
        return attachments.isEmpty() ? "" : attachments.get(0).mimeType;
    }

    public String getAttachmentBase64() {
        return attachments.isEmpty() ? "" : attachments.get(0).base64;
    }

    public boolean hasAttachment() {
        return !attachments.isEmpty();
    }

    public boolean hasImageAttachment() {
        return hasAttachment() && getAttachmentMimeType().toLowerCase(java.util.Locale.US).startsWith("image/");
    }

    public void setAttachment(String name, String mimeType, String base64) {
        attachments.clear();
        addAttachment(name, mimeType, base64);
    }

    public void addAttachment(String name, String mimeType, String base64) {
        String safeBase64 = base64 == null ? "" : base64.trim();
        if (safeBase64.isEmpty()) return;
        attachments.add(new Attachment(name, mimeType, safeBase64));
    }

    public void setAttachments(List<Attachment> next) {
        attachments.clear();
        if (next == null) return;
        for (Attachment attachment : next) {
            if (attachment != null) addAttachment(attachment.name, attachment.mimeType, attachment.base64);
        }
    }

    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public static class Attachment {
        public final String name;
        public final String mimeType;
        public final String base64;

        public Attachment(String name, String mimeType, String base64) {
            this.name = name == null ? "" : name;
            this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
            this.base64 = base64 == null ? "" : base64;
        }
    }

    public static class PendingAttachment {
        public final String name;
        public final String mimeType;
        public final String base64;

        public PendingAttachment(String name, String mimeType, String base64) {
            this.name = name;
            this.mimeType = mimeType;
            this.base64 = base64;
        }
    }
}


