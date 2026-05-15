package brettdansmith.drugdiary.ui.avatar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import brettdansmith.drugdiary.R;

public final class AvatarIconAdapter extends RecyclerView.Adapter<AvatarIconAdapter.ViewHolder> {
    public interface OnIconClickListener {
        void onIconSelected(@NonNull String iconId);
    }

    private final List<AvatarIconRegistry.AvatarIconOption> options;
    private final OnIconClickListener listener;
    @NonNull private final String initialsPreview;
    private String selectedIconId;

    public AvatarIconAdapter(@NonNull List<AvatarIconRegistry.AvatarIconOption> options,
                             @NonNull String profileName,
                             @NonNull OnIconClickListener listener) {
        this.options = options;
        this.initialsPreview = AvatarInitials.fromName(profileName);
        this.listener = listener;
    }

    public void setSelectedIconId(String selectedIconId) {
        this.selectedIconId = selectedIconId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar_icon_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AvatarIconRegistry.AvatarIconOption option = options.get(position);
        int avatarColor = holder.initialsView.getCurrentTextColor();
        if ("initials".equals(option.id)) {
            holder.iconView.setVisibility(View.GONE);
            holder.initialsView.setVisibility(View.VISIBLE);
            holder.initialsView.setText(initialsPreview);
            holder.initialsView.setContentDescription(holder.itemView.getContext().getString(option.contentDescriptionRes));
        } else {
            holder.initialsView.setVisibility(View.GONE);
            holder.iconView.setVisibility(View.VISIBLE);
            AvatarGlyphDrawable drawable = new AvatarGlyphDrawable(option.id);
            drawable.setMonotoneColor(avatarColor);
            holder.iconView.setImageTintList(null);
            holder.iconView.setImageDrawable(drawable);
            holder.iconView.setContentDescription(holder.itemView.getContext().getString(option.contentDescriptionRes));
        }
        holder.labelView.setText(option.labelRes);

        boolean selected = option.id.equals(selectedIconId);
        holder.cardView.setStrokeWidth(dp(holder.cardView, 2));
        holder.cardView.setStrokeColor(selected
                ? MaterialColors.getColor(holder.cardView, com.google.android.material.R.attr.colorOnSurface)
                : MaterialColors.getColor(holder.cardView, com.google.android.material.R.attr.colorOnSurfaceVariant));
        holder.labelView.setTypeface(holder.labelView.getTypeface(), selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        holder.itemView.setOnClickListener(v -> listener.onIconSelected(option.id));
    }

    private int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final ImageView iconView;
        final TextView initialsView;
        final TextView labelView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_avatar_option);
            iconView = itemView.findViewById(R.id.image_avatar_option_icon);
            initialsView = itemView.findViewById(R.id.text_avatar_option_initials);
            labelView = itemView.findViewById(R.id.text_avatar_option_label);
        }
    }
}
