package brettdansmith.drugdiary.ui.medications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.reference.DrugReferenceRepository;
import brettdansmith.drugdiary.domain.medication.MedicationCatalog;
import brettdansmith.drugdiary.domain.model.medication.MedicationCategory;
import brettdansmith.drugdiary.domain.model.medication.MedicationInventory;
import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.domain.model.medication.MedicationSchedule;

final class MedicationEditorDialog {
    private MedicationEditorDialog() {
    }

    interface ChangeCallback {
        void onChanged();
    }

    static void showAdd(Context context, ExecutorService executor, ChangeCallback callback) {
        show(context, executor, null, callback);
    }

    static void showEdit(Context context, ExecutorService executor, MedicationRecord existing, ChangeCallback callback) {
        show(context, executor, existing, callback);
    }

    private static void show(Context context, ExecutorService executor, @Nullable MedicationRecord existing, ChangeCallback callback) {
        if (context == null) return;
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_edit_medication, null, false);

        AutoCompleteTextView nameInput = content.findViewById(R.id.input_medication_name);
        EditText strengthInput = content.findViewById(R.id.input_medication_strength);
        EditText formInput = content.findViewById(R.id.input_medication_form);
        EditText routeInput = content.findViewById(R.id.input_medication_route);
        MultiAutoCompleteTextView categoryInput = content.findViewById(R.id.input_medication_category);
        MaterialSwitch activeSwitch = content.findViewById(R.id.switch_medication_active);
        MaterialSwitch prnSwitch = content.findViewById(R.id.switch_medication_prn);
        MaterialSwitch favoriteSwitch = content.findViewById(R.id.switch_medication_favorite);
        EditText inventoryInput = content.findViewById(R.id.input_inventory_amount);
        EditText notesInput = content.findViewById(R.id.input_medication_notes);
        MaterialButton autofillButton = content.findViewById(R.id.button_medication_autofill);
        TextView autofillStatus = content.findViewById(R.id.text_medication_autofill_status);

        configureCategoryInput(context, categoryInput);
        configureNameAutocomplete(context, nameInput, executor, autofillStatus, strengthInput, formInput, routeInput, categoryInput, notesInput);

        if (existing != null) {
            nameInput.setText(existing.name, false);
            strengthInput.setText(existing.strength);
            formInput.setText(existing.form);
            routeInput.setText(existing.route);
            categoryInput.setText(existing.categoryLabels());
            activeSwitch.setChecked(existing.active);
            prnSwitch.setChecked(existing.prn);
            favoriteSwitch.setChecked(existing.favorite);
            if (existing.inventory != null && existing.inventory.quantity > 0) {
                inventoryInput.setText(trimmedNumber(existing.inventory.quantity));
            }
            notesInput.setText(existing.notes);
        } else {
            categoryInput.setText(MedicationCategory.SAVED.label(), false);
            activeSwitch.setChecked(true);
        }

        autofillButton.setOnClickListener(v -> runAutofill(context, executor, nameInput, strengthInput, formInput, routeInput, categoryInput, notesInput, autofillStatus));

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(existing == null ? R.string.medication_add_entry : R.string.medication_edit_entry)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null);
        if (existing == null) {
            builder.setNeutralButton(R.string.clear_form, (d, w) -> {
                nameInput.setText("");
                strengthInput.setText("");
                formInput.setText("");
                routeInput.setText("");
                categoryInput.setText(MedicationCategory.SAVED.label(), false);
                activeSwitch.setChecked(true);
                prnSwitch.setChecked(false);
                favoriteSwitch.setChecked(false);
                inventoryInput.setText("");
                notesInput.setText("");
            });
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = safeText(nameInput);
            if (name.isEmpty()) {
                nameInput.setError(context.getString(R.string.name_required));
                return;
            }
            try {
                MedicationRecord record = buildRecord(existing, name, safeText(strengthInput), safeText(formInput), safeText(routeInput),
                        safeText(categoryInput), activeSwitch.isChecked(), favoriteSwitch.isChecked(), prnSwitch.isChecked(),
                        safeText(notesInput), parseDouble(safeText(inventoryInput)));
                new MedicationRepository(context).upsertRecord(record);
                if (callback != null) callback.onChanged();
                Toast.makeText(context, R.string.medication_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(context, R.string.medication_save_failed, Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private static void configureNameAutocomplete(Context context,
                                                  AutoCompleteTextView nameInput,
                                                  ExecutorService executor,
                                                  TextView autofillStatus,
                                                  EditText strengthInput,
                                                  EditText formInput,
                                                  EditText routeInput,
                                                  MultiAutoCompleteTextView categoryInput,
                                                  EditText notesInput) {
        List<String> suggestions = MedicationCatalog.nameSuggestions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, suggestions);
        nameInput.setAdapter(adapter);
        nameInput.setThreshold(1);
        nameInput.setOnItemClickListener((parent, view, position, id) -> runAutofill(context, executor, nameInput, strengthInput, formInput, routeInput, categoryInput, notesInput, autofillStatus));
    }

    private static void configureCategoryInput(Context context, MultiAutoCompleteTextView categoryInput) {
        String[] categories = new String[]{
                MedicationCategory.MEDICAL.label(),
                MedicationCategory.RECREATIONAL.label(),
                MedicationCategory.SUPPLEMENT.label(),
                MedicationCategory.HAVE_ACCESS.label(),
                MedicationCategory.WISHLIST.label(),
                MedicationCategory.SAVED.label(),
                MedicationCategory.ACTIVE.label(),
                MedicationCategory.ARCHIVED.label(),
                MedicationCategory.FAVORITE.label()
        };
        categoryInput.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, categories));
        categoryInput.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        categoryInput.setThreshold(1);
    }

    private static void runAutofill(Context context,
                                    ExecutorService executor,
                                    AutoCompleteTextView nameInput,
                                    EditText strengthInput,
                                    EditText formInput,
                                    EditText routeInput,
                                    MultiAutoCompleteTextView categoryInput,
                                    EditText notesInput,
                                    TextView statusText) {
        String query = safeText(nameInput);
        if (query.isEmpty()) {
            nameInput.setError(context.getString(R.string.name_required));
            return;
        }
        statusText.setText(context.getString(R.string.medication_autofill_loading, query));
        executor.execute(() -> {
            try {
                DrugReferenceRepository repository = new DrugReferenceRepository(context);
                MedicationAutofillResolver.Suggestion suggestion = MedicationAutofillResolver.resolve(repository.lookupAll(query), query);
                statusText.post(() -> {
                    if (!suggestion.name.isEmpty()) nameInput.setText(suggestion.name, false);
                    if (!suggestion.strength.isEmpty()) strengthInput.setText(suggestion.strength);
                    if (!suggestion.form.isEmpty()) formInput.setText(suggestion.form);
                    if (!suggestion.route.isEmpty()) routeInput.setText(suggestion.route);
                    if (suggestion.category != null) {
                        LinkedHashSet<MedicationCategory> groups = MedicationCategory.parseMany(safeText(categoryInput));
                        groups.add(suggestion.category);
                        categoryInput.setText(MedicationCategory.labels(groups), false);
                    }
                    String existingNotes = safeText(notesInput);
                    if (existingNotes.isEmpty()) notesInput.setText("Autofill source: " + suggestion.sources);
                    statusText.setText(context.getString(R.string.medication_autofill_done, suggestion.sources));
                });
            } catch (Exception e) {
                statusText.post(() -> statusText.setText(context.getString(R.string.reference_lookup_failed, e.getMessage())));
            }
        });
    }

    private static MedicationRecord buildRecord(@Nullable MedicationRecord existing,
                                                String name,
                                                String strength,
                                                String form,
                                                String route,
                                                String categoriesText,
                                                boolean active,
                                                boolean favorite,
                                                boolean prn,
                                                String notes,
                                                double inventoryAmount) {
        String canonical = MedicationCatalog.canonicalNameFor(name);
        long createdAt = existing == null ? System.currentTimeMillis() : existing.createdAt;
        LinkedHashSet<MedicationCategory> groups = MedicationCategory.parseMany(categoriesText);
        MedicationCategory primary = groups.isEmpty()
                ? (existing == null ? MedicationCategory.SAVED : existing.category)
                : groups.iterator().next();
        if (groups.contains(MedicationCategory.WISHLIST)) active = false;
        return new MedicationRecord(
                existing == null ? "" : existing.id,
                name,
                canonical,
                strength,
                form,
                route,
                primary,
                groups,
                active,
                favorite,
                true,
                prn,
                notes,
                existing == null
                        ? new MedicationSchedule(prn ? "prn" : "scheduled", "", 0)
                        : new MedicationSchedule(prn ? "prn" : "scheduled",
                        existing.schedule == null ? "" : existing.schedule.pattern,
                        existing.schedule == null ? 0 : existing.schedule.nextDoseAt),
                new MedicationInventory(inventoryAmount,
                        existing != null && existing.inventory != null ? existing.inventory.unit : "units",
                        existing != null && existing.inventory != null ? existing.inventory.lowStockThreshold : 0,
                        existing != null && existing.inventory != null ? existing.inventory.expiryAt : 0),
                MedicationCatalog.aliasesFor(name),
                createdAt,
                System.currentTimeMillis());
    }

    private static String safeText(android.widget.TextView view) {
        if (view == null || view.getText() == null) return "";
        return view.getText().toString().trim();
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String trimmedNumber(double value) {
        long whole = Math.round(value);
        return Math.abs(value - whole) < 0.01 ? String.valueOf(whole) : String.format(Locale.getDefault(), "%.1f", value);
    }
}
