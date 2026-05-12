package brettdansmith.drugdiary.ui.assistant;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.security.UserSession;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brettdansmith.drugdiary.databinding.FragmentAssistantBinding;
import brettdansmith.drugdiary.assistant.AssistantContextBuilder;
import brettdansmith.drugdiary.assistant.AssistantPlaceholders;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.domain.assistant.AssistantCommandParser;
import brettdansmith.drugdiary.domain.assistant.AssistantCommandRegistry;
import brettdansmith.drugdiary.network.ai.AssistantApiClient;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.ui.assistant.AssistantViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AssistantFragment extends Fragment {
    private static final int MAX_ATTACHMENT_BYTES = 2 * 1024 * 1024;
    private static final int MAX_IMAGE_SOURCE_BYTES = 12 * 1024 * 1024;
    private static final int[] IMAGE_MAX_SIDES = {3200, 2600, 2200, 1800, 1400};
    private static final int[] IMAGE_JPEG_QUALITIES = {96, 92, 88, 84, 80, 74};

    private FragmentAssistantBinding binding;
    private ChatAdapter adapter;
    private AssistantViewModel viewModel;
    private AssistantCommandRegistry commandRegistry;
    
    private ExecutorService networkExecutor;
    private ExecutorService diskExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object chatLock = new Object();
    
    private JSONArray chatSessions = new JSONArray();
    private String currentChatId = "";
    
    private boolean alwaysRunAssistantCommands = false;
    private PendingAttachment pendingAttachment;
    private ActivityResultLauncher<String> imagePicker;
    private ActivityResultLauncher<String> filePicker;
    private ActivityResultLauncher<Void> cameraLauncher;
    
    private Runnable streamUiUpdater;
    private final Runnable renderTabsRunnable = this::renderChatTabsNow;
    private String lastRenderedStreamingText = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) loadAttachmentFromUri(uri, true);
        });
        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) loadAttachmentFromUri(uri, false);
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) attachCameraBitmap(bitmap);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AssistantViewModel.class);
        viewModel.setApplicationContext(requireContext().getApplicationContext());
        networkExecutor = viewModel.getNetworkExecutor();
        diskExecutor = viewModel.getDiskExecutor();
        streamUiUpdater = new Runnable() {
            @Override
            public void run() {
                if (binding == null || !viewModel.isStreaming()) return;
                String currentText = viewModel.getStreamingText();
                if (!currentText.equals(lastRenderedStreamingText)) {
                    lastRenderedStreamingText = currentText;
                    List<ChatMessage> live = viewModel.getMessages().getValue();
                    if (live != null && !live.isEmpty()) {
                        List<ChatMessage> updated = new ArrayList<>(live);
                        ChatMessage last = updated.get(updated.size() - 1);
                        updated.set(updated.size() - 1, new ChatMessage(last.getId(), currentText + " \u2588", false, last.getCreatedAt()));
                        adapter.submitMessages(updated);
                        binding.recyclerChat.scrollToPosition(updated.size() - 1);
                    }
                }
                mainHandler.postDelayed(this, 100);
            }
        };
        commandRegistry = new AssistantCommandRegistry(requireContext(), this::createNewChat);
        
        setupRecyclerView();
        setupInputHandlers();
        observeViewModel();
        
        loadOrInitializeChatState();
        
        String initialPrompt = viewModel.consumeInitialPrompt();
        if (initialPrompt != null && !initialPrompt.isEmpty()) {
            binding.editMessage.setText(initialPrompt);
            binding.editMessage.requestFocus();
            binding.editMessage.setSelection(initialPrompt.length());
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(new ArrayList<>());
        adapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onEdit(ChatMessage message) {
                showEditMessageDialog(message);
            }

            @Override
            public void onDelete(ChatMessage message) {
                showDeleteMessageDialog(message);
            }

            @Override
            public void onImageClick(String base64) {
                showFullscreenImage(base64);
            }
        });
        adapter.setOnCommandClickListener(this::insertCommandIntoInput);
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChat.setAdapter(adapter);
    }

    private void setupInputHandlers() {
        binding.buttonSend.setOnClickListener(v -> sendMessage());
        binding.buttonNewChat.setOnClickListener(this::showNewChatOptions);
        binding.buttonAddAttachment.setOnClickListener(this::showAttachmentMenu);
        binding.buttonRemoveAttachment.setOnClickListener(v -> clearPendingAttachment());
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (!viewModel.isStreaming()) {
                adapter.submitMessages(messages);
                if (messages != null && !messages.isEmpty()) {
                    binding.recyclerChat.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    private void loadOrInitializeChatState() {
        if (viewModel.isChatStateLoaded()) {
            chatSessions = viewModel.getChatSessions();
            currentChatId = viewModel.getActiveChatId();
            if (currentChatId == null || currentChatId.trim().isEmpty()) {
                JSONObject first = chatSessions.length() > 0 ? chatSessions.optJSONObject(0) : null;
                currentChatId = first == null ? "" : first.optString("id", "");
            }
            JSONObject active = findSession(currentChatId);
            if (active != null) {
                viewModel.setMessages(messagesFromJson(active.optJSONArray("messages")));
            }
            renderChatTabs();
            openRequestedChatIfAny();
        } else if (!AppSettings.state(requireContext()).assistantMemory) {
            createNewChat("General");
        } else {
            loadChatSessionsFromEncryptedDiskAsync();
        }
    }
    
    private void showAttachmentMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, R.string.attach_image).setIcon(android.R.drawable.ic_menu_gallery);
        popup.getMenu().add(0, 2, 1, R.string.attach_file).setIcon(android.R.drawable.ic_menu_upload);
        popup.getMenu().add(0, 3, 2, R.string.take_photo).setIcon(android.R.drawable.ic_menu_camera);
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: imagePicker.launch("image/*"); return true;
                case 2: filePicker.launch("*/*"); return true;
                case 3: cameraLauncher.launch(null); return true;
            }
            return false;
        });
        popup.show();
    }

    private void showFullscreenImage(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) {
                FullscreenImageDialog.show(requireContext(), bitmap);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        openRequestedChatIfAny();
    }

    private void openRequestedChatIfAny() {
        if (viewModel == null || binding == null || !viewModel.isChatStateLoaded()) return;
        String requestedChatId = viewModel.consumeRequestedChatId();
        if (requestedChatId.isEmpty()) return;
        JSONObject session = findSession(requestedChatId);
        if (session == null) return;
        currentChatId = requestedChatId;
        viewModel.setChatState(chatSessions, currentChatId);
        viewModel.setMessages(messagesFromJson(session.optJSONArray("messages")));
        renderChatTabs();
        persistActiveChatIdAsync();
    }

    private void sendMessage() {
        String content = binding.editMessage.getText().toString().trim();
        if (content.isEmpty() && pendingAttachment == null) return;
        PendingAttachment attachment = pendingAttachment;
        if (content.isEmpty() && attachment != null) {
            content = attachment.mimeType.startsWith("image/")
                    ? "Please analyze this image. If it shows medication packaging, identify the medication, strength, and any useful label details. If confident, offer to add it to my medications."
                    : "Please review this attached file and summarize anything relevant.";
        }
        
        Context appContext = requireContext().getApplicationContext();
        JSONObject data = isPrivateSession(currentChatId) ? new JSONObject() : EncryptedProfileStore.loadProfileData(appContext);
        String expandedContent = AssistantPlaceholders.expand(appContext, data, content);

        binding.editMessage.setText("");
        clearPendingAttachment();
        processInput(expandedContent, true, false, attachment);
    }

    private void loadAttachmentFromUri(Uri uri, boolean preferImage) {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
                PendingAttachment attachment = readAttachment(appContext, uri, preferImage);
                mainHandler.post(() -> setPendingAttachment(attachment));
            } catch (Exception e) {
                mainHandler.post(() -> android.widget.Toast.makeText(requireContext(), R.string.attachment_read_failed, android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }

    private PendingAttachment readAttachment(Context context, Uri uri, boolean preferImage) throws Exception {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = preferImage ? "image/jpeg" : "application/octet-stream";
        }
        String name = displayNameFor(context, uri);
        boolean image = mimeType.startsWith("image/");
        byte[] bytes;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bytes = readBytes(input, image ? MAX_IMAGE_SOURCE_BYTES : MAX_ATTACHMENT_BYTES);
        }
        if (image) {
            PreparedImage prepared = prepareImageForUpload(bytes, mimeType);
            bytes = prepared.bytes;
            mimeType = prepared.mimeType;
        }
        if (bytes.length > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("Attachment too large");
        }
        return new PendingAttachment(name, mimeType, Base64.encodeToString(bytes, Base64.NO_WRAP));
    }

    private void attachCameraBitmap(Bitmap bitmap) {
        try {
            PreparedImage prepared = encodeBitmapForUpload(bitmap);
            setPendingAttachment(new PendingAttachment("camera-photo.jpg", prepared.mimeType, Base64.encodeToString(prepared.bytes, Base64.NO_WRAP)));
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), R.string.attachment_read_failed, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void setPendingAttachment(PendingAttachment attachment) {
        pendingAttachment = attachment;
        if (binding == null) return;
        binding.layoutAttachmentPreview.setVisibility(View.VISIBLE);
        binding.textAttachmentPreview.setText(getString(R.string.attachment_ready, attachment.name));
    }

    private void clearPendingAttachment() {
        pendingAttachment = null;
        if (binding == null) return;
        binding.layoutAttachmentPreview.setVisibility(View.GONE);
        binding.textAttachmentPreview.setText("");
    }

    private String displayNameFor(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) return name.trim();
                }
            }
        } catch (Exception ignored) {
        }
        String path = uri.getLastPathSegment();
        return path == null || path.trim().isEmpty() ? "attachment" : path;
    }

    private byte[] readBytes(InputStream input, int limit) throws Exception {
        if (input == null) throw new IllegalArgumentException("Attachment stream unavailable");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IllegalArgumentException("Attachment too large");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private PreparedImage prepareImageForUpload(byte[] originalBytes, String mimeType) throws Exception {
        if (originalBytes.length <= MAX_ATTACHMENT_BYTES) {
            return new PreparedImage(originalBytes, mimeType);
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length);
        if (bitmap == null) throw new IllegalArgumentException("Image could not be decoded");
        return encodeBitmapForUpload(bitmap);
    }

    private PreparedImage encodeBitmapForUpload(Bitmap bitmap) throws Exception {
        if (bitmap == null) throw new IllegalArgumentException("Image unavailable");
        for (int maxSide : IMAGE_MAX_SIDES) {
            Bitmap scaled = scaleBitmapIfNeeded(bitmap, maxSide);
            for (int quality : IMAGE_JPEG_QUALITIES) {
                byte[] encoded = encodeBitmap(scaled, Bitmap.CompressFormat.JPEG, quality);
                if (encoded.length <= MAX_ATTACHMENT_BYTES) {
                    return new PreparedImage(encoded, "image/jpeg");
                }
            }
        }
        throw new IllegalArgumentException("Image is too large to prepare");
    }

    private Bitmap scaleBitmapIfNeeded(Bitmap bitmap, int maxSide) {
        int side = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (side <= maxSide) return bitmap;
        float scale = maxSide / (float) side;
        int width = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(bitmap.getHeight() * scale));
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private byte[] encodeBitmap(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(format, quality, output);
        return output.toByteArray();
    }
    
    private void processInput(String content, boolean showUserMessage, boolean isAiTriggered) {
        processInput(content, showUserMessage, isAiTriggered, null);
    }

    private void processInput(String content, boolean showUserMessage, boolean isAiTriggered, @Nullable PendingAttachment attachment) {
        if (showUserMessage) {
            if (attachment == null) {
                viewModel.addMessage(new ChatMessage(content, true));
            } else {
                viewModel.addMessage(new ChatMessage(content, true, attachment.name, attachment.mimeType, attachment.base64));
            }
        }

        if (content.startsWith("/")) {
            boolean includeInContext = content.startsWith("//");
            String command = normalizeCommand(content);
            if (!AssistantCommandParser.isKnownCommand(command)) {
                viewModel.addMessage(new ChatMessage(commandResponse("Unknown command. Use [[command:/help]] or [[command:/?]] for available commands."), false));
                saveCurrentChatToEncryptedDiskAsync();
                return;
            }
            if (isAiTriggered && !alwaysRunAssistantCommands) {
                confirmAssistantCommand(command, includeInContext);
                return;
            }
            if (isAsyncCommand(command)) {
                runAsyncCommand(command, includeInContext, isAiTriggered);
            } else {
                String response = handleCommand(command, includeInContext);
                viewModel.addMessage(new ChatMessage(formatCommandReply(response, includeInContext), includeInContext));
                saveCurrentChatToEncryptedDiskAsync();
                if (isAiTriggered && includeInContext) {
                    requestOpenAiResponse();
                }
            }
            return;
        }

        saveCurrentChatToEncryptedDiskAsync();
        requestOpenAiResponse();
    }

    private void requestOpenAiResponse() {
        Context context = getContext();
        Context appContext = context == null ? viewModel.getApplicationContext() : context.getApplicationContext();
        if (appContext == null) {
            viewModel.addMessage(new ChatMessage("[[system-message]] Assistant request paused because the chat view is not attached. Reopen the Assistant tab and try again.", false));
            return;
        }
        final int requestRevision = viewModel.currentRevision();
        final String chatIdForRequest = currentChatId;
        ChatMessage loadingMsg = new ChatMessage(" ", false);
        viewModel.addMessage(loadingMsg);
        viewModel.beginStreaming();
        lastRenderedStreamingText = "";
        if (binding != null) mainHandler.post(streamUiUpdater);

        List<ChatMessage> liveMessages = viewModel.getMessages().getValue();
        List<ChatMessage> snapshot = new ArrayList<>(liveMessages == null ? new ArrayList<>() : liveMessages);
        if (!snapshot.isEmpty()) {
            snapshot.remove(snapshot.size() - 1);
        }
        
        if (!snapshot.isEmpty()) {
            ChatMessage first = snapshot.get(0);
            if (!first.isSent() && first.getContent() != null && first.getContent().startsWith("Hi ")) {
                snapshot.remove(0);
            }
        }

        networkExecutor.execute(() -> {
            try {
                JSONObject data = isPrivateSession(chatIdForRequest) ? new JSONObject() : EncryptedProfileStore.loadProfileData(appContext);
                List<ChatMessage> expandedSnapshot = expandPlaceholders(snapshot, appContext, data);
                String assistantContext = AssistantContextBuilder.buildPlainText(appContext, data, expandedSnapshot);
                
                StringBuilder fullResponseBuilder = new StringBuilder();

                AssistantApiClient.streamAssistantResponse(appContext, expandedSnapshot, assistantContext, new AssistantApiClient.StreamCallback() {
                    @Override
                    public void onChunk(String text) {
                        if (requestRevision != viewModel.currentRevision()) return;
                        fullResponseBuilder.append(text);
                        viewModel.appendStreamingText(text);
                    }

                    @Override
                    public void onDone() {
                        viewModel.postToMain(() -> {
                            if (requestRevision != viewModel.currentRevision()) return;
                            viewModel.finishStreaming();
                            
                            String finalResponse = fullResponseBuilder.toString();
                            
                            List<String> commandsToRun = new ArrayList<>();
                            Pattern p = Pattern.compile("\\[\\[execute:(.*?)\\]\\]", Pattern.DOTALL);
                            Matcher m = p.matcher(finalResponse);
                            while (m.find()) {
                                commandsToRun.add(m.group(1).trim());
                            }
                            finalResponse = m.replaceAll("").trim();

                            if (commandsToRun.isEmpty()) {
                                List<ChatMessage> currentMessages = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
                                if (currentMessages != null && !currentMessages.isEmpty()) {
                                    currentMessages.set(currentMessages.size() - 1, new ChatMessage(finalResponse, false));
                                    viewModel.setMessages(currentMessages);
                                }
                                autoTitleChatIfNeeded(snapshot);
                                saveCurrentChatToEncryptedDiskAsync(appContext);
                                return;
                            }

                            pauseForAssistantCommandApproval(commandsToRun, finalResponse, requestRevision, appContext, snapshot);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        viewModel.postToMain(() -> {
                            if (requestRevision != viewModel.currentRevision()) return;
                            viewModel.finishStreaming();
                            List<ChatMessage> currentMessages = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
                            if (currentMessages != null && !currentMessages.isEmpty()) {
                                currentMessages.set(currentMessages.size() - 1, new ChatMessage("[[system-message]] Connection failed. Check your API key, model, and network.\n\n" + safeFailureLine(error), false));
                                viewModel.setMessages(currentMessages);
                                saveCurrentChatToEncryptedDiskAsync(appContext);
                            }
                        });
                    }
                });
                
            } catch (Exception e) {
                viewModel.postToMain(() -> {
                    if (requestRevision != viewModel.currentRevision()) return;
                    viewModel.finishStreaming();
                    List<ChatMessage> currentMessages = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
                    if (currentMessages != null && !currentMessages.isEmpty()) {
                        currentMessages.set(currentMessages.size() - 1, new ChatMessage("[[system-message]] Error starting AI request.\n\n" + safeFailureLine(e.getMessage()), false));
                        viewModel.setMessages(currentMessages);
                        saveCurrentChatToEncryptedDiskAsync(appContext);
                    }
                });
            }
        });
    }
    
    private void autoTitleChatIfNeeded(List<ChatMessage> snapshot) {
        JSONObject session = findSession(currentChatId);
        if (session != null && "New chat".equals(session.optString("title"))) {
            for (int i = snapshot.size() - 1; i >= 0; i--) {
                if (snapshot.get(i).isSent()) {
                    String firstMessage = snapshot.get(i).getContent();
                    String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
                    try {
                        session.put("title", title);
                        renderChatTabs();
                    } catch (JSONException ignored) {}
                    break;
                }
            }
        }
    }

    private String handleCommand(String rawCommand, boolean includeInContext) {
        return commandRegistry.handleCommand(rawCommand);
    }

    private void insertCommandIntoInput(String command) {
        if (binding == null) return;
        binding.editMessage.setText(command);
        binding.editMessage.setSelection(binding.editMessage.getText().length());
        binding.editMessage.requestFocus();
        android.widget.Toast.makeText(requireContext(), R.string.assistant_command_inserted, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void confirmAssistantCommand(String command, boolean includeInContext) {
        if (binding == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assistant_auto_command_title)
                .setMessage(getString(R.string.assistant_auto_command_body, command))
                .setNegativeButton(R.string.assistant_auto_commands_decline, null)
                .setPositiveButton(R.string.assistant_auto_commands_accept, (dialog, which) -> runConfirmedAssistantCommand(command, includeInContext))
                .setNeutralButton(R.string.always_run, (dialog, which) -> {
                    alwaysRunAssistantCommands = true;
                    runConfirmedAssistantCommand(command, includeInContext);
                })
                .show();
    }

    private void runConfirmedAssistantCommand(String command, boolean includeInContext) {
        if (isAsyncCommand(command)) {
            runAsyncCommand(command, includeInContext, true);
        } else {
            String response = handleCommand(command, includeInContext);
            viewModel.addMessage(new ChatMessage(formatCommandReply(response, includeInContext), includeInContext));
            saveCurrentChatToEncryptedDiskAsync();
            if (includeInContext) {
                requestOpenAiResponse();
            }
        }
    }

    private boolean isAsyncCommand(String command) {
        return commandRegistry.isAsyncCommand(command);
    }

    private void runAsyncCommand(String command, boolean includeInContext, boolean isAiTriggered) {
        Context context = getContext();
        Context appContext = context == null ? viewModel.getApplicationContext() : context.getApplicationContext();
        if (appContext == null) {
            viewModel.addMessage(new ChatMessage("[[system-message]] Command paused because the assistant view is not attached. Reopen the Assistant tab and retry " + commandName(command) + ".", false));
            return;
        }
        final int requestRevision = viewModel.currentRevision();
        String safeName = commandName(command);
        String initialMsg = "[[system-message]] Running " + safeName + "...";
        ChatMessage loadingMsg = new ChatMessage(initialMsg, false);
        viewModel.addMessage(loadingMsg);
        final String loadingMsgId = loadingMsg.getId();
        
        networkExecutor.execute(() -> {
            String response;
            boolean failed = false;
            try {
                response = commandRegistry.handleAsyncCommand(command, update -> {
                    mainHandler.post(() -> {
                        if (requestRevision != viewModel.currentRevision()) return;
                        viewModel.updateMessage(loadingMsgId, "[[system-message]] " + update);
                    });
                });
            } catch (Exception e) {
                response = commandFailureMessage(command, e);
                failed = true;
            }
            
            String finalResponse = response;
            boolean isFailed = failed;
            
            viewModel.postToMain(() -> {
                if (requestRevision != viewModel.currentRevision()) return;
                viewModel.deleteMessage(loadingMsgId);
                
                if (isFailed) {
                    viewModel.addMessage(new ChatMessage("[[system-message]] " + finalResponse, false));
                } else {
                    viewModel.addMessage(new ChatMessage(formatCommandReply(finalResponse, includeInContext), includeInContext));
                }
                
                saveCurrentChatToEncryptedDiskAsync(appContext);
                
                if ((isAiTriggered && includeInContext) || (isFailed && includeInContext)) {
                    requestOpenAiResponse();
                }
            });
        });
    }

    private void pauseForAssistantCommandApproval(List<String> rawCommands, String assistantDraft, int requestRevision, Context appContext, List<ChatMessage> snapshot) {
        List<String> commands = new ArrayList<>();
        for (String raw : rawCommands) {
            String normalized = AssistantCommandParser.normalize(raw);
            if (!normalized.isEmpty()) commands.add(normalized);
        }
        if (commands.isEmpty()) return;

        List<ChatMessage> current = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
        if (!current.isEmpty()) {
            current.set(current.size() - 1, new ChatMessage("[[system-message]] " + getString(R.string.assistant_auto_commands_paused), false));
            viewModel.setMessages(current);
            saveCurrentChatToEncryptedDiskAsync(appContext);
        }

        if (alwaysRunAssistantCommands) {
            executeVerifiedAssistantCommands(commands, assistantDraft, requestRevision, appContext, snapshot);
            return;
        }
        if (!isAdded() || getContext() == null) {
            updatePausedReplyWithDecline(assistantDraft, appContext);
            return;
        }

        String commandList = buildCommandList(commands);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assistant_auto_commands_title)
                .setMessage(getString(R.string.assistant_auto_commands_body, commandList))
                .setNegativeButton(R.string.assistant_auto_commands_decline, (dialog, which) -> updatePausedReplyWithDecline(assistantDraft, appContext))
                .setPositiveButton(R.string.assistant_auto_commands_accept, (dialog, which) -> executeVerifiedAssistantCommands(commands, assistantDraft, requestRevision, appContext, snapshot))
                .setNeutralButton(R.string.always_run, (dialog, which) -> {
                    alwaysRunAssistantCommands = true;
                    executeVerifiedAssistantCommands(commands, assistantDraft, requestRevision, appContext, snapshot);
                })
                .show();
    }

    private void updatePausedReplyWithDecline(String assistantDraft, Context appContext) {
        List<ChatMessage> current = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
        if (current.isEmpty()) return;
        StringBuilder text = new StringBuilder();
        if (assistantDraft != null && !assistantDraft.trim().isEmpty()) {
            text.append(assistantDraft.trim()).append("\n\n");
        }
        text.append(getString(R.string.assistant_auto_commands_declined));
        current.set(current.size() - 1, new ChatMessage(text.toString(), false));
        viewModel.setMessages(current);
        saveCurrentChatToEncryptedDiskAsync(appContext);
    }

    private String buildCommandList(List<String> commands) {
        StringBuilder out = new StringBuilder();
        int count = Math.min(commands.size(), 8);
        for (int i = 0; i < count; i++) {
            out.append("- ").append(shortenCommandPreview(commands.get(i))).append("\n");
        }
        if (commands.size() > count) out.append("- ... +").append(commands.size() - count).append(" more");
        return out.toString().trim();
    }

    private String shortenCommandPreview(String command) {
        if (command == null) return "";
        String singleLine = command.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (singleLine.length() > 160) return singleLine.substring(0, 160) + "...";
        return singleLine;
    }

    private void executeVerifiedAssistantCommands(List<String> approvedCommands, String assistantDraft, int requestRevision, Context appContext, List<ChatMessage> snapshot) {
        if (approvedCommands == null || approvedCommands.isEmpty()) return;
        List<String> commands = new ArrayList<>(approvedCommands);

        List<ChatMessage> current = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
        if (!current.isEmpty()) {
            current.set(current.size() - 1, new ChatMessage("[[system-message]] " + getString(R.string.assistant_auto_commands_running), false));
            viewModel.setMessages(current);
        }

        networkExecutor.execute(() -> {
            List<ExecutedCommand> executed = new ArrayList<>();
            int maxCommands = Math.min(6, commands.size());
            for (int i = 0; i < maxCommands; i++) {
                if (requestRevision != viewModel.currentRevision()) return;
                String command = commands.get(i);
                boolean async = isAsyncCommand(command);
                try {
                    String output;
                    if (async) {
                        output = commandRegistry.handleAsyncCommand(command, update -> viewModel.postToMain(() -> {
                            if (requestRevision != viewModel.currentRevision()) return;
                            List<ChatMessage> statusMessages = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
                            if (!statusMessages.isEmpty()) {
                                statusMessages.set(statusMessages.size() - 1, new ChatMessage("[[system-message]] " + update, false));
                                viewModel.setMessages(statusMessages);
                            }
                        }));
                    } else {
                        output = executeSyncCommandSafely(command);
                    }
                    executed.add(new ExecutedCommand(command, output, false));
                } catch (Exception e) {
                    executed.add(new ExecutedCommand(command, commandFailureMessage(command, e), true));
                }
            }

            viewModel.postToMain(() -> {
                if (requestRevision != viewModel.currentRevision()) return;
                List<ChatMessage> done = new ArrayList<>(viewModel.getMessages().getValue() == null ? new ArrayList<>() : viewModel.getMessages().getValue());
                if (!done.isEmpty()) {
                    done.set(done.size() - 1, new ChatMessage(buildOutcomeReply(assistantDraft, executed, commands.size()), false));
                    viewModel.setMessages(done);
                }
                autoTitleChatIfNeeded(snapshot);
                saveCurrentChatToEncryptedDiskAsync(appContext);
            });
        });
    }

    private String executeSyncCommandSafely(String command) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return commandRegistry.handleCommand(command);
        }
        AtomicReference<String> output = new AtomicReference<>("");
        AtomicReference<Exception> error = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);
        mainHandler.post(() -> {
            try {
                output.set(commandRegistry.handleCommand(command));
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for command on main thread.");
        }
        if (error.get() != null) throw error.get();
        String result = output.get();
        return result == null ? "" : result;
    }

    private String buildOutcomeReply(String assistantDraft, List<ExecutedCommand> executed, int requestedCount) {
        StringBuilder out = new StringBuilder();
        if (assistantDraft != null && !assistantDraft.trim().isEmpty()) {
            out.append(assistantDraft.trim()).append("\n\n");
        }
        out.append(buildExecutionSummary(executed, requestedCount)).append("\n\n");
        out.append(getString(R.string.assistant_auto_commands_finished)).append("\n");
        for (ExecutedCommand item : executed) {
            out.append("- ").append(item.command).append(": ").append(item.failed ? "failed" : "ok").append("\n");
            if (item.output != null && !item.output.trim().isEmpty()) {
                String singleLine = item.output.replace('\n', ' ').replace('\r', ' ').trim();
                if (singleLine.length() > 160) singleLine = singleLine.substring(0, 160) + "...";
                out.append("  ").append(singleLine).append("\n");
            }
        }
        return out.toString().trim();
    }

    private String buildExecutionSummary(List<ExecutedCommand> executed, int requestedCount) {
        if (executed == null || executed.isEmpty()) {
            return "[[system-message]] Assistant requested actions but none executed.";
        }
        int failed = 0;
        for (ExecutedCommand item : executed) {
            if (item.failed) failed++;
        }
        StringBuilder out = new StringBuilder("Executed and verified ");
        out.append(executed.size()).append(" action");
        if (executed.size() != 1) out.append("s");
        if (requestedCount > executed.size()) {
            out.append(" (").append(requestedCount - executed.size()).append(" skipped due to safety limit)");
        }
        out.append(".\n");
        if (failed > 0) {
            out.append("- ").append(failed).append(" failed\n");
        }
        for (ExecutedCommand item : executed) {
            out.append("- ").append(item.command).append(": ").append(item.failed ? "failed" : "ok").append("\n");
        }
        out.append("\nVerified command outputs were saved below.");
        return out.toString().trim();
    }

    private static final class ExecutedCommand {
        final String command;
        final String output;
        final boolean failed;

        ExecutedCommand(String command, String output, boolean failed) {
            this.command = command == null ? "" : command;
            this.output = output == null ? "No output" : output;
            this.failed = failed;
        }
    }

    private String commandResponse(String response) {
        return ChatAdapter.COMMAND_PREFIX + "\n" + response;
    }

    private String formatCommandReply(String response, boolean includeInContext) {
        return includeInContext ? "Local command result:\n\n" + response : commandResponse(response);
    }

    private String normalizeCommand(String content) {
        return content.startsWith("//") ? "/" + content.substring(2) : content;
    }

    private String commandName(String command) {
        String trimmed = command == null ? "" : command.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private String commandFailureMessage(String command, Exception error) {
        String name = commandName(command);
        StringBuilder out = new StringBuilder();
        out.append("Command failed: ").append(name.isEmpty() ? "/command" : name).append("\n");
        out.append(safeFailureLine(error == null ? "" : error.getMessage())).append("\n\n");
        out.append("What to try next:\n");
        out.append("- Check the spelling or try a generic/brand alias.\n");
        out.append("- Use \"[[command:/alias <name>]]\" for local identity matching.\n");
        out.append("- Use \"[[command:/help ").append(name.startsWith("/") ? name.substring(1) : name).append("]]\" for the exact command format.");
        return out.toString();
    }

    private String safeFailureLine(String error) {
        String clean = error == null ? "" : error.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.isEmpty()) clean = "No detailed error was returned.";
        if (clean.length() > 180) clean = clean.substring(0, 180) + "...";
        return "Reason: " + clean;
    }

    private List<ChatMessage> expandPlaceholders(List<ChatMessage> messages, Context appContext, JSONObject data) {
        List<ChatMessage> expanded = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message == null || message.getContent() == null) continue;
            if (message.getContent().startsWith(ChatAdapter.COMMAND_PREFIX) || message.getContent().startsWith("[[system-message]]")) {
                expanded.add(message);
                continue;
            }
            String content = AssistantPlaceholders.expand(appContext, data, message.getContent());
            expanded.add(new ChatMessage(
                    message.getId(),
                    content,
                    message.isSent(),
                    message.getCreatedAt(),
                    message.getAttachmentName(),
                    message.getAttachmentMimeType(),
                    message.getAttachmentBase64()));
        }
        return expanded;
    }

    private void saveCurrentChatToEncryptedDiskAsync() {
        List<ChatMessage> messages = viewModel.getMessages().getValue();
        if (messages == null) return;
        Context context = getContext();
        if (context == null) return;
        saveCurrentChatToEncryptedDiskAsync(context.getApplicationContext());
    }

    private void saveCurrentChatToEncryptedDiskAsync(Context appContext) {
        if (!AppSettings.state(appContext).assistantMemory) return;
        if (isPrivateSession(currentChatId)) {
            viewModel.setChatState(chatSessions, currentChatId);
            return;
        }
        List<ChatMessage> messages = viewModel.getMessages().getValue();
        if (messages == null) return;
        final List<ChatMessage> messagesToSave = new ArrayList<>(messages);
        final String chatIdToSave = currentChatId;

        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                synchronized (chatLock) {
                    JSONObject session = findSessionLocked(chatIdToSave);
                    if (session == null) {
                        session = createSessionObject("Chat");
                        chatSessions.put(session);
                        currentChatId = session.getString("id");
                    }
                    session.put("updated_at", System.currentTimeMillis());
                    session.put("messages", messagesToJson(messagesToSave));
                    data.put("assistant_chats", persistentSessionsLocked());
                    data.put("assistant_active_chat_id", persistentActiveChatIdLocked());
                    viewModel.setChatState(chatSessions, currentChatId);
                }
                EncryptedProfileStore.saveProfileData(appContext, data);
                scheduleRenderChatTabs();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadChatSessionsFromEncryptedDiskAsync() {
        Context appContext = requireContext().getApplicationContext();
        if (!AppSettings.state(appContext).assistantMemory) {
            createNewChat("General");
            return;
        }
        diskExecutor.execute(() -> {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            JSONArray loadedSessions = data.optJSONArray("assistant_chats");
            if (loadedSessions == null) loadedSessions = new JSONArray();
            String loadedChatId = data.optString("assistant_active_chat_id", "");

            JSONArray finalLoadedSessions = loadedSessions;
            mainHandler.post(() -> {
                if (binding == null) return;
                synchronized (chatLock) {
                    chatSessions = finalLoadedSessions;
                    currentChatId = loadedChatId;
                    if (chatSessions.length() == 0) {
                        createNewChat("General");
                        return;
                    }
                    String requestedChatId = viewModel.consumeRequestedChatId();
                    if (!requestedChatId.isEmpty() && findSessionLocked(requestedChatId) != null) {
                        currentChatId = requestedChatId;
                    }
                    JSONObject active = findSessionLocked(currentChatId);
                    if (active == null) {
                        active = chatSessions.optJSONObject(0);
                        currentChatId = active == null ? "" : active.optString("id", "");
                    }
                    viewModel.setChatState(chatSessions, currentChatId);
                    viewModel.setMessages(messagesFromJson(active == null ? null : active.optJSONArray("messages")));
                }
                renderChatTabs();
            });
        });
    }

    private void showNewChatOptions(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, R.string.new_chat);
        popup.getMenu().add(0, 2, 1, R.string.private_chat);
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: createNewChat(getString(R.string.new_chat), false); return true;
                case 2: createNewChat(getString(R.string.private_chat), true); return true;
            }
            return false;
        });
        popup.show();
    }

    private void createNewChat() {
        createNewChat("New chat", false);
    }

    private void createNewChat(String title) {
        createNewChat(title, false);
    }

    private void createNewChat(String title, boolean privateChat) {
        try {
            viewModel.incrementRevision();
            JSONObject session = createSessionObject(title, privateChat);
            chatSessions.put(session);
            currentChatId = session.getString("id");
            viewModel.setChatState(chatSessions, currentChatId);
            viewModel.setMessages(messagesFromJson(session.getJSONArray("messages")));
            if (!privateChat) saveCurrentChatToEncryptedDiskAsync();
            renderChatTabs();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createSessionObject(String title) throws JSONException {
        return createSessionObject(title, false);
    }

    private JSONObject createSessionObject(String title, boolean privateChat) throws JSONException {
        JSONObject session = new JSONObject();
        session.put("id", "chat_" + System.currentTimeMillis());
        session.put("title", title);
        session.put("private", privateChat);
        session.put("created_at", System.currentTimeMillis());
        session.put("updated_at", System.currentTimeMillis());
        JSONArray messages = new JSONArray();
        String profileName = UserSession.getInstance().getProfileName();
        String greetingName = profileName == null || profileName.trim().isEmpty() ? "there" : profileName.trim();
        String greeting = privateChat
                ? "Hi " + greetingName + ". This is a private chat. It will stay out of encrypted assistant history and disappear when this app session ends."
                : "Hi " + greetingName + ". What can I help with today?\n\nI can answer questions, help think through medications or logs, and use any profile context you have enabled.\n\n[[suggest:Show my medications]] [[suggest:Are there interactions in my saved meds?]]\nTry checking your meds with [[command:/meds]] or ask about a combination with [[command:/interact]].";
        messages.put(new JSONObject().put("content", greeting).put("isSent", false));
        session.put("messages", messages);
        return session;
    }

    private JSONObject findSession(String id) {
        if (id == null) return null;
        synchronized (chatLock) {
            return findSessionLocked(id);
        }
    }

    private JSONObject findSessionLocked(String id) {
        if (id == null) return null;
        for (int i = 0; i < chatSessions.length(); i++) {
            JSONObject session = chatSessions.optJSONObject(i);
            if (session != null && id.equals(session.optString("id"))) return session;
        }
        return null;
    }

    private JSONArray messagesToJson(List<ChatMessage> messages) throws JSONException {
        JSONArray array = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject obj = new JSONObject();
            obj.put("id", msg.getId());
            obj.put("content", msg.getContent());
            obj.put("isSent", msg.isSent());
            obj.put("created_at", msg.getCreatedAt());
            obj.put("attachment_name", msg.getAttachmentName());
            obj.put("attachment_mime_type", msg.getAttachmentMimeType());
            obj.put("attachment_base64", msg.getAttachmentBase64());
            array.put(obj);
        }
        return array;
    }

    private List<ChatMessage> messagesFromJson(JSONArray array) {
        List<ChatMessage> messages = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    messages.add(new ChatMessage(
                            obj.optString("id", ""),
                            obj.optString("content", ""),
                            obj.optBoolean("isSent", false),
                            obj.optLong("created_at", 0),
                            obj.optString("attachment_name", ""),
                            obj.optString("attachment_mime_type", ""),
                            obj.optString("attachment_base64", "")));
                }
            }
        }
        return messages;
    }

    private void showEditMessageDialog(ChatMessage message) {
        if (message == null || !message.isSent() || binding == null || !viewModel.isLastUserMessage(message.getId())) return;
        EditText editText = new EditText(requireContext());
        editText.setMinLines(3);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setText(message.getContent());
        editText.setSelection(editText.getText().length());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assistant_edit_message_title)
                .setView(editText)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String editedContent = editText.getText().toString().trim();
                    if (editedContent.isEmpty()) return;
                    if (!viewModel.isLastUserMessage(message.getId())) {
                        Toast.makeText(requireContext(), R.string.assistant_last_message_only, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.incrementRevision();
                    viewModel.truncateAfter(message.getId());
                    viewModel.updateMessage(message.getId(), editedContent);
                    saveCurrentChatToEncryptedDiskAsync();
                    
                    Context appContext = requireContext().getApplicationContext();
                    JSONObject data = isPrivateSession(currentChatId) ? new JSONObject() : EncryptedProfileStore.loadProfileData(appContext);
                    String expandedContent = AssistantPlaceholders.expand(appContext, data, editedContent);
                    processInput(expandedContent, false, false);
                })
                .show();
    }

    private void showDeleteMessageDialog(ChatMessage message) {
        if (message == null || !message.isSent() || binding == null || !viewModel.isLastUserMessage(message.getId())) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assistant_delete_message_title)
                .setMessage(R.string.assistant_delete_message_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (!viewModel.isLastUserMessage(message.getId())) {
                        Toast.makeText(requireContext(), R.string.assistant_last_message_only, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.incrementRevision();
                    viewModel.deleteLastUserTurn(message.getId());
                    saveCurrentChatToEncryptedDiskAsync();
                })
                .show();
    }

    private void renderChatTabs() {
        mainHandler.removeCallbacks(renderTabsRunnable);
        mainHandler.post(renderTabsRunnable);
    }

    private void scheduleRenderChatTabs() {
        mainHandler.removeCallbacks(renderTabsRunnable);
        mainHandler.postDelayed(renderTabsRunnable, 120);
    }

    private void renderChatTabsNow() {
        if (binding == null) return;
        binding.layoutChatTabs.removeAllViews();
        for (int i = 0; i < chatSessions.length(); i++) {
            JSONObject session = chatSessions.optJSONObject(i);
            if (session == null) continue;
            String id = session.optString("id");
            LinearLayout tab = new LinearLayout(requireContext());
            tab.setOrientation(LinearLayout.HORIZONTAL);
            tab.setGravity(android.view.Gravity.CENTER_VERTICAL);
            tab.setSelected(id.equals(currentChatId));
            tab.setBackgroundResource(R.drawable.assistant_history_chip_bg);
            tab.setPadding(dp(12), 0, dp(2), 0);
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(dp(154), dp(34));
            tabParams.setMargins(0, 0, dp(8), 0);
            tab.setLayoutParams(tabParams);
            tab.setOnClickListener(v -> switchChat(id));

            TextView title = new TextView(requireContext());
            title.setText(session.optBoolean("private", false)
                    ? session.optString("title", getString(R.string.private_chat))
                    : session.optString("title", "Chat"));
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            title.setTextColor(com.google.android.material.color.MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurface));
            title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tab.addView(title);

            android.widget.ImageView close = new android.widget.ImageView(requireContext());
            close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            close.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
            close.setPadding(dp(8), dp(8), dp(8), dp(8));
            close.setOnClickListener(v -> closeChat(id));
            tab.addView(close);

            binding.layoutChatTabs.addView(tab);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void closeChat(String id) {
        viewModel.incrementRevision();
        synchronized (chatLock) {
            JSONArray next = new JSONArray();
            for (int i = 0; i < chatSessions.length(); i++) {
                JSONObject session = chatSessions.optJSONObject(i);
                if (session != null && !id.equals(session.optString("id"))) {
                    next.put(session);
                }
            }
            chatSessions = next;
            if (chatSessions.length() == 0) {
                createNewChat("General");
                return;
            }
            if (id.equals(currentChatId)) {
                JSONObject first = chatSessions.optJSONObject(0);
                currentChatId = first == null ? "" : first.optString("id");
                viewModel.setChatState(chatSessions, currentChatId);
                viewModel.setMessages(messagesFromJson(first == null ? null : first.optJSONArray("messages")));
            }
        }
        renderChatTabs();
        persistChatSessionsAsync();
    }

    private void persistChatSessionsAsync() {
        Context appContext = requireContext().getApplicationContext();
        if (!AppSettings.state(appContext).assistantMemory) return;
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                synchronized (chatLock) {
                    data.put("assistant_chats", persistentSessionsLocked());
                    data.put("assistant_active_chat_id", persistentActiveChatIdLocked());
                    viewModel.setChatState(chatSessions, currentChatId);
                }
                EncryptedProfileStore.saveProfileData(appContext, data);
            } catch (Exception ignored) {}
        });
    }

    private void switchChat(String id) {
        viewModel.incrementRevision();
        synchronized (chatLock) {
            JSONObject session = findSessionLocked(id);
            if (session == null) return;
            currentChatId = id;
            viewModel.setChatState(chatSessions, currentChatId);
            viewModel.setMessages(messagesFromJson(session.optJSONArray("messages")));
        }
        renderChatTabs();
        if (!isPrivateSession(currentChatId)) persistActiveChatIdAsync();
    }

    private void persistActiveChatIdAsync() {
        Context appContext = requireContext().getApplicationContext();
        if (!AppSettings.state(appContext).assistantMemory) return;
        String activeChatId = currentChatId;
        if (isPrivateSession(activeChatId)) return;
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                data.put("assistant_active_chat_id", activeChatId);
                EncryptedProfileStore.saveProfileData(appContext, data);
            } catch (Exception ignored) {}
        });
    }

    private boolean isPrivateSession(String id) {
        JSONObject session = findSession(id);
        return session != null && session.optBoolean("private", false);
    }

    private JSONArray persistentSessionsLocked() {
        JSONArray persistent = new JSONArray();
        for (int i = 0; i < chatSessions.length(); i++) {
            JSONObject session = chatSessions.optJSONObject(i);
            if (session != null && !session.optBoolean("private", false)) {
                persistent.put(session);
            }
        }
        return persistent;
    }

    private String persistentActiveChatIdLocked() {
        JSONObject current = findSessionLocked(currentChatId);
        if (current != null && !current.optBoolean("private", false)) return currentChatId;
        for (int i = 0; i < chatSessions.length(); i++) {
            JSONObject session = chatSessions.optJSONObject(i);
            if (session != null && !session.optBoolean("private", false)) {
                return session.optString("id", "");
            }
        }
        return "";
    }

    private static final class PendingAttachment {
        final String name;
        final String mimeType;
        final String base64;

        PendingAttachment(String name, String mimeType, String base64) {
            this.name = name == null || name.trim().isEmpty() ? "attachment" : name.trim();
            this.mimeType = mimeType == null || mimeType.trim().isEmpty() ? "application/octet-stream" : mimeType.trim();
            this.base64 = base64 == null ? "" : base64;
        }
    }

    private static final class PreparedImage {
        final byte[] bytes;
        final String mimeType;

        PreparedImage(byte[] bytes, String mimeType) {
            this.bytes = bytes == null ? new byte[0] : bytes;
            this.mimeType = mimeType == null || mimeType.trim().isEmpty() ? "image/jpeg" : mimeType.trim();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
