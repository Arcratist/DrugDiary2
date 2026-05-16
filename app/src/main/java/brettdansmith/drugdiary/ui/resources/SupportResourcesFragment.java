package brettdansmith.drugdiary.ui.resources;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.databinding.FragmentSupportResourcesBinding;
import brettdansmith.drugdiary.domain.model.resources.SupportResourceRegistry;
import brettdansmith.drugdiary.ui.assistant.AssistantIntegration;

/**
 * Local, privacy-preserving support hub.
 *
 * The links and phone actions here open outside the encrypted profile vault and do not include
 * profile data, diary entries, medications, assistant context, or identifiers. Keep this screen
 * useful without turning it into a triage tool: it points users to qualified help and local app
 * utilities, but does not diagnose, rank emergencies, or send private data to third parties.
 */
public class SupportResourcesFragment extends Fragment {
    private static final String URL_988 = "https://988lifeline.org/";
    private static final String URL_SAMHSA = "https://www.samhsa.gov/find-help/helplines/national-helpline";
    private static final String URL_FIND_TREATMENT = "https://findtreatment.gov/";
    private static final String URL_POISON_HELP = "https://poisonhelp.hrsa.gov/";
    private static final String URL_LIFELINE_AU = "https://www.lifeline.org.au/";
    private static final String URL_POISONS_AU = "https://www.healthdirect.gov.au/poisons-information-centre";
    private static final String URL_NIDA = "https://nida.nih.gov/research-topics/commonly-used-drugs-charts";
    private static final String URL_DANCESAFE = "https://dancesafe.org/";
    private static final String URL_EROWID = "https://erowid.org/";
    private static final String URL_PSYCHONAUT = "https://psychonautwiki.org/";

    private FragmentSupportResourcesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSupportResourcesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buildSupportLibrary();
    }

    private void buildSupportLibrary() {
        binding.layoutSupportContainer.removeAllViews();

        addSection(
                getString(R.string.support_immediate_help_title),
                getString(R.string.support_immediate_help_body),
                action(getString(R.string.call_988), () -> dial("988")),
                action(getString(R.string.open_988_lifeline), () -> openUrl(URL_988)),
                action(getString(R.string.call_poison_control_us), () -> dial("18002221222")),
                action(getString(R.string.open_poison_help), () -> openUrl(URL_POISON_HELP)),
                action(getString(R.string.call_lifeline_au), () -> dial("131114")),
                action(getString(R.string.open_lifeline_au), () -> openUrl(URL_LIFELINE_AU)),
                action(getString(R.string.call_poisons_au), () -> dial("131126")),
                action(getString(R.string.open_poisons_au), () -> openUrl(URL_POISONS_AU))
        );

        addSection(
                getString(R.string.support_substance_treatment_title),
                getString(R.string.support_substance_treatment_body),
                action(getString(R.string.open_samhsa_helpline), () -> openUrl(URL_SAMHSA)),
                action(getString(R.string.open_find_treatment), () -> openUrl(URL_FIND_TREATMENT)),
                action(getString(R.string.open_nida_drug_facts), () -> openUrl(URL_NIDA))
        );

        addSection(
                getString(R.string.support_app_tools_title),
                getString(R.string.support_app_tools_body),
                action(getString(R.string.open_interaction_checker), () -> navigate(R.id.interactionCheckerFragment)),
                action(getString(R.string.open_dose_calculator), () -> navigate(R.id.doseCalculatorFragment)),
                action(getString(R.string.open_reagent_charts), () -> navigate(R.id.reagentChartFragment))
        );

        addSection(
                getString(R.string.support_harm_reduction_title),
                getString(R.string.support_harm_reduction_body),
                action(getString(R.string.open_dancesafe), () -> openUrl(URL_DANCESAFE)),
                action(getString(R.string.open_erowid), () -> openUrl(URL_EROWID)),
                action(getString(R.string.open_psychonautwiki), () -> openUrl(URL_PSYCHONAUT))
        );

        addSection(getString(R.string.support_urgent_warning_title), getString(R.string.support_urgent_warning_body));
        addSection(getString(R.string.support_safer_use_title), getString(R.string.support_safer_use_body));
        addSection(getString(R.string.support_grounding_title), getString(R.string.support_grounding_body));
        addSection(getString(R.string.support_privacy_boundary_title), getString(R.string.support_privacy_boundary_body));
    }

    private SupportAction action(String label, Runnable runnable) {
        return new SupportAction(label, runnable);
    }

    private void addSection(String title, String body, SupportAction... actions) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setRadius(dp(10));
        card.setCardElevation(0f);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.light_border));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(16));

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        content.addView(titleView);

        TextView bodyView = new TextView(requireContext());
        bodyView.setText(body);
        bodyView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyParams.setMargins(0, dp(6), 0, actions.length == 0 ? dp(8) : dp(10));
        bodyView.setLayoutParams(bodyParams);
        content.addView(bodyView);

        for (SupportAction action : actions) {
            content.addView(createActionButton(action));
        }

        // Add a horizontal layout for the "Ask AI" button at the bottom right
        LinearLayout bottomBar = new LinearLayout(requireContext());
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.END);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bottomParams.setMargins(0, actions.length > 0 ? dp(6) : 0, 0, 0);
        bottomBar.setLayoutParams(bottomParams);

        MaterialButton askAiButton = new MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonStyle
        );
        askAiButton.setAllCaps(false);
        askAiButton.setText("Ask AI");
        askAiButton.setTextSize(12);
        LinearLayout.LayoutParams askAiParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(36)
        );
        askAiButton.setLayoutParams(askAiParams);
        askAiButton.setOnClickListener(v -> {
            AssistantIntegration.askAbout(SupportResourcesFragment.this, buildSectionPrompt(title, body), false, true);
        });
        askAiButton.setOnLongClickListener(v -> {
            showAskAiPopup(v, title, body);
            return true;
        });

        bottomBar.addView(askAiButton);
        content.addView(bottomBar);

        card.addView(content);
        binding.layoutSupportContainer.addView(card);
    }

    private MaterialButton createActionButton(SupportAction action) {
        MaterialButton button = new MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
        );
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setText(action.label);
        button.setOnClickListener(v -> action.runnable.run());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(4), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void navigate(@IdRes int destinationId) {
        NavHostFragment.findNavController(this).navigate(destinationId);
    }

    private void openUrl(String url) {
        startExternalIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void dial(String phoneNumber) {
        startExternalIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
    }

    private void startExternalIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), R.string.no_app_can_open_resource, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String buildSectionPrompt(String title, String body) {
        return SupportResourceRegistry.buildAssistantSuggestionContext(title + " " + body, null)
                + " Section focus: " + title + ". " + body
                + " Please provide practical, non-judgemental harm-minimisation guidance and next-step options.";
    }

    private void showAskAiPopup(View anchor, String title, String body) {
        ListPopupWindow popup = new ListPopupWindow(requireContext());
        popup.setAnchorView(anchor);
        popup.setModal(true);
        popup.setWidth(dp(210));
        popup.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.bg_assistant_popup));
        popup.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_command_suggestion, new String[]{"Ask in private chat"}));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                AssistantIntegration.askAbout(SupportResourcesFragment.this, buildSectionPrompt(title, body), true, true);
            }
            popup.dismiss();
        });
        popup.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static final class SupportAction {
        private final String label;
        private final Runnable runnable;

        private SupportAction(String label, Runnable runnable) {
            this.label = label;
            this.runnable = runnable;
        }
    }
}
