# Phase 6: Fragment Migration Guide

## Overview
This phase gradually migrates fragments from direct repository access to the ViewModel pattern, improving code organization and testability.

## Pattern: Fragment + ViewModel + ServiceLocator

### Step 1: Create a ViewModel
```java
public class MyListViewModel extends ViewModel {
    private final MyRepository repository;
    private final MutableLiveData<List<Item>> itemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    
    public MyListViewModel(ServiceLocator serviceLocator) {
        this.repository = serviceLocator.getMyRepository();
    }
    
    public LiveData<List<Item>> getItems() {
        return itemsLiveData;
    }
    
    public void loadItems() {
        loadingLiveData.setValue(true);
        new Thread(() -> {
            try {
                List<Item> items = repository.listAll();
                itemsLiveData.postValue(items);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            } finally {
                loadingLiveData.postValue(false);
            }
        }).start();
    }
}
```

### Step 2: Register ViewModel in ViewModelFactory
```java
public class ViewModelFactory implements ViewModelProvider.Factory {
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MyListViewModel.class)) {
            return (T) new MyListViewModel(serviceLocator);
        }
        // ... other ViewModels
    }
}
```

### Step 3: Use in Fragment
```java
public class MyListFragment extends Fragment {
    private MyListViewModel viewModel;
    private FragmentMyListBinding binding;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Create ViewModel with factory
        viewModel = new ViewModelProvider(this, 
            new ViewModelFactory(requireContext()))
            .get(MyListViewModel.class);
        
        // Observe data
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Load data
        viewModel.loadItems();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

## ViewModels Created in Phase 6

### ✅ MedicationListViewModel
- `getMedications()` - LiveData for medications list
- `loadMedications()` - Load all medications
- `saveMedication(record)` - Save/update medication
- `deleteMedication(id)` - Delete medication
- `getMedicationCount()` - Get count

### ✅ DiaryListViewModel
- `getEntries()` - LiveData for diary entries
- `loadEntries()` - Load all entries
- `saveEntry(entry)` - Save entry
- `loadEntriesBetween(start, end)` - Load by date range

### ✅ SettingsViewModel
- `getThemeMode()` - Get theme setting
- `getAiProvider()` - Get AI provider
- `getPrivateMode()` - Get private mode
- `getWebSearchEnabled()` - Get web search setting
- `setThemeMode(theme)` - Update theme
- `setAiProvider(provider)` - Change AI provider
- `setPrivateMode(enabled)` - Toggle private mode
- `setWebSearchEnabled(enabled)` - Toggle web search

### ✅ ViewModelFactory
- Central factory for creating all ViewModels
- Injects ServiceLocator for repository access
- Easy to extend with new ViewModels

## Migration Priority

### Priority 1 (Next)
- MedicationsFragment → MedicationListViewModel
- DiaryFragment → DiaryListViewModel
- SettingsFragment → SettingsViewModel

### Priority 2
- ResourcesFragment → ResourcesViewModel
- InteractionCheckerFragment → InteractionCheckerViewModel

### Priority 3
- AssistantFragment (already uses custom logic, lower priority)
- ProfileFragment (simpler, update later)

## Benefits of This Pattern

1. **Separation of Concerns**: ViewModels handle data, fragments handle UI
2. **Lifecycle Management**: ViewModels survive configuration changes
3. **Testability**: Easy to unit test ViewModels independently
4. **Error Handling**: Centralized error handling in ViewModels
5. **Threading**: ViewModels handle background operations safely
6. **Reusability**: Same ViewModel can be used by multiple fragments if needed

## Common Patterns

### Load Data on View Created
```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    viewModel = new ViewModelProvider(this, 
        new ViewModelFactory(requireContext()))
        .get(MyViewModel.class);
    
    viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
        adapter.submitList(items);
    });
    
    viewModel.loadItems();
}
```

### Handle Errors
```java
viewModel.getError().observe(getViewLifecycleOwner(), error -> {
    if (error != null && !error.isEmpty()) {
        showErrorDialog(error);
    }
});
```

### Show Loading State
```java
viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
    binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
});
```

### Save Data
```java
private void saveMedication(MedicationRecord record) {
    viewModel.saveMedication(record);
}
```

## Updating Fragments

### Before (Direct Repository)
```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    try {
        MedicationRepository repo = 
            ServiceLocator.getInstance(requireContext())
                .getMedicationRepository();
        List<MedicationRecord> medications = repo.listRecords();
        adapter.submitList(medications);
    } catch (Exception e) {
        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
```

### After (ViewModel Pattern)
```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    viewModel = new ViewModelProvider(this,
        new ViewModelFactory(requireContext()))
        .get(MedicationListViewModel.class);
    
    viewModel.getMedications().observe(getViewLifecycleOwner(), medications -> {
        adapter.submitList(medications);
    });
    
    viewModel.getError().observe(getViewLifecycleOwner(), error -> {
        if (error != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        }
    });
    
    viewModel.loadMedications();
}
```

## Testing ViewModels

### Unit Test Example
```java
@RunWith(JUnit4.class)
public class MedicationListViewModelTest {
    private MedicationListViewModel viewModel;
    private ServiceLocator serviceLocator;
    
    @Before
    public void setup() {
        serviceLocator = Mockito.mock(ServiceLocator.class);
        MedicationRepository mockRepo = Mockito.mock(MedicationRepository.class);
        Mockito.when(serviceLocator.getMedicationRepository())
            .thenReturn(mockRepo);
        
        viewModel = new MedicationListViewModel(serviceLocator);
    }
    
    @Test
    public void testLoadMedications() {
        viewModel.loadMedications();
        // Assert that LiveData is updated
    }
}
```

## Next Steps

1. Update MedicationsFragment to use MedicationListViewModel
2. Update DiaryFragment to use DiaryListViewModel
3. Update SettingsFragment to use SettingsViewModel
4. Create ResourcesViewModel and update ResourcesFragment
5. Create InteractionCheckerViewModel and update fragment
6. Write unit tests for ViewModels

## Checklist for Each Fragment Migration

- [ ] Create ViewModel class
- [ ] Add to ViewModelFactory
- [ ] Update fragment to create and use ViewModel
- [ ] Test LiveData observation works
- [ ] Test error handling
- [ ] Test loading state
- [ ] Write unit tests for ViewModel
- [ ] Verify existing functionality still works

