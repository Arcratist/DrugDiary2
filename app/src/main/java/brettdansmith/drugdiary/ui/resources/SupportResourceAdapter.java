package brettdansmith.drugdiary.ui.resources;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.domain.model.resources.SupportResource;
import brettdansmith.drugdiary.domain.model.resources.SupportResourceRegistry;

public class SupportResourceAdapter extends RecyclerView.Adapter<SupportResourceAdapter.ItemViewHolder> {

    public interface OnAskAiClickListener {
        void onAskAi(SupportResource resource, boolean privateChat);
    }

    private final OnAskAiClickListener askAiClickListener;
    private List<SupportResource> items = new ArrayList<>();

    public SupportResourceAdapter(OnAskAiClickListener askAiClickListener) {
        this.askAiClickListener = askAiClickListener;
    }

    public void submitList(List<SupportResource> newItems) {
        this.items = newItems == null ? new ArrayList<>() : new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_resource, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        SupportResource resource = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(resource.getName());
        holder.chipRegion.setText(resource.getRegion().label());
        holder.tvClassification.setText(SupportResourceRegistry.classifications(resource));
        holder.tvDescription.setText(resource.getDescription());
        holder.tvAvailability.setText("Availability: " + pretty(resource.getAvailability()));
        holder.tvContact.setText("Contacts: call " + pretty(resource.getPhone()) + " | message " + pretty(resource.getMessageContact()));
        holder.tvNotes.setText("Coverage: " + pretty(resource.getRegionDetails()) + " • " + pretty(resource.getNotes()));

        setupAction(holder.btnCall, resource.getPhone(), () -> dial(context, resource.getPhone()));
        setupAction(holder.btnMessage, resource.getMessageContact(), () -> message(context, resource.getMessageContact()));
        setupAction(holder.btnWebsite, resource.getWebsiteUrl(), () -> openUrl(context, resource.getWebsiteUrl()));

        holder.btnAskAi.setOnClickListener(v -> {
            if (askAiClickListener != null) askAiClickListener.onAskAi(resource, false);
        });
        holder.btnAskAi.setOnLongClickListener(v -> {
            showAskAiPopup(v, resource);
            return true;
        });
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }

    private static String pretty(String value) {
        String cleaned = safe(value);
        return cleaned.isEmpty() ? "Unavailable" : cleaned;
    }

    private void setupAction(Button button, String data, Runnable action) {
        if (data != null && !data.trim().isEmpty()) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(v -> action.run());
        } else {
            button.setVisibility(View.GONE);
        }
    }

    private void openUrl(Context context, String url) {
        startExternalIntent(context, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void dial(Context context, String phoneNumber) {
        String normalized = phoneNumber.replaceAll("[^0-9+]", "");
        startExternalIntent(context, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + normalized)));
    }

    private void message(Context context, String contact) {
        String normalized = contact.replaceAll("[^0-9+]", "");
        startExternalIntent(context, new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + normalized)));
    }

    private void startExternalIntent(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.no_app_can_open_resource, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAskAiPopup(View anchor, SupportResource resource) {
        Context context = anchor.getContext();
        ListPopupWindow popup = new ListPopupWindow(context);
        popup.setAnchorView(anchor);
        popup.setModal(true);
        popup.setWidth(dp(context, 210));
        popup.setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.bg_assistant_popup));

        ArrayAdapter<String> adapter = new PopupOptionAdapter(context);
        popup.setAdapter(adapter);
        popup.setOnItemClickListener((parent, view, position, id) -> {
            if (askAiClickListener != null && position == 0) {
                askAiClickListener.onAskAi(resource, true);
            }
            popup.dismiss();
        });
        popup.show();
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class PopupOptionAdapter extends ArrayAdapter<String> {
        PopupOptionAdapter(Context context) {
            super(context, R.layout.item_command_suggestion, new String[]{"Ask in private chat"});
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view instanceof TextView) {
                ((TextView) view).setCompoundDrawablesRelativeWithIntrinsicBounds(
                        android.R.drawable.presence_invisible, 0, 0, 0);
                ((TextView) view).setCompoundDrawablePadding(20);
            }
            return view;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Chip chipRegion;
        TextView tvClassification;
        TextView tvDescription;
        TextView tvAvailability;
        TextView tvContact;
        TextView tvNotes;
        Button btnCall;
        Button btnMessage;
        Button btnWebsite;
        Button btnAskAi;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            chipRegion = itemView.findViewById(R.id.chip_region);
            tvClassification = itemView.findViewById(R.id.tv_classification);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvAvailability = itemView.findViewById(R.id.tv_availability);
            tvContact = itemView.findViewById(R.id.tv_contact);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            btnCall = itemView.findViewById(R.id.btn_call);
            btnMessage = itemView.findViewById(R.id.btn_message);
            btnWebsite = itemView.findViewById(R.id.btn_website);
            btnAskAi = itemView.findViewById(R.id.btn_ask_ai);
        }
    }
}
