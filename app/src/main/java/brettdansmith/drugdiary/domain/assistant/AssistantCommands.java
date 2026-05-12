package brettdansmith.drugdiary.domain.assistant;

abstract class NamedCommand implements AssistantCommand {
    private final String name;
    private final boolean async;

    NamedCommand(String name, boolean async) {
        this.name = name;
        this.async = async;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean runsAsync() {
        return async;
    }

    @Override
    public String execute(android.content.Context context, String command, AssistantCommandRegistry.UiActions actions) {
        throw new UnsupportedOperationException("Registered command is routed by AssistantCommandRegistry.");
    }
}

final class HelpCommand extends NamedCommand { HelpCommand() { super("/help", false); } }
final class ContextCommand extends NamedCommand { ContextCommand() { super("/context", false); } }
final class SettingsCommand extends NamedCommand { SettingsCommand() { super("/settings", false); } }
final class PrivacyCommand extends NamedCommand { PrivacyCommand() { super("/privacy", false); } }
final class SourcesCommand extends NamedCommand { SourcesCommand() { super("/sources", false); } }
final class PlaceholdersCommand extends NamedCommand { PlaceholdersCommand() { super("/placeholders", false); } }
final class MedicationsCommand extends NamedCommand { MedicationsCommand() { super("/meds", false); } }
final class AddMedicationCommand extends NamedCommand { AddMedicationCommand() { super("/addmed", false); } }
final class UpdateMedicationCommand extends NamedCommand { UpdateMedicationCommand() { super("/updatemed", false); } }
final class EditMedicationCommand extends NamedCommand { EditMedicationCommand() { super("/editmed", false); } }
final class RemoveMedicationCommand extends NamedCommand { RemoveMedicationCommand() { super("/removemed", false); } }
final class DeleteMedicationCommand extends NamedCommand { DeleteMedicationCommand() { super("/deletemed", false); } }
final class ClearMedicationsCommand extends NamedCommand { ClearMedicationsCommand() { super("/clearmeds", false); } }
final class ImportMedicationsCommand extends NamedCommand { ImportMedicationsCommand() { super("/importmeds", false); } }
final class ReminderCommand extends NamedCommand { ReminderCommand() { super("/reminder", false); } }
final class LogsCommand extends NamedCommand { LogsCommand() { super("/logs", false); } }
final class RemindersCommand extends NamedCommand { RemindersCommand() { super("/reminders", false); } }
final class AliasCommand extends NamedCommand { AliasCommand() { super("/alias", false); } }
final class DoseCommand extends NamedCommand { DoseCommand() { super("/dose", false); } }
final class MedInfoCommand extends NamedCommand { MedInfoCommand() { super("/medinfo", false); } }
final class PubChemCommand extends NamedCommand { PubChemCommand() { super("/pubchem", true); } }
final class RxNormCommand extends NamedCommand { RxNormCommand() { super("/rxnorm", true); } }
final class OpenFdaCommand extends NamedCommand { OpenFdaCommand() { super("/openfda", true); } }
final class ChemblCommand extends NamedCommand { ChemblCommand() { super("/chembl", true); } }
final class DailyMedCommand extends NamedCommand { DailyMedCommand() { super("/dailymed", true); } }
final class WikipediaCommand extends NamedCommand { WikipediaCommand() { super("/wikipedia", true); } }
final class DrugDataCommand extends NamedCommand { DrugDataCommand() { super("/drugdata", true); } }
final class DrugCacheCommand extends NamedCommand { DrugCacheCommand() { super("/drugcache", true); } }
final class NewChatCommand extends NamedCommand { NewChatCommand() { super("/newchat", false); } }

