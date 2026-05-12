package brettdansmith.drugdiary.ui.assistant;

import brettdansmith.drugdiary.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
import android.text.style.StrikethroughSpan;
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
import java.util.Locale;
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

    private List<ChatMessage> messages;
    private OnCommandClickListener commandListener;
    private OnMessageActionListener messageActionListener;

    public interface OnCommandClickListener {
        void onCommandClick(String command);
    }

    public interface OnMessageActionListener {
        void onEdit(ChatMessage message);
        void onDelete(ChatMessage message);
        void onImageClick(String base64);
    }

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
        setHasStableIds(true);
    }

    public void submitMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> nextMessages = newMessages == null ? new ArrayList<>() : new ArrayList<>(newMessages);
        String previousLastUserId = lastUserMessageId(this.messages);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ChatDiffCallback(this.messages, nextMessages), false);
        this.messages = nextMessages;
        diff.dispatchUpdatesTo(this);
        String nextLastUserId = lastUserMessageId(this.messages);
        if (!stringEquals(previousLastUserId, nextLastUserId)) {
            notifyMessageChanged(previousLastUserId);
            notifyMessageChanged(nextLastUserId);
        }
    }
    
    public void updateLastMessage(ChatMessage message) {
        if (messages.isEmpty()) {
            messages.add(message);
            notifyItemInserted(0);
        } else {
            messages.set(messages.size() - 1, message);
            notifyItemChanged(messages.size() - 1, "text_only");
        }
    }

    public void setOnCommandClickListener(OnCommandClickListener listener) {
        this.commandListener = listener;
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.messageActionListener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains("text_only")) {
            bindMessageData(holder, position, true);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        bindMessageData(holder, position, false);
    }

    private void bindMessageData(ChatViewHolder holder, int position, boolean isTextOnlyUpdate) {
        ChatMessage message = messages.get(position);
        Context context = holder.itemView.getContext();
        String expandedContent = message.getContent() == null ? "" : message.getContent();

        if (!isTextOnlyUpdate) {
            holder.layoutSent.setVisibility(View.GONE);
            holder.layoutReceived.setVisibility(View.GONE);
            holder.layoutCommand.setVisibility(View.GONE);
            holder.layoutSystem.setVisibility(View.GONE);
            holder.layoutMessageActions.setVisibility(View.GONE);
            bindAttachmentPreview(holder, message);
        }

        if (expandedContent.startsWith(COMMAND_PREFIX)) {
            holder.layoutCommand.setVisibility(View.VISIBLE);
            String cmdText = expandedContent.substring(COMMAND_PREFIX.length()).trim();
            if (isTextOnlyUpdate) {
                holder.textCommand.setText(formatRichText(cmdText, context, false, true, true));
                setupTextViewForLinks(holder.textCommand);
            } else {
                bindTextAndImage(holder.textCommand, holder.imageCommand, holder.imageCommandSecondary, cmdText, false, true);
            }
            
            if (cmdText.contains("failed") || cmdText.contains("error") || cmdText.contains("Error")) {
                holder.cardCommand.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorErrorContainer, android.graphics.Color.RED));
                holder.textCommand.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnErrorContainer, android.graphics.Color.WHITE));
            } else {
                holder.cardCommand.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, android.graphics.Color.GRAY));
                holder.textCommand.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, android.graphics.Color.BLACK));
            }
        } else if (expandedContent.startsWith(SYSTEM_PREFIX)) {
            holder.layoutSystem.setVisibility(View.VISIBLE);
            String sysText = expandedContent.substring(SYSTEM_PREFIX.length()).trim();
            holder.textSystem.setText(sysText);
            setupTextViewForLinks(holder.textSystem);
            
            if (sysText.contains("Error") || sysText.contains("failed")) {
                holder.cardSystem.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorErrorContainer, android.graphics.Color.RED));
                holder.textSystem.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnErrorContainer, android.graphics.Color.WHITE));
            } else {
                holder.cardSystem.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, android.graphics.Color.LTGRAY));
                holder.textSystem.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, android.graphics.Color.BLACK));
            }
        } else if (message.isSent()) {
            holder.layoutSent.setVisibility(View.VISIBLE);
            holder.textSent.setText(formatRichText(expandedContent, context, true, false, true));
            setupTextViewForLinks(holder.textSent);
            if (!isTextOnlyUpdate) {
                bindMessageActions(holder, message, isLastUserMessage(position));
            }
        } else {
            holder.layoutReceived.setVisibility(View.VISIBLE);
            holder.cardReceived.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(context, android.R.attr.windowBackground, android.graphics.Color.WHITE));
            holder.cardReceived.setStrokeWidth(0);
            holder.textReceived.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.BLACK));
            
            if (isTextOnlyUpdate) {
                holder.textReceived.setText(formatRichText(stripImageMarker(expandedContent).trim(), context, false, false, true));
                setupTextViewForLinks(holder.textReceived);
            } else {
                bindTextAndImage(holder.textReceived, holder.imageReceived, holder.imageReceivedSecondary, expandedContent, false, false);
            }
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
        TextView textSentAttachment, textReceivedAttachment;
        ImageView imageReceived, imageReceivedSecondary, imageCommand, imageCommandSecondary, imageSentAttachment;
        MaterialCardView cardReceived, cardCommand, cardSystem;
        MaterialButton buttonEdit, buttonDelete;

        ChatViewHolder(View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layout_sent);
            layoutReceived = itemView.findViewById(R.id.layout_received);
            layoutCommand = itemView.findViewById(R.id.layout_command);
            layoutSystem = itemView.findViewById(R.id.layout_system);
            layoutMessageActions = itemView.findViewById(R.id.layout_message_actions);
            
            textSent = itemView.findViewById(R.id.text_sent);
            textReceived = itemView.findViewById(R.id.text_received);
            textCommand = itemView.findViewById(R.id.text_command);
            textSystem = itemView.findViewById(R.id.text_system);
            textSentAttachment = itemView.findViewById(R.id.text_sent_attachment);
            textReceivedAttachment = itemView.findViewById(R.id.text_received_attachment);
            
            imageReceived = itemView.findViewById(R.id.image_received);
            imageReceivedSecondary = itemView.findViewById(R.id.image_received_secondary);
            imageCommand = itemView.findViewById(R.id.image_command);
            imageCommandSecondary = itemView.findViewById(R.id.image_command_secondary);
            imageSentAttachment = itemView.findViewById(R.id.image_sent_attachment);
            
            cardReceived = (MaterialCardView) ((ViewGroup) layoutReceived).getChildAt(0);
            cardCommand = (MaterialCardView) ((ViewGroup) layoutCommand).getChildAt(0);
            cardSystem = (MaterialCardView) ((ViewGroup) layoutSystem).getChildAt(0);

            buttonEdit = itemView.findViewById(R.id.button_edit_message);
            buttonDelete = itemView.findViewById(R.id.button_delete_message);
        }
    }

    private void bindMessageActions(ChatViewHolder holder, ChatMessage message, boolean allowEdit) {
        boolean canModify = message != null && message.isSent() && allowEdit;
        holder.layoutMessageActions.setVisibility(canModify ? View.VISIBLE : View.GONE);
        holder.buttonEdit.setVisibility(canModify ? View.VISIBLE : View.GONE);
        holder.buttonDelete.setVisibility(canModify ? View.VISIBLE : View.GONE);
        if (!canModify) {
            holder.buttonEdit.setOnClickListener(null);
            holder.buttonDelete.setOnClickListener(null);
            return;
        }
        holder.buttonEdit.setOnClickListener(v -> {
            if (messageActionListener != null) messageActionListener.onEdit(message);
        });
        holder.buttonDelete.setOnClickListener(v -> {
            if (messageActionListener != null) messageActionListener.onDelete(message);
        });
    }

    private void bindAttachmentPreview(ChatViewHolder holder, ChatMessage message) {
        holder.textSentAttachment.setVisibility(View.GONE);
        holder.textReceivedAttachment.setVisibility(View.GONE);
        holder.imageSentAttachment.setVisibility(View.GONE);
        holder.imageSentAttachment.setImageDrawable(null);
        if (message == null || !message.hasAttachment()) return;

        String label = attachmentLabel(message);
        if (message.isSent()) {
            holder.textSentAttachment.setText(label);
            holder.textSentAttachment.setVisibility(View.VISIBLE);
            if (message.hasImageAttachment()) {
                Bitmap bitmap = decodeAttachmentBitmap(message);
                if (bitmap != null) {
                    holder.imageSentAttachment.setImageBitmap(bitmap);
                    holder.imageSentAttachment.setVisibility(View.VISIBLE);
                    holder.imageSentAttachment.setOnClickListener(v -> {
                        if (messageActionListener != null) messageActionListener.onImageClick(message.getAttachmentBase64());
                    });
                }
            }
        } else {
            holder.textReceivedAttachment.setText(label);
            holder.textReceivedAttachment.setVisibility(View.VISIBLE);
        }
    }

    private String attachmentLabel(ChatMessage message) {
        String name = message.getAttachmentName();
        String mime = message.getAttachmentMimeType();
        if (name == null || name.trim().isEmpty()) name = "attachment";
        if (mime == null || mime.trim().isEmpty()) return "Attached: " + name;
        return "Attached: " + name + " (" + mime + ")";
    }

    private Bitmap decodeAttachmentBitmap(ChatMessage message) {
        try {
            byte[] bytes = Base64.decode(message.getAttachmentBase64(), Base64.DEFAULT);
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
            ChatMessage message = messages.get(i);
            if (message != null && message.isSent()) return false;
        }
        return true;
    }

    private String lastUserMessageId(List<ChatMessage> source) {
        if (source == null) return "";
        for (int i = source.size() - 1; i >= 0; i--) {
            ChatMessage message = source.get(i);
            if (message != null && message.isSent()) return message.getId();
        }
        return "";
    }

    private void notifyMessageChanged(String messageId) {
        if (messageId == null || messageId.isEmpty()) return;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message != null && messageId.equals(message.getId())) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    private void bindTextAndImage(TextView textView, ImageView imageView, ImageView secondaryImageView, String content, boolean isSent, boolean isCommandCard) {
        List<String> imageUrls = extractImageUrls(content);
        String imageUrl = imageUrls.isEmpty() ? "" : imageUrls.get(0);
        String secondaryImageUrl = imageUrls.size() > 1 ? imageUrls.get(1) : "";
        String visibleText = stripImageMarker(content).trim();
        if (imageUrl.isEmpty()) imageUrl = firstImageUrl(visibleText);
        
        textView.setText(formatRichText(visibleText, textView.getContext(), isSent, isCommandCard, true));
        setupTextViewForLinks(textView);
        
        if (imageUrl.isEmpty()) {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
            secondaryImageView.setImageDrawable(null);
            secondaryImageView.setVisibility(View.GONE);
            return;
        }
        loadImage(imageView, imageUrl);
        if (secondaryImageUrl.isEmpty()) {
            secondaryImageView.setImageDrawable(null);
            secondaryImageView.setVisibility(View.GONE);
        } else {
            loadImage(secondaryImageView, secondaryImageUrl);
        }
    }

    private void loadImage(ImageView imageView, String imageUrl) {
        final String resolvedImageUrl = imageUrl;
        imageView.setTag(resolvedImageUrl);
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        
        IMAGE_EXECUTOR.execute(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(new URL(resolvedImageUrl).openStream());
                imageView.post(() -> {
                    if (resolvedImageUrl.equals(imageView.getTag())) {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setOnClickListener(v -> {
                                if (messageActionListener != null) {
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    String base64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
                                    messageActionListener.onImageClick(base64);
                                }
                            });
                        } else {
                            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                        }
                    }
                });
            } catch (Exception ignored) {
                imageView.post(() -> {
                    if (resolvedImageUrl.equals(imageView.getTag())) {
                        imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                    }
                });
            }
        });
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
        Matcher markdownImage = MARKDOWN_IMAGE_PATTERN.matcher(content);
        while (markdownImage.find()) {
            String url = cleanUrl(markdownImage.group(1));
            if (!url.isEmpty()) urls.add(url);
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
        return MARKDOWN_IMAGE_PATTERN.matcher(cleaned).replaceAll("");
    }

    private CharSequence formatRichText(String content, Context context, boolean isSent, boolean isCommandCard, boolean applyLinkify) {
        if (content == null) content = "";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = content.split("\\n", -1);
        
        boolean inCodeBlock = false;
        
        for (String rawLine : lines) {
            if (rawLine.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue; 
            }
            
            int start = builder.length();
            String line = rawLine;
            
            if (inCodeBlock) {
                builder.append(line).append("\n");
                builder.setSpan(new TypefaceSpan("monospace"), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new BackgroundColorSpan(Color.parseColor("#20888888")), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            
            String trimmed = line.trim();
            boolean heading = trimmed.startsWith("# ");
            boolean subHeading = trimmed.startsWith("## ") || (trimmed.startsWith("===") && trimmed.endsWith("==="));
            boolean bullet = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ");
            boolean numbered = trimmed.matches("\\d+\\.\\s+.*");
            boolean checklist = trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ");
            boolean quote = trimmed.startsWith("> ");
            boolean tableRow = trimmed.startsWith("|") && trimmed.endsWith("|");
            boolean tableDivider = tableRow && trimmed.matches("\\|?\\s*:?[-]{3,}:?\\s*(\\|\\s*:?[-]{3,}:?\\s*)+\\|?");
            
            if (heading) {
                line = trimmed.substring(2).trim();
            } else if (subHeading) {
                line = trimmed.startsWith("===") ? trimmed.replace("=", "").trim() : trimmed.replaceFirst("^#+\\s*", "").trim();
            } else if (checklist) {
                boolean checked = trimmed.toLowerCase(Locale.US).startsWith("- [x]");
                line = (checked ? "[x] " : "[ ] ") + trimmed.substring(6).trim();
            } else if (bullet) {
                line = "• " + trimmed.substring(2).trim();
            } else if (numbered) {
                line = trimmed;
            } else if (quote) {
                line = trimmed.substring(2).trim();
            } else if (tableDivider) {
                continue;
            } else if (tableRow) {
                line = trimmed.substring(1, trimmed.length() - 1).replace("|", "   ");
            }
            
            builder.append(line).append("\n");
            int end = builder.length();
            
            if (heading || subHeading || "Local command result:\n\n".trim().equals(line)) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, Math.max(start, end - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new RelativeSizeSpan(heading ? 1.22f : 1.1f), start, Math.max(start, end - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (quote) {
                builder.setSpan(new BackgroundColorSpan(Color.parseColor("#12000000")), start, Math.max(start, end - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.ITALIC), start, Math.max(start, end - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (tableRow) {
                builder.setSpan(new TypefaceSpan("monospace"), start, Math.max(start, end - 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (bullet || numbered || checklist) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    builder.setSpan(new StyleSpan(Typeface.BOLD), start, start + colon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        trimTrailingNewline(builder);
        
        applyInlineCode(builder);
        applyInlineStrikethrough(builder);
        applyMarkdownLinks(builder);
        applyInlineBold(builder);
        applyInlineItalic(builder);
        
        if (applyLinkify) {
            try {
                Linkify.addLinks(builder, Linkify.WEB_URLS);
            } catch (Exception ignored) {} 
        }

        try {
            for (AssistantCommandParser.Span span : AssistantCommandParser.findCommands(builder.toString())) {
                final String finalCmd = span.command;
                final boolean isSuggestion = span.isSuggestion;
                
                // Add a bold and background color highlight span to make it look like a chip
                int bgColor = isSuggestion ? 0x228B008B : 0x220000FF; // Semi-transparent magenta/blue
                builder.setSpan(new BackgroundColorSpan(bgColor), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                builder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (commandListener != null) {
                            commandListener.onCommandClick(finalCmd);
                        }
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        if (isSuggestion) {
                            ds.setColor(com.google.android.material.color.MaterialColors.getColor(context, androidx.appcompat.R.attr.colorAccent, Color.MAGENTA));
                            ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                            return;
                        }
                        if (isSent) {
                            ds.setColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
                            ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        } else if (isCommandCard) {
                            ds.setColor(com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLUE));
                            ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        } else {
                            ds.setColor(com.google.android.material.color.MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, Color.BLUE));
                            ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        }
                    }
                }, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {} 
        
        try {
            Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(builder.toString());
            int placeholderSearchStart = 0;
            while (placeholderMatcher.find(placeholderSearchStart)) {
                int start = placeholderMatcher.start();
                int end = placeholderMatcher.end();
                final String foundPlaceholder = placeholderMatcher.group();
                
                builder.setSpan(new BackgroundColorSpan(0x22FFA500), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (commandListener != null) {
                            commandListener.onCommandClick(foundPlaceholder);
                        }
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(com.google.android.material.color.MaterialColors.getColor(context, androidx.appcompat.R.attr.colorAccent, Color.rgb(255, 165, 0))); // Orange
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                placeholderSearchStart = end;
                placeholderMatcher = PLACEHOLDER_PATTERN.matcher(builder.toString());
            }
        } catch (Exception ignored) {}
        
        // Remove structural tags from visible text, keeping inner content text intact
        String[] prefixes = {"[[suggest:", "[[command:"};
        for (String prefix : prefixes) {
            int prefixLen = prefix.length();
            int idx = builder.toString().indexOf(prefix);
            while (idx >= 0) {
                int endIdx = builder.toString().indexOf("]]", idx);
                if (endIdx < 0) break;
                builder.delete(endIdx, endIdx + 2);
                builder.delete(idx, idx + prefixLen);
                idx = builder.toString().indexOf(prefix);
            }
        }
        
        return builder;
    }

    private void trimTrailingNewline(SpannableStringBuilder builder) {
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.delete(builder.length() - 1, builder.length());
        }
    }

    private void applyInlineCode(SpannableStringBuilder builder) {
        String marker = "`";
        int startMarker = builder.toString().indexOf(marker);
        while (startMarker >= 0) {
            int endMarker = builder.toString().indexOf(marker, startMarker + marker.length());
            if (endMarker < 0) break;
            builder.delete(endMarker, endMarker + marker.length());
            builder.delete(startMarker, startMarker + marker.length());
            builder.setSpan(new TypefaceSpan("monospace"), startMarker, endMarker - marker.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new BackgroundColorSpan(Color.parseColor("#20888888")), startMarker, endMarker - marker.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            startMarker = builder.toString().indexOf(marker, startMarker + 1);
        }
    }

    private void applyInlineStrikethrough(SpannableStringBuilder builder) {
        String marker = "~~";
        int startMarker = builder.toString().indexOf(marker);
        while (startMarker >= 0) {
            int endMarker = builder.toString().indexOf(marker, startMarker + marker.length());
            if (endMarker < 0) break;
            builder.delete(endMarker, endMarker + marker.length());
            builder.delete(startMarker, startMarker + marker.length());
            builder.setSpan(new StrikethroughSpan(), startMarker, endMarker - marker.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            startMarker = builder.toString().indexOf(marker, startMarker + 1);
        }
    }

    private void applyMarkdownLinks(SpannableStringBuilder builder) {
        Pattern pattern = Pattern.compile("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)");
        Matcher matcher = pattern.matcher(builder.toString());
        int searchStart = 0;
        while (matcher.find(searchStart)) {
            String label = matcher.group(1);
            String url = matcher.group(2);
            int start = matcher.start();
            int end = matcher.end();
            builder.replace(start, end, label);
            builder.setSpan(new URLSpan(url), start, start + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            searchStart = start + label.length();
            matcher = pattern.matcher(builder.toString());
        }
    }

    private void applyInlineBold(SpannableStringBuilder builder) {
        applyStyle(builder, "**", Typeface.BOLD);
    }

    private void applyInlineItalic(SpannableStringBuilder builder) {
        applyStyle(builder, "*", Typeface.ITALIC);
    }

    private void applyStyle(SpannableStringBuilder builder, String marker, int style) {
        int markerLen = marker.length();
        int startMarker = builder.toString().indexOf(marker);
        while (startMarker >= 0) {
            int endMarker = builder.toString().indexOf(marker, startMarker + markerLen);
            if (endMarker < 0) break;
            builder.delete(endMarker, endMarker + markerLen);
            builder.delete(startMarker, startMarker + markerLen);
            builder.setSpan(new StyleSpan(style), startMarker, endMarker - markerLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            startMarker = builder.toString().indexOf(marker, startMarker + 1);
        }
    }

    private String firstImageUrl(String content) {
        if (content == null) return "";
        Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            String url = cleanUrl(matcher.group());
            if (isImageUrl(url)) return url;
        }
        return "";
    }

    private boolean isImageUrl(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif")
                || lower.contains("pubchem.ncbi.nlm.nih.gov/image/")
                || lower.contains("pubchem.ncbi.nlm.nih.gov/image/imgsrv.fcgi");
    }

    private String cleanUrl(String url) {
        while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith("]")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static final class ChatDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldMessages;
        private final List<ChatMessage> newMessages;

        ChatDiffCallback(List<ChatMessage> oldMessages, List<ChatMessage> newMessages) {
            this.oldMessages = oldMessages == null ? new ArrayList<>() : oldMessages;
            this.newMessages = newMessages == null ? new ArrayList<>() : newMessages;
        }

        @Override
        public int getOldListSize() {
            return oldMessages.size();
        }

        @Override
        public int getNewListSize() {
            return newMessages.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldMessage = oldMessages.get(oldItemPosition);
            ChatMessage newMessage = newMessages.get(newItemPosition);
            if (oldMessage == null || newMessage == null) return oldMessage == newMessage;
            return stringEquals(oldMessage.getId(), newMessage.getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldMessage = oldMessages.get(oldItemPosition);
            ChatMessage newMessage = newMessages.get(newItemPosition);
            if (oldMessage == null || newMessage == null) return oldMessage == newMessage;
            return oldMessage.isSent() == newMessage.isSent()
                    && oldMessage.getCreatedAt() == newMessage.getCreatedAt()
                    && stringEquals(oldMessage.getContent(), newMessage.getContent())
                    && stringEquals(oldMessage.getAttachmentName(), newMessage.getAttachmentName())
                    && stringEquals(oldMessage.getAttachmentMimeType(), newMessage.getAttachmentMimeType())
                    && stringEquals(oldMessage.getAttachmentBase64(), newMessage.getAttachmentBase64());
        }

        private static boolean stringEquals(String first, String second) {
            if (first == null) return second == null;
            return first.equals(second);
        }
    }

    private static boolean stringEquals(String first, String second) {
        if (first == null) return second == null;
        return first.equals(second);
    }
}
