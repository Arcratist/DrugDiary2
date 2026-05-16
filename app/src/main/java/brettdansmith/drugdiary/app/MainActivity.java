package brettdansmith.drugdiary.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.MenuItemCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.ui.AppBarConfiguration;

import brettdansmith.drugdiary.security.UserSession;
import brettdansmith.drugdiary.data.profile.AvatarType;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;
import brettdansmith.drugdiary.data.profile.ProfileAvatarDataStore;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.ui.assistant.AssistantNotificationController;
import brettdansmith.drugdiary.ui.assistant.AssistantExternalIntentParser;
import brettdansmith.drugdiary.ui.assistant.AssistantIntegration;
import brettdansmith.drugdiary.ui.assistant.NotificationRegistry;
import brettdansmith.drugdiary.ui.assistant.AssistantViewModel;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;
import brettdansmith.drugdiary.ui.avatar.AvatarNavIconFactory;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;
import brettdansmith.drugdiary.ui.profile.ProfileFragment;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.databinding.ActivityMainBinding;

import brettdansmith.drugdiary.R;

public class MainActivity extends AppCompatActivity {
    public static final String ACTION_OPEN_ASSISTANT_CHAT = "brettdansmith.drugdiary.OPEN_ASSISTANT_CHAT";
    public static final String EXTRA_ASSISTANT_CHAT_ID = "assistant_chat_id";
    public static final String ACTION_OPEN_ASSISTANT_COMPOSER = "brettdansmith.drugdiary.OPEN_ASSISTANT_COMPOSER";

    private String pendingAssistantPrompt = "";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private Toast panicToast;
    private int currentDestinationId = 0;
    private long lastBottomNavSelectionAt = 0L;

    public static final String PREFS_NAME = AppSettings.PREFS_NAME;
    private static final String PREF_DONT_SHOW_WELCOME = "dont_show_welcome";
    private static final String PREF_WELCOME_VERSION = "welcome_version";
    public static final String PREF_THEME = AppSettings.PREF_THEME;
    private static boolean welcomeCheckedThisProcess = false;

    private static final int PANIC_BACK_PRESS_THRESHOLD = 3;
    private static final long PANIC_BACK_PRESS_WINDOW_MS = 1800L;
    private int panicBackPressCount = 0;
    private int panicBackDestinationId = 0;
    private long panicBackLastPressedAt = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new SettingsRepository(this).applySavedLocale();
        applyTheme();
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        NotificationRegistry.ensureAllChannels(getApplicationContext());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        setupBottomNavigation(navController);

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.profileFragment, R.id.diaryFragment, R.id.assistantFragment,
                R.id.toolsFragment, R.id.resourcesFragment
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            currentDestinationId = destId;
            invalidateOptionsMenu();
            boolean isLoggedIn = UserSession.getInstance().isActive();
            boolean isAuthScreen = destId == R.id.ProfileSelectorFragment || destId == R.id.CreateProfileFragment || destId == R.id.globalSettingsFragment;

            binding.appBar.setVisibility(View.GONE);

                if (isAuthScreen) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                binding.bottomNav.setVisibility(View.GONE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                if (!isLoggedIn) {
                    safeNavigateToProfileSelector(controller);
                    return;
                }
                binding.bottomNav.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
                if (isLoggedIn) {
                    updateProfileNavIcon();
                    MenuItem bottomItem = binding.bottomNav.getMenu().findItem(destId);
                    if (bottomItem != null && binding.bottomNav.getSelectedItemId() != destId) {
                        binding.bottomNav.setSelectedItemId(destId);
                    }
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavController controller = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
                if (controller.getCurrentDestination() == null) return;

                int destId = controller.getCurrentDestination().getId();

                if (destId == R.id.profileFragment || destId == R.id.ProfileSelectorFragment) {
                    handlePanicBackPress(destId);
                } else {
                    resetPanicBackState();
                    if (!controller.popBackStack()) {
                        controller.navigate(R.id.profileFragment);
                    }
                }
                maybeOpenPendingAssistant(controller);
            }
        });

        if (savedInstanceState == null) {
            checkWelcomeScreen();
        }
        handleAssistantIntent(getIntent(), navController);
    }

    private void setupBottomNavigation(NavController navController) {
        // Disable default tinting to allow per-item custom tinting
        binding.bottomNav.setItemIconTintList(null);
        ColorStateList navTint = AppCompatResources.getColorStateList(this, R.color.nav_item_tint);
        binding.bottomNav.setItemTextColor(navTint);

        int selectedColor = navTint.getColorForState(new int[]{android.R.attr.state_selected},
                navTint.getColorForState(new int[]{android.R.attr.state_checked}, navTint.getDefaultColor()));
        int defaultColor = navTint.getDefaultColor();

        Menu menu = binding.bottomNav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() != R.id.profileFragment) {
                Drawable icon = item.getIcon();
                if (icon != null) {
                    StateListDrawable sld = new StateListDrawable();

                    Drawable selectedIcon = icon.mutate();
                    Drawable wrappedSelected = DrawableCompat.wrap(selectedIcon);
                    DrawableCompat.setTint(wrappedSelected, selectedColor);
                    sld.addState(new int[]{android.R.attr.state_selected}, wrappedSelected);
                    sld.addState(new int[]{android.R.attr.state_checked}, wrappedSelected);

                    Drawable normalIcon = icon.mutate();
                    Drawable wrappedNormal = DrawableCompat.wrap(normalIcon);
                    DrawableCompat.setTint(wrappedNormal, defaultColor);
                    sld.addState(new int[]{}, wrappedNormal);

                    item.setIcon(sld);
                }
            }
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (!UserSession.getInstance().isActive()) return true;
            int itemId = item.getItemId();
            if (currentDestinationId == itemId) return true;

            long now = android.os.SystemClock.elapsedRealtime();
            if (now - lastBottomNavSelectionAt < 120L) return true;
            lastBottomNavSelectionAt = now;

            try {
                return NavigationUI.onNavDestinationSelected(item, navController);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                return true;
            }
        });
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Keep rapid taps from stacking duplicate fragment transactions.
        });
    }

    private void handleAssistantIntent(android.content.Intent intent, NavController navController) {
        if (intent == null) return;
        if (ACTION_OPEN_ASSISTANT_CHAT.equals(intent.getAction())) {
            AssistantViewModel model = new ViewModelProvider(this, new ViewModelFactory(this))
                    .get(AssistantViewModel.class);
            model.setApplicationContext(getApplicationContext());
            model.requestOpenChat(intent.getStringExtra(EXTRA_ASSISTANT_CHAT_ID));
            if (UserSession.getInstance().isActive()
                    && navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() != R.id.assistantFragment) {
                navController.navigate(R.id.assistantFragment);
            }
            return;
        }
        maybeHandleExternalAssistantIntent(intent, navController);
    }

    private void maybeHandleExternalAssistantIntent(android.content.Intent intent, NavController navController) {
        brettdansmith.drugdiary.data.settings.SettingsState settings = new SettingsRepository(getApplicationContext()).getState();
        String action = intent.getAction();
        if ((android.content.Intent.ACTION_SEND.equals(action) || android.content.Intent.ACTION_SEND_MULTIPLE.equals(action))
                && !settings.assistantEntryFromShareEnabled) return;
        if (android.content.Intent.ACTION_PROCESS_TEXT.equals(action) && !settings.assistantEntryFromTextSelectionEnabled) return;

        AssistantExternalIntentParser.ParsedRequest parsed = AssistantExternalIntentParser.parse(intent);
        if (parsed == null || parsed.prompt.isEmpty()) return;

        AssistantViewModel model = new ViewModelProvider(this, new ViewModelFactory(this))
                .get(AssistantViewModel.class);
        model.setApplicationContext(getApplicationContext());
        model.requestOpenChat(intent.getStringExtra(EXTRA_ASSISTANT_CHAT_ID));
        model.setInitialPrompt(parsed.prompt);
        pendingAssistantPrompt = parsed.prompt;
        if (UserSession.getInstance().isActive()) {
            maybeOpenPendingAssistant(navController);
        }
    }

    private void maybeOpenPendingAssistant(NavController navController) {
        if (!UserSession.getInstance().isActive()) return;
        if (pendingAssistantPrompt == null || pendingAssistantPrompt.trim().isEmpty()) return;
        // Avoid placing potentially large prompt text into the navigation bundle (can trigger
        // TransactionTooLargeException on some devices / large shared content). The prompt has
        // already been set on the AssistantViewModel via model.setInitialPrompt(parsed.prompt), so
        // navigate with only the auto-send flag and let the fragment consume the prompt from the
        // view model instead of the navigation arguments.
        Bundle args = new Bundle();
        args.putBoolean(AssistantIntegration.ARG_AUTO_SEND_PROMPT, true);
        if (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.assistantFragment) {
            navController.navigate(R.id.assistantFragment, args);
        }

    }

    /**
     * Called by the assistant fragment to consume a prompt that originated from an external
     * intent (share/process-text). We keep it on the activity rather than passing it through
     * the navigation bundle to avoid TransactionTooLargeException for large shared content.
     */
    public String consumePendingAssistantPrompt() {
        String p = pendingAssistantPrompt == null ? "" : pendingAssistantPrompt;
        pendingAssistantPrompt = "";
        return p;
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAssistantIntent(intent, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AssistantNotificationController.setAppInFocus(true);
    }

    @Override
    protected void onPause() {
        AssistantNotificationController.setAppInFocus(false);
        super.onPause();
    }

    private void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(AppSettings.getTheme(this));
    }

    public void logout() {
        UserSession.getInstance().endSession();
        resetPanicBackState();

        // Return to global theme
        AppCompatDelegate.setDefaultNightMode(AppSettings.getTheme(this));

        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            safeNavigateToProfileSelector(navController);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            // Keep activity alive; destination listener will redirect once nav host is ready.
        }
    }

    private void handlePanicBackPress(int destinationId) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (panicBackDestinationId != destinationId || now - panicBackLastPressedAt > PANIC_BACK_PRESS_WINDOW_MS) {
            panicBackPressCount = 0;
        }

        panicBackDestinationId = destinationId;
        panicBackLastPressedAt = now;
        panicBackPressCount++;
        if (panicToast != null) panicToast.cancel();

        int remaining = PANIC_BACK_PRESS_THRESHOLD - panicBackPressCount;
        if (remaining > 0) {
            int messageRes = destinationId == R.id.ProfileSelectorFragment
                    ? R.string.exit_toast
                    : R.string.panic_logout_toast;
            panicToast = Toast.makeText(this, getString(messageRes, remaining), Toast.LENGTH_SHORT);
            panicToast.show();
            return;
        }

        resetPanicBackState();
        if (destinationId == R.id.ProfileSelectorFragment) {
            finish();
        } else {
            requestProfileSaveThenLogout();
        }
    }

    private void resetPanicBackState() {
        panicBackPressCount = 0;
        panicBackDestinationId = 0;
        panicBackLastPressedAt = 0L;
        if (panicToast != null) {
            panicToast.cancel();
            panicToast = null;
        }
    }

    private void safeNavigateToProfileSelector(@NonNull NavController navController) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.ProfileSelectorFragment) {
            return;
        }
        try {
            boolean popped = navController.popBackStack(R.id.ProfileSelectorFragment, false);
            if (!popped || navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.ProfileSelectorFragment) {
                navController.navigate(R.id.ProfileSelectorFragment);
            }
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            try {
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build();
                navController.navigate(R.id.ProfileSelectorFragment, null, navOptions);
            } catch (IllegalArgumentException | IllegalStateException ignoredAgain) {
                // Ignore final fallback failure to avoid app crash during shutdown paths.
            }
        }
    }

    private void requestProfileSaveThenLogout() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            logout();
            return;
        }
        androidx.fragment.app.Fragment activeFragment = navHostFragment
                .getChildFragmentManager()
                .getPrimaryNavigationFragment();
        if (activeFragment instanceof ProfileFragment) {
            ((ProfileFragment) activeFragment).requestSaveThenLogout();
            return;
        }
        logout();
    }

    public void showSettingsMenu(View anchor, NavController navController) {
        navController.navigate(R.id.userSpecificSettingsFragment);
    }

    private void updateProfileNavIcon() {
        try {
            SharedPreferences prefs = getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
            String profileName = prefs.getString(ProfileSelectorFragment.KEY_LAST_PROFILE, "");
            if (profileName.isEmpty()) {
                return;
            }
            ProfileAvatar avatar = ProfileAvatarDataStore.readFromPrefs(this, profileName);
            applyProfileNavIcon(profileName, avatar);
        } catch (Exception ignored) {
            ProfileAvatar fallback = ProfileAvatar.initials();
            applyProfileNavIcon(getString(R.string.user_name), fallback);
        }
    }

    public void refreshProfileNavIconNow(@NonNull String profileName, @NonNull ProfileAvatar avatar) {
        applyProfileNavIcon(profileName, avatar);
    }

    private void applyProfileNavIcon(@Nullable String profileName, @Nullable ProfileAvatar avatar) {
        ProfileAvatar safeAvatar = avatar == null ? ProfileAvatar.initials() : avatar;
        int iconSize = getResources().getDimensionPixelSize(R.dimen.icon_size_nav);
        Drawable icon = AvatarNavIconFactory.create(
                this,
                profileName,
                safeAvatar,
                iconSize);

        MenuItem profileItem = binding.bottomNav.getMenu().findItem(R.id.profileFragment);
        if (profileItem != null) {
            profileItem.setIcon(icon);
            MenuItemCompat.setIconTintList(profileItem, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                profileItem.setIconTintList(null);
            }
            binding.bottomNav.post(binding.bottomNav::invalidate);
        }
    }

    private void checkWelcomeScreen() {
        if (welcomeCheckedThisProcess) return;
        welcomeCheckedThisProcess = true;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean dontShow = prefs.getBoolean(PREF_DONT_SHOW_WELCOME, false);
        long currentVersion = getVersionCode();
        if (!dontShow || currentVersion > prefs.getLong(PREF_WELCOME_VERSION, -1)) {
            showWelcomeDialog(true);
        }
    }

    private long getVersionCode() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pInfo.getLongVersionCode() : pInfo.versionCode;
        } catch (Exception e) { return -1; }
    }

    public void showWelcomeDialog(boolean isFirstTime) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_welcome, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        CheckBox cb = view.findViewById(R.id.checkbox_dont_show);
        cb.setVisibility(isFirstTime ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.button_continue).setOnClickListener(v -> {
            if (isFirstTime && cb.isChecked()) {
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(PREF_DONT_SHOW_WELCOME, true)
                    .putLong(PREF_WELCOME_VERSION, getVersionCode()).apply();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigateUp() || super.onSupportNavigateUp();
    }
}

