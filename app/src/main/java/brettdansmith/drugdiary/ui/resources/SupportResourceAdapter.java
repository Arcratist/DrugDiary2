package brettdansmith.drugdiary.ui.resources;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.domain.model.resources.SupportResource;

public class SupportResourceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<SupportResource> items = new ArrayList<>();

    public void submitList(List<SupportResource> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == items.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_resource, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            SupportResource resource = items.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            Context context = itemHolder.itemView.getContext();

            itemHolder.tvName.setText(resource.getName());

            if (resource.getRegion() != null && !resource.getRegion().isEmpty()) {
                itemHolder.chipRegion.setText(resource.getRegion());
                itemHolder.chipRegion.setVisibility(View.VISIBLE);
            } else {
                itemHolder.chipRegion.setVisibility(View.GONE);
            }

            if (resource.getCategories() != null && !resource.getCategories().isEmpty()) {
                String categoriesText = resource.getCategories().stream()
                        .map(Enum::name)
                        .map(name -> name.replace("_", " "))
                        .collect(Collectors.joining(", "));
                itemHolder.tvCategory.setText(categoriesText);
                itemHolder.tvCategory.setVisibility(View.VISIBLE);
            } else {
                itemHolder.tvCategory.setVisibility(View.GONE);
            }

            itemHolder.tvDescription.setText(resource.getDescription());

            if (resource.getAvailability() != null && !resource.getAvailability().isEmpty()) {
                itemHolder.tvAvailability.setText("Availability: " + resource.getAvailability());
                itemHolder.tvAvailability.setVisibility(View.VISIBLE);
            } else {
                itemHolder.tvAvailability.setVisibility(View.GONE);
            }

            if (resource.getNotes() != null && !resource.getNotes().isEmpty()) {
                itemHolder.tvNotes.setText(resource.getNotes());
                itemHolder.tvNotes.setVisibility(View.VISIBLE);
            } else {
                itemHolder.tvNotes.setVisibility(View.GONE);
            }

            // Actions
            setupAction(itemHolder.btnPhone, resource.getPhone(), () -> dial(context, resource.getPhone()));
            setupAction(itemHolder.btnSms, resource.getSms(), () -> sms(context, resource.getSms()));
            setupAction(itemHolder.btnWebsite, resource.getWebsiteUrl(), () -> openUrl(context, resource.getWebsiteUrl()));
            setupAction(itemHolder.btnMeeting, resource.getMeetingFinderUrl(), () -> openUrl(context, resource.getMeetingFinderUrl()));
        }
    }

    private void setupAction(Button button, String data, Runnable action) {
        if (data != null && !data.isEmpty()) {
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
        startExternalIntent(context, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber.replaceAll("[^0-9+]", ""))));
    }

    private void sms(Context context, String phoneNumber) {
        startExternalIntent(context, new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber.replaceAll("[^0-9+]", ""))));
    }

    private void startExternalIntent(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.no_app_can_open_resource, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + 1; // +1 for footer
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Chip chipRegion;
        TextView tvCategory;
        TextView tvDescription;
        TextView tvAvailability;
        TextView tvNotes;
        Button btnPhone;
        Button btnSms;
        Button btnWebsite;
        Button btnMeeting;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            chipRegion = itemView.findViewById(R.id.chip_region);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvAvailability = itemView.findViewById(R.id.tv_availability);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            btnPhone = itemView.findViewById(R.id.btn_phone);
            btnSms = itemView.findViewById(R.id.btn_sms);
            btnWebsite = itemView.findViewById(R.id.btn_website);
            btnMeeting = itemView.findViewById(R.id.btn_meeting);
        }
    }
}
