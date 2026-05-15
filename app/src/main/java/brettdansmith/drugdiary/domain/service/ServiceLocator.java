package brettdansmith.drugdiary.domain.service;

import android.content.Context;

import brettdansmith.drugdiary.data.diary.DiaryRepositoryImpl;
import brettdansmith.drugdiary.data.medication.MedicationRepositoryImpl;
import brettdansmith.drugdiary.data.reference.DrugReferenceRepositoryImpl;
import brettdansmith.drugdiary.data.settings.SettingsRepositoryImpl;
import brettdansmith.drugdiary.domain.repository.AiService;
import brettdansmith.drugdiary.domain.repository.DiaryRepository;
import brettdansmith.drugdiary.domain.repository.DrugReferenceRepository;
import brettdansmith.drugdiary.domain.repository.MedicationRepository;
import brettdansmith.drugdiary.domain.repository.SettingsRepository;
import brettdansmith.drugdiary.network.ai.AiServiceImpl;

/**
 * Service locator for dependency injection.
 * Provides centralized access to service instances across the application.
 *
 * This is a simple service locator pattern without external dependency injection
 * frameworks. For small to medium projects where Dagger/Hilt overhead is not needed,
 * this pattern provides clean separation while keeping dependencies manageable.
 */
public final class ServiceLocator {
    private static final Object LOCK = new Object();

    private static ServiceLocator instance;

    private final Context context;
    private final MedicationRepository medicationRepo;
    private final SettingsRepository settingsRepo;
    private final DiaryRepository diaryRepo;
    private final DrugReferenceRepository drugRefRepo;
    private final AiService aiService;

    /**
     * Gets the singleton ServiceLocator instance.
     *
     * @param context application context
     * @return ServiceLocator instance
     */
    public static ServiceLocator getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ServiceLocator(context);
                }
            }
        }
        return instance;
    }

    /**
     * Resets the service locator (useful for testing).
     */
    public static void reset() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    private ServiceLocator(Context context) {
        this.context = context.getApplicationContext();
        // Initialize repositories and services
        this.settingsRepo = new SettingsRepositoryImpl(this.context);
        this.medicationRepo = new MedicationRepositoryImpl(this.context);
        this.diaryRepo = new DiaryRepositoryImpl(this.context);
        this.drugRefRepo = new DrugReferenceRepositoryImpl(this.context);
        this.aiService = new AiServiceImpl(settingsRepo);
    }

    /**
     * Gets the medication repository.
     *
     * @return MedicationRepository instance
     */
    public MedicationRepository getMedicationRepository() {
        return medicationRepo;
    }

    /**
     * Gets the settings repository.
     *
     * @return SettingsRepository instance
     */
    public SettingsRepository getSettingsRepository() {
        return settingsRepo;
    }

    /**
     * Gets the diary repository.
     *
     * @return DiaryRepository instance
     */
    public DiaryRepository getDiaryRepository() {
        return diaryRepo;
    }

    /**
     * Gets the drug reference repository.
     *
     * @return DrugReferenceRepository instance
     */
    public DrugReferenceRepository getDrugReferenceRepository() {
        return drugRefRepo;
    }

    /**
     * Gets the AI service.
     *
     * @return AiService instance
     */
    public AiService getAiService() {
        return aiService;
    }

    /**
     * Gets the application context.
     *
     * @return Context
     */
    public Context getContext() {
        return context;
    }
}

