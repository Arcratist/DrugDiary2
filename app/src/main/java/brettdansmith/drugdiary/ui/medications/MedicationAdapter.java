package brettdansmith.drugdiary.ui.medications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.domain.units.TimeFormatter;
import brettdansmith.drugdiary.model.medication.MedicationRecord;

public class MedicationAdapter extends ListAdapter<MedicationRecord, MedicationAdapter.MedicationViewHolder> {

    private final OnMedicationClickListener clickListener;

    public interface OnMedicationClickListener {
        void onMedicationClick(MedicationRecord medication);
        void onAskAssistant(String medicationName);
        void onLogDose(MedicationRecord medication);
        void onEditMedication(MedicationRecord medication);
        void onDeleteMedication(MedicationRecord medication);
    }

    public MedicationAdapter(OnMedicationClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageForm;
        private final ImageView buttonMore;
        private final TextView textName;
        private final TextView textStrength;
        private final TextView textNextDoseTime;
        private final MaterialButton buttonAskAssistant;
        private final MaterialButton buttonLogDose;

        MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageForm = itemView.findViewById(R.id.image_medication_form);
            buttonMore = itemView.findViewById(R.id.button_more);
            textName = itemView.findViewById(R.id.text_medication_name);
            textStrength = itemView.findViewById(R.id.text_medication_strength);
            textNextDoseTime = itemView.findViewById(R.id.text_next_dose_time);
            buttonAskAssistant = itemView.findViewById(R.id.button_ask_assistant);
            buttonLogDose = itemView.findViewById(R.id.button_log_dose);
        }

        void bind(MedicationRecord med, OnMedicationClickListener listener) {
            if (med == null) return;

            textName.setText(med.favorite ? med.name + " ★" : med.name);
            textStrength.setText(buildSummary(med));

            long nextDoseAt = med.schedule == null ? 0 : med.schedule.nextDoseAt;
            if (!med.active) {
                textNextDoseTime.setText(itemView.getContext().getString(R.string.medication_status_inactive));
            } else if (med.prn) {
                textNextDoseTime.setText(itemView.getContext().getString(R.string.medication_status_prn));
            } else if (nextDoseAt > 0) {
                textNextDoseTime.setText(TimeFormatter.formatDateTime(itemView.getContext(), nextDoseAt));
            } else {
                textNextDoseTime.setText(itemView.getContext().getString(R.string.medication_status_due_anytime));
            }

            imageForm.setImageResource(med.active ? android.R.drawable.presence_online : android.R.drawable.presence_invisible);
            buttonLogDose.setEnabled(med.active);

            itemView.setOnClickListener(v -> listener.onMedicationClick(med));
            buttonAskAssistant.setOnClickListener(v -> listener.onAskAssistant(med.name));
            buttonLogDose.setOnClickListener(v -> listener.onLogDose(med));
            buttonMore.setOnClickListener(v -> showActionMenu(v, med, listener));
        }

        private void showActionMenu(View anchor, MedicationRecord med, OnMedicationClickListener listener) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.inflate(R.menu.menu_medication_item);
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_edit_medication) {
                    listener.onEditMedication(med);
                    return true;
                }
                if (item.getItemId() == R.id.action_delete_medication) {
                    listener.onDeleteMedication(med);
                    return true;
                }
                return false;
            });
            popup.show();
        }

        private String buildSummary(MedicationRecord med) {
            StringBuilder out = new StringBuilder();
            if (!med.strength.isEmpty()) out.append(med.strength).append(" ");
            if (!med.form.isEmpty()) out.append(med.form).append(" • ");
            out.append(med.categoryLabels());
            if (med.inventory != null && med.inventory.quantity > 0) {
                out.append(" • ")
                        .append(trimmedNumber(med.inventory.quantity))
                        .append(" ")
                        .append(med.inventory.unit);
            }
            return out.toString().trim();
        }

        private String trimmedNumber(double value) {
            long whole = Math.round(value);
            return Math.abs(value - whole) < 0.01 ? String.valueOf(whole) : String.format(java.util.Locale.getDefault(), "%.1f", value);
        }
    }

    private static final DiffUtil.ItemCallback<MedicationRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<MedicationRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull MedicationRecord oldItem, @NonNull MedicationRecord newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MedicationRecord oldItem, @NonNull MedicationRecord newItem) {
            return oldItem.updatedAt == newItem.updatedAt
                    && oldItem.favorite == newItem.favorite
                    && oldItem.active == newItem.active
                    && oldItem.notes.equals(newItem.notes)
                    && oldItem.strength.equals(newItem.strength)
                    && oldItem.form.equals(newItem.form)
                    && oldItem.route.equals(newItem.route)
                    && oldItem.category == newItem.category
                    && oldItem.categories.equals(newItem.categories);
        }
    };
}
