package brettdansmith.drugdiary.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import brettdansmith.drugdiary.domain.service.ServiceLocator;
import brettdansmith.drugdiary.ui.diary.DiaryListViewModel;
import brettdansmith.drugdiary.ui.medications.DoseCalculatorViewModel;
import brettdansmith.drugdiary.ui.medications.MedicationListViewModel;
import brettdansmith.drugdiary.ui.profile.ProfileViewModel;
import brettdansmith.drugdiary.ui.reference.InteractionCheckerViewModel;
import brettdansmith.drugdiary.ui.reference.ReagentChartViewModel;
import brettdansmith.drugdiary.ui.resources.ResourcesViewModel;
import brettdansmith.drugdiary.ui.settings.SettingsViewModel;
import brettdansmith.drugdiary.ui.assistant.AssistantViewModel;

/**
 * Factory for creating ViewModels with ServiceLocator dependency injection.
 *
 * Usage:
 * viewModel = new ViewModelProvider(this, new ViewModelFactory(context))
 *     .get(MedicationListViewModel.class);
 */
public class ViewModelFactory implements ViewModelProvider.Factory {
    private final ServiceLocator serviceLocator;

    public ViewModelFactory(Context context) {
        this.serviceLocator = ServiceLocator.getInstance(context);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MedicationListViewModel.class)) {
            return (T) new MedicationListViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(DiaryListViewModel.class)) {
            return (T) new DiaryListViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(ResourcesViewModel.class)) {
            return (T) new ResourcesViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(InteractionCheckerViewModel.class)) {
            return (T) new InteractionCheckerViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(DoseCalculatorViewModel.class)) {
            return (T) new DoseCalculatorViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(ReagentChartViewModel.class)) {
            return (T) new ReagentChartViewModel(serviceLocator);
        } else if (modelClass.isAssignableFrom(AssistantViewModel.class)) {
            return (T) new AssistantViewModel(serviceLocator);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}

