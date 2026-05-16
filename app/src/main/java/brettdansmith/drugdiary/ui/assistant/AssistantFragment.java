package brettdansmith.drugdiary.ui.assistant;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListPopupWindow;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.File;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.databinding.FragmentAssistantBinding;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;
import brettdansmith.drugdiary.app.MainActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class AssistantFragment extends Fragment {
    private static final int MAX_ATTACHMENT_BYTES = 8 * 1024 * 1024;
    private FragmentAssistantBinding binding;
    private ChatAdapter chatAdapter;
    private SessionAdapter sessionAdapter;
    private AssistantViewModel viewModel;
    private ListPopupWindow commandPopup;

    private ActivityResultLauncher<String> imagePicker;
    private ActivityResultLauncher<String> filePicker;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri pendingCameraUri;
    private final List<ChatMessage.PendingAttachment> pendingAttachments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity(), new ViewModelFactory(requireContext()))
                .get(AssistantViewModel.class);
        viewModel.setApplicationContext(requireContext());

        setupChatRecyclerView();
        setupSidebar();
        setupToolbar();
        setupInputHandlers();
        observeViewModel();
        
        viewModel.loadOrInitializeChatState();

        // Handle initial prompt from "Ask Assistant"
        String prompt = viewModel.consumeInitialPrompt();
        boolean startPrivateChat = false;
        boolean autoSendPrompt = true;
        if ((prompt == null || prompt.trim().isEmpty()) && getArguments() != null) {
            prompt = getArguments().getString(AssistantIntegration.ARG_INITIAL_PROMPT, "");
        }
        if (getArguments() != null) {
            startPrivateChat = getArguments().getBoolean(AssistantIntegration.ARG_START_PRIVATE_CHAT, false);
            autoSendPrompt = getArguments().getBoolean(AssistantIntegration.ARG_AUTO_SEND_PROMPT, true);
        }
        // If prompt still empty, try consuming pending prompt from the hosting activity.
        if ((prompt == null || prompt.trim().isEmpty()) && requireActivity() instanceof MainActivity) {
            String pending = ((MainActivity) requireActivity()).consumePendingAssistantPrompt();
            if (pending != null && !pending.isEmpty()) prompt = pending;
        }

        if (prompt != null && !prompt.isEmpty()) {
            if (startPrivateChat) {
                viewModel.createNewChat(true);
            }
            binding.editMessage.setText(prompt);
            if (autoSendPrompt) {
                sendMessage();
            } else {
                binding.editMessage.setSelection(binding.editMessage.getText().length());
                binding.editMessage.requestFocus();
            }
            if (getArguments() != null) {
                getArguments().remove(AssistantIntegration.ARG_INITIAL_PROMPT);
                getArguments().remove(AssistantIntegration.ARG_START_PRIVATE_CHAT);
                getArguments().remove(AssistantIntegration.ARG_AUTO_SEND_PROMPT);
            }
        }
    }

    private void setupChatRecyclerView() {
        chatAdapter = new ChatAdapter(new ArrayList<>());
        chatAdapter.setOnCommandClickListener(command -> {
            if (binding == null || command == null || command.trim().isEmpty()) return;
            binding.editMessage.setText(command + " ");
            binding.editMessage.setSelection(binding.editMessage.getText().length());
            binding.editMessage.requestFocus();
            Toast.makeText(requireContext(), R.string.assistant_command_inserted, Toast.LENGTH_SHORT).show();
        });
        chatAdapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onEdit(ChatMessage message) {
                if (message != null) chatAdapter.setEditingMessageId(message.getId());
            }

            @Override
            public void onDelete(ChatMessage message) {
                viewModel.deleteMessage(message.getId());
            }

            @Override
            public void onImageClick(String base64) {
                Bundle args = new Bundle();
                args.putString(AttachmentViewerFragment.ARG_BASE64, base64);
                NavHostFragment.findNavController(AssistantFragment.this)
                        .navigate(R.id.attachmentViewerFragment, args);
            }

            @Override
            public void onInlineEditPrivateBranch(ChatMessage message, String updatedText, List<ChatMessage.Attachment> attachments) {
                if (message == null) return;
                viewModel.editMessageAndReplay(message.getId(), updatedText, true, true, attachments);
            }

            @Override
            public void onInlineEditUpdate(ChatMessage message, String updatedText, List<ChatMessage.Attachment> attachments) {
                if (message == null) return;
                viewModel.editMessageAndReplay(message.getId(), updatedText, false, false, attachments);
            }

            @Override
            public void onInlineEditBranch(ChatMessage message, String updatedText, List<ChatMessage.Attachment> attachments) {
                if (message == null) return;
                viewModel.editMessageAndReplay(message.getId(), updatedText, true, false, attachments);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.recyclerChat.setLayoutManager(layoutManager);
        binding.recyclerChat.setAdapter(chatAdapter);
    }

    private void setupSidebar() {
        sessionAdapter = new SessionAdapter(new SessionAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(String sessionId) {
                viewModel.switchChat(sessionId);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onSessionDelete(String sessionId) {
                viewModel.deleteChat(sessionId);
            }
        });

        binding.recyclerSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSessions.setAdapter(sessionAdapter);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.buttonNewChat.setOnClickListener(this::showNewChatMenu);
    }

    private void showNewChatMenu(View v) {
        ListPopupWindow popup = new ListPopupWindow(requireContext());
        popup.setAnchorView(v);
        popup.setModal(true);
        popup.setWidth(dp(180)); // Explicit width
        popup.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.bg_assistant_popup));
        
        List<PopupOption> options = new ArrayList<>();
        options.add(new PopupOption(getString(R.string.new_chat), android.R.drawable.stat_notify_chat));
        options.add(new PopupOption(getString(R.string.private_chat), android.R.drawable.presence_invisible));
        ArrayAdapter<PopupOption> adapter = new PopupOptionAdapter(requireContext(), options);
        popup.setAdapter(adapter);
        
        popup.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) viewModel.createNewChat(false);
            else if (position == 1) viewModel.createNewChat(true);
            popup.dismiss();
        });
        popup.show();
    }

    private void setupInputHandlers() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (chatAdapter != null && chatAdapter.cancelEditingIfActive()) {
                    return;
                }
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });

        binding.buttonSend.setOnClickListener(v -> sendMessage());
        binding.buttonAddAttachment.setOnClickListener(this::showAttachmentMenu);
        binding.buttonRemoveAttachment.setOnClickListener(v -> clearPendingAttachment());
        
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) loadAttachmentFromUri(uri);
        });

        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) loadAttachmentFromUri(uri);
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
                loadAttachmentFromCameraUri(pendingCameraUri);
            }
            pendingCameraUri = null;
        });

        setupAutocomplete();
    }

    private void showAttachmentMenu(View v) {
        ListPopupWindow popup = new ListPopupWindow(requireContext());
        popup.setAnchorView(v);
        popup.setModal(true);
        popup.setWidth(dp(160)); // Explicit width
        popup.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.bg_assistant_popup));
        
        List<PopupOption> options = new ArrayList<>();
        options.add(new PopupOption("Camera", android.R.drawable.ic_menu_camera));
        options.add(new PopupOption("Image", android.R.drawable.ic_menu_gallery));
        options.add(new PopupOption("File", android.R.drawable.ic_menu_agenda));
        ArrayAdapter<PopupOption> adapter = new PopupOptionAdapter(requireContext(), options);
        popup.setAdapter(adapter);
        
        popup.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) launchCameraCapture();
            else if (position == 1) imagePicker.launch("image/*");
            else if (position == 2) filePicker.launch("*/*");
            popup.dismiss();
        });
        popup.show();
    }

    private void setupAutocomplete() {
        commandPopup = new ListPopupWindow(requireContext());
        commandPopup.setAnchorView(binding.layoutComposerContainer);
        commandPopup.setModal(false);
        
        // Match app's card rounding (12dp) via drawable
        commandPopup.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.bg_assistant_popup));
        
        List<AssistantViewModel.CommandAutocompleteOption> commands = new ArrayList<>();
        for (String command : viewModel.getAvailableCommands()) {
            commands.add(new AssistantViewModel.CommandAutocompleteOption(command, command + " "));
        }
        CommandSuggestionAdapter adapter = new CommandSuggestionAdapter(requireContext(), commands);
        commandPopup.setAdapter(adapter);
        
        // Match composer container width exactly and add padding logic
        binding.layoutComposerContainer.post(() -> {
            if (binding != null) {
                // Subtract 24dp to match side padding (12dp each side)
                int width = binding.layoutComposerContainer.getWidth() - dp(24);
                commandPopup.setWidth(width);
                commandPopup.setHorizontalOffset(dp(12));
            }
        });
        
        // Show ABOVE the chat box
        commandPopup.setDropDownGravity(android.view.Gravity.TOP);
        // Vertical offset to push it above the composer container
        commandPopup.setVerticalOffset(dp(-4));
        
        commandPopup.setOnItemClickListener((parent, view, position, id) -> {
            AssistantViewModel.CommandAutocompleteOption option = (AssistantViewModel.CommandAutocompleteOption) parent.getItemAtPosition(position);
            binding.editMessage.setText(option.insertText);
            binding.editMessage.setSelection(binding.editMessage.getText().length());
            commandPopup.dismiss();
        });

        binding.editMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s == null ? "" : s.toString();
                String trimmed = text.trim();
                if (trimmed.startsWith("/")) {
                    List<AssistantViewModel.CommandAutocompleteOption> options = viewModel.getCommandAutocompleteOptions(trimmed);
                    adapter.replaceAll(options);
                    if (!options.isEmpty()) {
                        if (!commandPopup.isShowing()) commandPopup.show();
                    } else {
                        commandPopup.dismiss();
                    }
                } else {
                    commandPopup.dismiss();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getActiveChatTitle().observe(getViewLifecycleOwner(), title -> {
            binding.textChatTitle.setText(title);
            binding.editMessage.setText("");
            binding.editMessage.clearFocus();
            chatAdapter.setEditingMessageId("");
        });

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            boolean isEmpty = messages == null || messages.isEmpty();
            binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerChat.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            
            chatAdapter.submitMessages(messages);
            if (!isEmpty) {
                binding.recyclerChat.post(() -> binding.recyclerChat.scrollToPosition(messages.size() - 1));
            }
        });

        viewModel.getChatSessions().observe(getViewLifecycleOwner(), sessions -> {
            sessionAdapter.submitSessions(sessions, viewModel.getActiveChatId().getValue());
        });

        viewModel.isStreaming().observe(getViewLifecycleOwner(), isStreaming -> {
            chatAdapter.setStreaming(isStreaming);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void sendMessage() {
        String text = binding.editMessage.getText().toString().trim();
        if (text.isEmpty() && pendingAttachments.isEmpty()) return;

        viewModel.sendMessage(text, new ArrayList<>(pendingAttachments));
        binding.editMessage.setText("");
        clearPendingAttachment();
    }

    private void loadAttachmentFromUri(Uri uri) {
        try {
            Context context = requireContext();
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_ATTACHMENT_BYTES) {
                    is.close();
                    Toast.makeText(context, "Attachment too large", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            is.close();
            byte[] bytes = out.toByteArray();

            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null || mimeType.trim().isEmpty()) mimeType = "application/octet-stream";
            String name = resolveAttachmentName(uri);
            if (name == null || name.trim().isEmpty()) name = "Attachment";

            pendingAttachments.add(new ChatMessage.PendingAttachment(name, mimeType, base64));
            refreshAttachmentPreview();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to load attachment", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCameraCapture() {
        try {
            File file = new File(requireContext().getCacheDir(), "assistant_camera_" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            pendingCameraUri = null;
            Toast.makeText(requireContext(), "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAttachmentFromCameraUri(Uri uri) {
        try {
            Context context = requireContext();
            byte[] bytes = readAllBytes(uri);
            if (bytes == null || bytes.length == 0) throw new IllegalStateException("Empty camera image");
            if (bytes.length > MAX_ATTACHMENT_BYTES) bytes = compressToSize(bytes, MAX_ATTACHMENT_BYTES);
            if (bytes == null || bytes.length > MAX_ATTACHMENT_BYTES) throw new IllegalStateException("Camera image too large");
            pendingAttachments.add(new ChatMessage.PendingAttachment(
                    "Camera_" + System.currentTimeMillis() + ".jpg",
                    "image/jpeg",
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
            ));
            refreshAttachmentPreview();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to capture camera attachment", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        InputStream is = requireContext().getContentResolver().openInputStream(uri);
        if (is == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) out.write(buffer, 0, read);
        is.close();
        return out.toByteArray();
    }

    private byte[] compressToSize(byte[] source, int maxBytes) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(source, 0, source.length);
            if (bitmap == null) return source;
            int quality = 100;
            byte[] result = source;
            while (quality >= 55) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                result = out.toByteArray();
                if (result.length <= maxBytes) return result;
                quality -= 5;
            }
            return result;
        } catch (Exception ignored) {
            return source;
        }
    }

    private String resolveAttachmentName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) return cursor.getString(nameIndex);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String last = uri.getLastPathSegment();
        return last == null ? "Attachment" : last;
    }

    private void refreshAttachmentPreview() {
        if (pendingAttachments.isEmpty()) {
            binding.layoutAttachmentPreview.setVisibility(View.GONE);
            return;
        }
        binding.layoutAttachmentPreview.setVisibility(View.VISIBLE);
        if (pendingAttachments.size() == 1) {
            binding.textAttachmentPreview.setText(pendingAttachments.get(0).name);
        } else {
            binding.textAttachmentPreview.setText(pendingAttachments.size() + " attachments ready");
        }
    }

    private void clearPendingAttachment() {
        pendingAttachments.clear();
        refreshAttachmentPreview();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static class PopupOption {
        final String label;
        final int iconRes;

        PopupOption(String label, int iconRes) {
            this.label = label;
            this.iconRes = iconRes;
        }
    }

    private class PopupOptionAdapter extends ArrayAdapter<PopupOption> {
        PopupOptionAdapter(Context context, List<PopupOption> options) {
            super(context, R.layout.item_command_suggestion, options);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                PopupOption option = getItem(position);
                if (option != null) {
                    textView.setText(option.label);
                    android.graphics.drawable.Drawable icon = AppCompatResources.getDrawable(getContext(), option.iconRes);
                    if (icon != null) {
                        icon = DrawableCompat.wrap(icon.mutate());
                        DrawableCompat.setTint(icon, ContextCompat.getColor(getContext(), android.R.color.darker_gray));
                        int size = dp(18);
                        icon.setBounds(0, 0, size, size);
                    }
                    textView.setCompoundDrawablesRelative(icon, null, null, null);
                    textView.setCompoundDrawablePadding(dp(10));
                }
            }
            return view;
        }
    }

    private static class CommandSuggestionAdapter extends ArrayAdapter<AssistantViewModel.CommandAutocompleteOption> {
        CommandSuggestionAdapter(Context context, List<AssistantViewModel.CommandAutocompleteOption> options) {
            super(context, R.layout.item_command_suggestion, new ArrayList<>(options));
        }

        void replaceAll(List<AssistantViewModel.CommandAutocompleteOption> options) {
            clear();
            if (options != null) addAll(options);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof TextView) {
                AssistantViewModel.CommandAutocompleteOption option = getItem(position);
                ((TextView) view).setText(option == null || TextUtils.isEmpty(option.label) ? "" : option.label);
            }
            return view;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
