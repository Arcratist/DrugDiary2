package brettdansmith.drugdiary.ui.assistant;

import brettdansmith.drugdiary.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.textfield.TextInputEditText;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brettdansmith.drugdiary.domain.assistant.AssistantCommandParser;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public static final String COMMAND_PREFIX = "[[command-response]]";
    private static final String SYSTEM_PREFIX = "[[system-message]]";
    private static final String IMAGE_PREFIX = "[[image-url:";
    private static final String IMAGE_SUFFIX = "]]";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\((https?://[^\\s)]+)\\)");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("!@([a-zA-Z0-9_]+):([^\\s|,;]+)");
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(3);

    private List<ChatMessage> messages = new ArrayList<>();
    private OnCommandClickListener commandListener;
    private OnMessageActionListener messageActionListener;
    private boolean isStreaming = false;
    private String editingMessageId = "";
    private final Map<String, List<ChatMessage.Attachment>> editingAttachments = new HashMap<>();

    public interface OnCommandClickListener {
        void onCommandClick(String command);
    }

    public interface OnMessageActionListener {
        void onEdit(ChatMessage message);
        void onDelete(ChatMessage message);
        void onImageClick(String base64);
        void onInlineEditBranch(ChatMessage message, String updatedText, List<ChatMessage.Attachment> attachments);
        void onInlineEditPrivateBranch(ChatMessage message, String updatedText, List<ChatMessage.Attachment> attachments);
        void onInlineEditUpdate(ChatMessage message, String updatedText, List<ChatMessage.Attachment> attachments);
    }

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
        setHasStableIds(true);
    }

    public void submitMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> nextMessages = newMessages == null ? new ArrayList<>() : new ArrayList<>(newMessages);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ChatDiffCallback(this.messages, nextMessages), false);
        this.messages = nextMessages;
        diff.dispatchUpdatesTo(this);
    }

    public void setStreaming(boolean streaming) {
        this.isStreaming = streaming;
        if (!messages.isEmpty()) {
            notifyItemChanged(messages.size() - 1);
        }
    }

    public void setOnCommandClickListener(OnCommandClickListener listener) {
        this.commandListener = listener;
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.messageActionListener = listener;
    }

    public void setEditingMessageId(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            editingAttachments.clear();
        } else if (!messageId.equals(this.editingMessageId)) {
            editingAttachments.remove(this.editingMessageId);
        }
        this.editingMessageId = messageId == null ? "" : messageId;
        notifyDataSetChanged();
    }

    public boolean cancelEditingIfActive() {
        if (editingMessageId == null || editingMessageId.isEmpty()) return false;
        editingMessageId = "";
        editingAttachments.clear();
        notifyDataSetChanged();
        return true;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        Context context = holder.itemView.getContext();
        String content = message.getContent() == null ? "" : message.getContent();

        holder.layoutSent.setVisibility(View.GONE);
        holder.layoutReceived.setVisibility(View.GONE);
        holder.layoutCommand.setVisibility(View.GONE);
        holder.layoutSystem.setVisibility(View.GONE);
        holder.layoutMessageActions.setVisibility(View.GONE);

        bindAttachmentPreview(holder, message);

        if (content.startsWith(COMMAND_PREFIX)) {
            holder.layoutCommand.setVisibility(View.VISIBLE);
            String cmdText = content.substring(COMMAND_PREFIX.length()).trim();
            holder.textCommand.setText(cmdText);
        } else if (content.startsWith(SYSTEM_PREFIX)) {
            holder.layoutSystem.setVisibility(View.VISIBLE);
            String sysText = content.substring(SYSTEM_PREFIX.length()).trim();
            holder.textSystem.setText(sysText);
        } else if (message.isSent()) {
            holder.layoutSent.setVisibility(View.VISIBLE);
            bindInlineEditor(holder, message, content, context);
            bindMessageActions(holder, message, isLastUserMessage(position));
        } else {
            holder.layoutReceived.setVisibility(View.VISIBLE);
            String visibleText = stripImageMarker(content).trim();
            if (isStreaming && position == messages.size() - 1) {
                visibleText += " █";
            }
            holder.textReceived.setText(formatRichText(visibleText, context, false, false, true));
            setupTextViewForLinks(holder.textReceived);
            bindEmbeddedImages(holder, content);
        }
    }

    private void setupTextViewForLinks(TextView textView) {
        if (!(textView.getMovementMethod() instanceof LinkMovementMethod)) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        textView.setLinksClickable(true);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= messages.size()) return RecyclerView.NO_ID;
        ChatMessage message = messages.get(position);
        String id = message == null ? "" : message.getId();
        return id == null || id.isEmpty() ? position : (id.hashCode() & 0xffffffffL);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View layoutSent, layoutReceived, layoutCommand, layoutSystem;
        View layoutMessageActions;
        TextView textSent, textReceived, textCommand, textSystem;
        TextView textReceivedAttachment;
        View layoutInlineEdit;
        LinearLayout layoutInlineEditAttachments;
        TextInputEditText editInlineMessage;
        TextView textEditHelper;
        ImageView imageSentAttachment;
        LinearLayout layoutReceivedImages, layoutSentAttachments;
        MaterialCardView cardSent, cardCommand, cardSystem;
        MaterialButton buttonEdit, buttonDelete;
        MaterialButton buttonInlineCancel, buttonInlineUpdate, buttonInlineBranch;

        ChatViewHolder(View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layout_sent);
            layoutReceived = itemView.findViewById(R.id.layout_received);
            layoutCommand = itemView.findViewById(R.id.layout_command);
            layoutSystem = itemView.findViewById(R.id.layout_system);
            layoutMessageActions = itemView.findViewById(R.id.layout_message_actions);
            
            textSent = itemView.findViewById(R.id.text_sent);
            layoutInlineEdit = itemView.findViewById(R.id.layout_inline_edit);
            layoutInlineEditAttachments = itemView.findViewById(R.id.layout_inline_edit_attachments);
            editInlineMessage = itemView.findViewById(R.id.edit_inline_message);
            textEditHelper = itemView.findViewById(R.id.text_edit_helper);
            textReceived = itemView.findViewById(R.id.text_received);
            textCommand = itemView.findViewById(R.id.text_command);
            textSystem = itemView.findViewById(R.id.text_system);
            textReceivedAttachment = itemView.findViewById(R.id.text_received_attachment);
            
            imageSentAttachment = itemView.findViewById(R.id.image_sent_attachment);
            layoutReceivedImages = itemView.findViewById(R.id.layout_received_images);
            layoutSentAttachments = itemView.findViewById(R.id.layout_sent_attachments);
            
            cardSent = itemView.findViewById(R.id.card_sent);
            cardCommand = itemView.findViewById(R.id.card_command);
            cardSystem = itemView.findViewById(R.id.card_system);

            buttonEdit = itemView.findViewById(R.id.button_edit_message);
            buttonDelete = itemView.findViewById(R.id.button_delete_message);
            buttonInlineCancel = itemView.findViewById(R.id.button_inline_cancel);
            buttonInlineUpdate = itemView.findViewById(R.id.button_inline_update);
            buttonInlineBranch = itemView.findViewById(R.id.button_inline_branch);
        }
    }

    private void bindMessageActions(ChatViewHolder holder, ChatMessage message, boolean allowEdit) {
        boolean canModify = message != null && message.isSent() && allowEdit;
        if (isEditing(message)) canModify = false;
        holder.layoutMessageActions.setVisibility(canModify ? View.VISIBLE : View.GONE);
        if (!canModify) return;
        holder.buttonEdit.setOnClickListener(v -> {
            setEditingMessageId(message.getId());
        });
        holder.buttonDelete.setOnClickListener(v -> {
            if (messageActionListener != null) messageActionListener.onDelete(message);
        });
    }

    private boolean isEditing(ChatMessage message) {
        return message != null && message.getId() != null && message.getId().equals(editingMessageId);
    }

    private void bindInlineEditor(ChatViewHolder holder, ChatMessage message, String content, Context context) {
        boolean editing = isEditing(message);
        holder.layoutInlineEdit.setVisibility(editing ? View.VISIBLE : View.GONE);
        holder.textSent.setVisibility(editing ? View.GONE : View.VISIBLE);
        holder.layoutInlineEditAttachments.setVisibility(View.GONE);
        holder.layoutInlineEditAttachments.removeAllViews();

        if (!editing) {
            holder.textSent.setText(formatRichText(content, context, true, false, true));
            setupTextViewForLinks(holder.textSent);
            return;
        }
        holder.editInlineMessage.setText(content);
        holder.editInlineMessage.setSelection(holder.editInlineMessage.getText() == null ? 0 : holder.editInlineMessage.getText().length());
        holder.editInlineMessage.post(() -> {
            holder.editInlineMessage.requestFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(holder.editInlineMessage, InputMethodManager.SHOW_IMPLICIT);
        });
        holder.textEditHelper.setText("Replies after this message will be regenerated.");
        bindInlineEditAttachmentChips(holder, message);

        holder.buttonInlineCancel.setOnClickListener(v -> setEditingMessageId(""));
        holder.buttonInlineUpdate.setOnClickListener(v -> {
            if (messageActionListener == null) return;
            String updated = holder.editInlineMessage.getText() == null ? "" : holder.editInlineMessage.getText().toString().trim();
            if (updated.isEmpty()) return;
            List<ChatMessage.Attachment> editedAttachments = getEditingAttachments(message);
            setEditingMessageId("");
            messageActionListener.onInlineEditUpdate(message, updated, editedAttachments);
        });
        holder.buttonInlineBranch.setOnClickListener(v -> {
            if (messageActionListener == null) return;
            String updated = holder.editInlineMessage.getText() == null ? "" : holder.editInlineMessage.getText().toString().trim();
            if (updated.isEmpty()) return;
            List<ChatMessage.Attachment> editedAttachments = getEditingAttachments(message);
            setEditingMessageId("");
            messageActionListener.onInlineEditBranch(message, updated, editedAttachments);
        });
        holder.buttonInlineBranch.setOnLongClickListener(v -> {
            if (messageActionListener == null) return true;
            String updated = holder.editInlineMessage.getText() == null ? "" : holder.editInlineMessage.getText().toString().trim();
            if (updated.isEmpty()) return true;
            List<ChatMessage.Attachment> editedAttachments = getEditingAttachments(message);
            setEditingMessageId("");
            messageActionListener.onInlineEditPrivateBranch(message, updated, editedAttachments);
            return true;
        });
    }

    private void bindInlineEditAttachmentChips(ChatViewHolder holder, ChatMessage message) {
        if (message == null || !message.hasAttachment()) return;
        holder.layoutInlineEditAttachments.setVisibility(View.VISIBLE);
        List<ChatMessage.Attachment> attachments = getEditingAttachments(message);
        for (int i = 0; i < attachments.size(); i++) {
            final int index = i;
            ChatMessage.Attachment attachment = attachments.get(i);
            if (attachment == null) continue;
            String name = attachment.name == null || attachment.name.trim().isEmpty() ? "Attachment" : attachment.name.trim();
            String mime = attachment.mimeType == null || attachment.mimeType.trim().isEmpty() ? "file" : attachment.mimeType.trim();
            holder.layoutInlineEditAttachments.addView(createEditableAttachmentChip(
                    holder.itemView.getContext(),
                    name,
                    mime,
                    () -> {
                        List<ChatMessage.Attachment> mutable = editingAttachments.get(message.getId());
                        if (mutable == null || index < 0 || index >= mutable.size()) return;
                        mutable.remove(index);
                        notifyDataSetChanged();
                    }
            ));
        }
    }

    private List<ChatMessage.Attachment> getEditingAttachments(ChatMessage message) {
        if (message == null || message.getId() == null) return new ArrayList<>();
        List<ChatMessage.Attachment> cached = editingAttachments.get(message.getId());
        if (cached != null) return new ArrayList<>(cached);
        List<ChatMessage.Attachment> seed = new ArrayList<>(message.getAttachments());
        editingAttachments.put(message.getId(), new ArrayList<>(seed));
        return seed;
    }

    private void bindAttachmentPreview(ChatViewHolder holder, ChatMessage message) {
        if (isEditing(message)) {
            holder.textReceivedAttachment.setVisibility(View.GONE);
            holder.imageSentAttachment.setVisibility(View.GONE);
            holder.imageSentAttachment.setImageDrawable(null);
            holder.layoutSentAttachments.removeAllViews();
            holder.layoutSentAttachments.setVisibility(View.GONE);
            return;
        }
        holder.textReceivedAttachment.setVisibility(View.GONE);
        holder.imageSentAttachment.setVisibility(View.GONE);
        holder.imageSentAttachment.setImageDrawable(null);
        holder.layoutSentAttachments.removeAllViews();
        holder.layoutSentAttachments.setVisibility(View.GONE);
        if (message == null || !message.hasAttachment()) return;

        if (message.isSent()) {
            List<ChatMessage.Attachment> attachments = message.getAttachments();
            int rendered = 0;
            for (ChatMessage.Attachment attachment : attachments) {
                if (attachment == null) continue;
                String mime = attachment.mimeType == null ? "" : attachment.mimeType.toLowerCase(Locale.US);
                String name = attachment.name == null || attachment.name.trim().isEmpty() ? "Attachment" : attachment.name.trim();
                if (mime.startsWith("image/")) {
                    Bitmap bitmap = decodeAttachmentBitmap(attachment.base64);
                    if (bitmap != null) {
                        ImageView imageView = createAttachmentImageView(holder.itemView.getContext(), true);
                        imageView.setImageBitmap(bitmap);
                        String base64 = attachment.base64;
                        imageView.setOnClickListener(v -> {
                            if (messageActionListener != null) messageActionListener.onImageClick(base64);
                        });
                        holder.layoutSentAttachments.addView(imageView);
                        rendered++;
                        continue;
                    }
                }
                holder.layoutSentAttachments.addView(createAttachmentChip(holder.itemView.getContext(), name, mime));
                rendered++;
            }
            holder.layoutSentAttachments.setVisibility(rendered > 0 ? View.VISIBLE : View.GONE);
            if (rendered == 0) {
                holder.layoutSentAttachments.addView(createAttachmentChip(
                        holder.itemView.getContext(),
                        "Attachment",
                        "unavailable preview"
                ));
                holder.layoutSentAttachments.setVisibility(View.VISIBLE);
            }
        } else {
            int count = message.getAttachments().size();
            String suffix = count > 1 ? " (+" + (count - 1) + " more)" : "";
            holder.textReceivedAttachment.setText("Attached: " + message.getAttachmentName() + suffix);
            holder.textReceivedAttachment.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap decodeAttachmentBitmap(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isLastUserMessage(int position) {
        if (position < 0 || position >= messages.size()) return false;
        ChatMessage current = messages.get(position);
        if (current == null || !current.isSent()) return false;
        for (int i = messages.size() - 1; i > position; i--) {
            ChatMessage m = messages.get(i);
            if (m != null && m.isSent()) return false;
        }
        return true;
    }

    private void bindEmbeddedImages(ChatViewHolder holder, String content) {
        holder.layoutReceivedImages.removeAllViews();
        List<String> imageUrls = extractImageUrls(content);
        if (imageUrls.isEmpty()) {
            holder.layoutReceivedImages.setVisibility(View.GONE);
            return;
        }
        holder.layoutReceivedImages.setVisibility(View.VISIBLE);
        for (String imageUrl : imageUrls) {
            ImageView imageView = createAttachmentImageView(holder.itemView.getContext(), false);
            holder.layoutReceivedImages.addView(imageView);
            loadImage(imageView, imageUrl);
        }
    }

    private void loadImage(ImageView imageView, String imageUrl) {
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        IMAGE_EXECUTOR.execute(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(new URL(imageUrl).openStream());
                imageView.post(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setOnClickListener(v -> {
                            if (messageActionListener != null) {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                messageActionListener.onImageClick(Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT));
                            }
                        });
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private ImageView createAttachmentImageView(Context context, boolean sent) {
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = sent ? dp(context, 8) : dp(context, 6);
        imageView.setLayoutParams(params);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxWidth(dp(context, sent ? 220 : 260));
        imageView.setScaleType(sent ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        return imageView;
    }

    private TextView createAttachmentChip(Context context, String name, String mime) {
        AppCompatTextView chip = new AppCompatTextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, 6);
        chip.setLayoutParams(params);
        chip.setBackgroundResource(R.drawable.assistant_attachment_chip_bg);
        chip.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        chip.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        chip.setMaxWidth(dp(context, 230));
        String safeMime = mime == null || mime.trim().isEmpty() ? "file" : mime;
        chip.setText(name + " (" + safeMime + ")");
        return chip;
    }

    private View createEditableAttachmentChip(Context context, String name, String mime, Runnable onRemove) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(context, 6);
        row.setLayoutParams(params);
        row.setBackgroundResource(R.drawable.assistant_attachment_chip_bg);
        row.setPadding(dp(context, 10), dp(context, 4), dp(context, 6), dp(context, 4));

        AppCompatTextView text = new AppCompatTextView(context);
        text.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        text.setMaxWidth(dp(context, 140));
        text.setText(name + " (" + mime + ")");
        row.addView(text);

        MaterialButton remove = new MaterialButton(context, null, com.google.android.material.R.attr.materialIconButtonStyle);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(context, 20), dp(context, 20));
        removeParams.leftMargin = dp(context, 4);
        remove.setLayoutParams(removeParams);
        remove.setMinimumWidth(0);
        remove.setMinimumHeight(0);
        remove.setMinWidth(0);
        remove.setMinHeight(0);
        remove.setInsetTop(0);
        remove.setInsetBottom(0);
        remove.setPadding(0, 0, 0, 0);
        remove.setIconSize(dp(context, 12));
        remove.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        remove.setOnClickListener(v -> onRemove.run());
        row.addView(remove);
        return row;
    }

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    private List<String> extractImageUrls(String content) {
        List<String> urls = new ArrayList<>();
        if (content == null) return urls;
        int start = content.indexOf(IMAGE_PREFIX);
        while (start >= 0) {
            int end = content.indexOf(IMAGE_SUFFIX, start);
            if (end < 0) break;
            String url = content.substring(start + IMAGE_PREFIX.length(), end).trim();
            if (!url.isEmpty()) urls.add(url);
            start = content.indexOf(IMAGE_PREFIX, end + IMAGE_SUFFIX.length());
        }
        Matcher markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(content);
        while (markdownMatcher.find()) {
            String markdownUrl = markdownMatcher.group(1);
            if (markdownUrl != null && !markdownUrl.trim().isEmpty() && !urls.contains(markdownUrl.trim())) {
                urls.add(markdownUrl.trim());
            }
        }
        return urls;
    }

    private String stripImageMarker(String content) {
        if (content == null) return "";
        String cleaned = content;
        int start = cleaned.indexOf(IMAGE_PREFIX);
        while (start >= 0) {
            int end = cleaned.indexOf(IMAGE_SUFFIX, start);
            if (end < 0) break;
            cleaned = cleaned.substring(0, start) + cleaned.substring(end + IMAGE_SUFFIX.length());
            start = cleaned.indexOf(IMAGE_PREFIX);
        }
        cleaned = MARKDOWN_IMAGE_PATTERN.matcher(cleaned).replaceAll("").trim();
        return cleaned;
    }

    private CharSequence formatRichText(String rawContent, Context context, boolean isSent, boolean isCommandCard, boolean applyLinkify) {
        if (rawContent == null) return "";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        String[] lines = rawContent.split("\\n", -1);
        boolean inCodeBlock = false;

        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            int start = builder.length();
            if (inCodeBlock) {
                builder.append(rawLine).append("\n");
                builder.setSpan(new TypefaceSpan("monospace"), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new BackgroundColorSpan(Color.parseColor("#15000000")), start, Math.max(start, builder.length() - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            // Tables
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                String line = trimmed.substring(1, trimmed.length() - 1).replace("|", "  │  ");
                builder.append(line).append("\n");
                builder.setSpan(new TypefaceSpan("monospace"), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new RelativeSizeSpan(0.85f), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            // Quotes
            if (trimmed.startsWith("> ")) {
                builder.append(trimmed.substring(2)).append("\n");
                builder.setSpan(new StyleSpan(Typeface.ITALIC), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new BackgroundColorSpan(Color.parseColor("#10000000")), start, Math.max(start, builder.length() - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            // Bullets
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                builder.append("  • ").append(trimmed.substring(2)).append("\n");
                continue;
            }

            // Numbered
            if (trimmed.matches("\\d+\\.\\s+.*")) {
                builder.append("  ").append(trimmed).append("\n");
                continue;
            }

            builder.append(rawLine).append("\n");
        }
        trimTrailingNewline(builder);

        // Inline Code
        applyInlineStyle(builder, "`", new TypefaceSpan("monospace"), new BackgroundColorSpan(Color.parseColor("#15000000")));
        // Bold
        applyInlineStyle(builder, "**", new StyleSpan(Typeface.BOLD));
        // Italic
        applyInlineStyle(builder, "*", new StyleSpan(Typeface.ITALIC));
        // Strikethrough
        applyInlineStyle(builder, "~~", new android.text.style.StrikethroughSpan());

        // Highlight recognized commands using shared parser/registry metadata
        for (AssistantCommandParser.Span span : AssistantCommandParser.findCommands(builder.toString())) {
            int start = Math.max(0, Math.min(span.start, builder.length()));
            int end = Math.max(start, Math.min(span.end, builder.length()));
            if (end > start) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new BackgroundColorSpan(Color.parseColor("#20888888")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (commandListener != null) {
                    String commandText = span.command;
                    builder.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            commandListener.onCommandClick(commandText);
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                        }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        if (applyLinkify) {
            try {
                Linkify.addLinks(builder, Linkify.WEB_URLS);
            } catch (Exception ignored) {}
        }
        return builder;
    }

    private void applyInlineStyle(SpannableStringBuilder builder, String delimiter, Object... spans) {
        String text = builder.toString();
        int start = text.indexOf(delimiter);
        while (start != -1) {
            int end = text.indexOf(delimiter, start + delimiter.length());
            if (end == -1) break;

            builder.delete(end, end + delimiter.length());
            builder.delete(start, start + delimiter.length());

            for (Object span : spans) {
                // Clone span if possible or create new instance of same type (simplified for now)
                builder.setSpan(span, start, end - delimiter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            text = builder.toString();
            start = text.indexOf(delimiter, end - delimiter.length());
        }
    }

    private void trimTrailingNewline(SpannableStringBuilder builder) {
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.delete(builder.length() - 1, builder.length());
        }
    }

    private static final class ChatDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldMessages;
        private final List<ChatMessage> newMessages;

        ChatDiffCallback(List<ChatMessage> oldMessages, List<ChatMessage> newMessages) {
            this.oldMessages = oldMessages;
            this.newMessages = newMessages;
        }

        @Override public int getOldListSize() { return oldMessages.size(); }
        @Override public int getNewListSize() { return newMessages.size(); }

        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldMessages.get(oldPos).getId().equals(newMessages.get(newPos).getId());
        }

        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            ChatMessage o = oldMessages.get(oldPos);
            ChatMessage n = newMessages.get(newPos);
            boolean sameCore = o.getContent().equals(n.getContent())
                    && o.isSent() == n.isSent()
                    && o.getAttachments().size() == n.getAttachments().size();
            if (!sameCore) return false;

            boolean oldWasLastUser = isLastUserMessage(oldMessages, oldPos);
            boolean newIsLastUser = isLastUserMessage(newMessages, newPos);
            return oldWasLastUser == newIsLastUser;
        }

        private boolean isLastUserMessage(List<ChatMessage> list, int position) {
            if (position < 0 || position >= list.size()) return false;
            ChatMessage current = list.get(position);
            if (current == null || !current.isSent()) return false;
            for (int i = list.size() - 1; i > position; i--) {
                ChatMessage m = list.get(i);
                if (m != null && m.isSent()) return false;
            }
            return true;
        }
    }
}
