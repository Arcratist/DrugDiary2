package brettdansmith.drugdiary.ui.medications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.reference.DrugReferenceRepository;
import brettdansmith.drugdiary.model.medication.MedicationCategory;
import brettdansmith.drugdiary.model.medication.MedicationRecord;

public final class MedicationOverviewDialog {
    private MedicationOverviewDialog() {
    }

    public static void show(Context context, MedicationRecord medication, ExecutorService executor, Runnable onChanged) {
        if (context == null || medication == null) return;
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_medication_overview, null, false);
        TextView title = content.findViewById(R.id.text_overview_title);
        TextView detail = content.findViewById(R.id.text_overview_detail);
        TextView source = content.findViewById(R.id.text_overview_source);

        title.setText(medication.name);
        detail.setText(buildDetail(medication));
        source.setText(context.getString(R.string.reference_lookup_searching, medication.canonicalName));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(medication.favorite ? R.string.unfavorite : R.string.favorite, (d, which) -> toggleFavourite(context, medication, onChanged))
                .setPositiveButton(medication.active ? R.string.archive : R.string.activate, (d, which) -> toggleActive(context, medication, onChanged))
                .create();
        dialog.show();

        executor.execute(() -> {
            try {
                DrugReferenceRepository repository = new DrugReferenceRepository(context);
                String summary = repository.summary(repository.lookupAll(medication.canonicalName));
                dialog.getWindow().getDecorView().post(() -> source.setText(summary));
            } catch (Exception e) {
                dialog.getWindow().getDecorView().post(() -> source.setText(context.getString(R.string.reference_lookup_failed, e.getMessage())));
            }
        });
    }

    private static String buildDetail(MedicationRecord medication) {
        StringBuilder out = new StringBuilder();
        out.append("Groups: ").append(medication.categoryLabels()).append('\n');
        out.append("Strength: ").append(medication.strength.isEmpty() ? "-" : medication.strength).append('\n');
        out.append("Form: ").append(medication.form.isEmpty() ? "-" : medication.form).append('\n');
        out.append("Route: ").append(medication.route.isEmpty() ? "-" : medication.route).append('\n');
        out.append("PRN: ").append(medication.prn ? "Yes" : "No").append('\n');
        out.append("Inventory: ")
                .append(medication.inventory == null ? "-" : String.format(java.util.Locale.getDefault(), "%.1f %s", medication.inventory.quantity, medication.inventory.unit))
                .append('\n');
        if (!medication.notes.isEmpty()) out.append("Notes: ").append(medication.notes).append('\n');
        return out.toString().trim();
    }

    private static void toggleFavourite(Context context, MedicationRecord medication, Runnable onChanged) {
        try {
            new MedicationRepository(context).setFavorite(medication.id, !medication.favorite);
            if (onChanged != null) onChanged.run();
        } catch (Exception e) {
            Toast.makeText(context, R.string.medication_favourite_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static void toggleActive(Context context, MedicationRecord medication, Runnable onChanged) {
        try {
            MedicationRecord updated = new MedicationRecord(
                    medication.id,
                    medication.name,
                    medication.canonicalName,
                    medication.strength,
                    medication.form,
                    medication.route,
                    medication.category,
                    updatedCategories(medication, medication.active ? MedicationCategory.ARCHIVED : MedicationCategory.ACTIVE),
                    !medication.active,
                    medication.favorite,
                    medication.saved,
                    medication.prn,
                    medication.notes,
                    medication.schedule,
                    medication.inventory,
                    medication.aliases,
                    medication.createdAt,
                    System.currentTimeMillis());
            new MedicationRepository(context).upsertRecord(updated);
            if (onChanged != null) onChanged.run();
        } catch (Exception e) {
            Toast.makeText(context, R.string.medication_update_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static LinkedHashSet<MedicationCategory> updatedCategories(MedicationRecord medication, MedicationCategory toggleTarget) {
        LinkedHashSet<MedicationCategory> categories = new LinkedHashSet<>(medication.categories);
        if (toggleTarget == MedicationCategory.ARCHIVED) {
            categories.add(MedicationCategory.ARCHIVED);
            categories.remove(MedicationCategory.ACTIVE);
        } else if (toggleTarget == MedicationCategory.ACTIVE) {
            categories.remove(MedicationCategory.ARCHIVED);
            categories.add(MedicationCategory.ACTIVE);
        }
        return categories;
    }
}

