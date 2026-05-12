package brettdansmith.drugdiary.ui.assistant;

import java.util.UUID;

public class ChatMessage {
    private final String id;
    private String content;
    private boolean isSent;
    private long createdAt;
    private String attachmentName;
    private String attachmentMimeType;
    private String attachmentBase64;

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
        this.attachmentName = attachmentName == null ? "" : attachmentName;
        this.attachmentMimeType = attachmentMimeType == null ? "" : attachmentMimeType;
        this.attachmentBase64 = attachmentBase64 == null ? "" : attachmentBase64;
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
        return attachmentName;
    }

    public String getAttachmentMimeType() {
        return attachmentMimeType;
    }

    public String getAttachmentBase64() {
        return attachmentBase64;
    }

    public boolean hasAttachment() {
        return !attachmentBase64.trim().isEmpty();
    }

    public boolean hasImageAttachment() {
        return hasAttachment() && attachmentMimeType.toLowerCase(java.util.Locale.US).startsWith("image/");
    }
}



