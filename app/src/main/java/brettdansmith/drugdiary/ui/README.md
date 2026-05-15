# UI Layer Architecture

## Overview
The UI layer follows MVVM (Model-View-ViewModel) architecture with clean separation between Fragments (Views), ViewModels, and the domain/data layers.

## Directory Structure

```
ui/
├── assistant/          # AI Assistant chat interface (ChatGPT-style)
├── auth/              # Profile selection and authentication
├── diary/             # Diary tracking and logging
├── medications/       # Medication management
├── profile/           # User profile management
├── reference/         # Drug reference tools (interactions, reagents)
├── resources/         # Support resources and help
├── settings/          # App settings and preferences
├── tools/             # Utility tools interface
└── adapter/           # Shared adapter classes
```

## Key Components

### Fragment Responsibilities
- **View rendering**: Display data using ViewBinding
- **User interaction**: Handle click events and input
- **Fragment navigation**: Navigate to other screens via NavController
- **ViewModel lifecycle**: Manage ViewModel and observe LiveData

### ViewModel Responsibilities
- **State management**: Hold and manage UI state
- **Data transformation**: Convert domain models to UI models
- **Business logic coordination**: Coordinate with repositories
- **Lifecycle awareness**: Survive configuration changes

### Adapter Responsibilities
- **List rendering**: Efficiently display lists of items
- **Item binding**: Map data to UI views
- **Click handling**: Delegate clicks to parent fragment
- **Performance**: Implement DiffUtil for smooth updates

## Design Patterns

### MVVM Pattern
```
Fragment (View)
    ↓ observes
ViewModel
    ↓ uses
Repository Interface
    ↓ implements
Repository Implementation
    ↓ uses
Local/Network Data Sources
```

### Navigation Pattern
Fragments communicate through NavController using safe args where possible.

```java
NavHostFragment.findNavController(this).navigate(R.id.targetFragment)
```

### Adapter Pattern
Use RecyclerView.Adapter with DiffUtil for efficient list updates.

```java
public class ItemAdapter extends RecyclerView.Adapter<ItemViewHolder> {
    private List<Item> items = new ArrayList<>();
    
    public void submitList(List<Item> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
                // Compare old vs new items
            }
        );
        this.items = newItems;
        result.dispatchUpdatesTo(this);
    }
}
```

## Fragment Best Practices

### 1. Use ViewBinding
```java
private FragmentExampleBinding binding;

@Override
public View onCreateView(@NonNull LayoutInflater inflater, 
        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = FragmentExampleBinding.inflate(inflater, container, false);
    return binding.getRoot();
}

@Override
public void onDestroyView() {
    super.onDestroyView();
    binding = null;
}
```

### 2. Initialize ViewModel
```java
@Override
public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);
}
```

### 3. Observe LiveData
```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    viewModel.getData().observe(getViewLifecycleOwner(), data -> {
        // Update UI with data
    });
}
```

### 4. Handle Arguments Safely
```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    Bundle args = getArguments();
    if (args != null) {
        String itemId = args.getString("item_id");
        // Use itemId
    }
}
```

## AI Assistant (ChatGPT-style) Design

The AssistantFragment follows ChatGPT's Android client design pattern:

### Features
- **Chat interface**: Message bubbles for user and AI
- **Streaming responses**: Real-time response updates
- **Chat history**: Tab-based chat session navigation
- **Rich text**: Markdown formatting, code blocks, links
- **Attachments**: Support for images and files
- **Commands**: Special command execution with formatted output
- **Edit/Delete**: Message editing and deletion capabilities

### Message Types
1. **User Messages** (Sent) - Right-aligned, blue background
2. **AI Messages** (Received) - Left-aligned, gray background
3. **Command Messages** - Special formatting with command output
4. **System Messages** - Info/error messages with status colors

### Layout Structure
```
┌─────────────────────────────┐
│ [New Chat] Chat Tabs ▶▶      │ ← Chat history/tabs
├─────────────────────────────┤
│                             │
│ [User Message] ███████████  │
│                             │
│ ███████████ [AI Message]    │
│                             │
├─────────────────────────────┤
│ [Attachment] [+] Input [→]  │ ← Message composer
└─────────────────────────────┘
```

## Support Resources Design

The Support Resources screen provides:
- **Information cards**: Organized by category (Emergency, Treatment, Resources)
- **Quick actions**: Phone numbers, websites, navigation to app tools
- **Ask AI button**: Each card has an "Ask AI" button in bottom-right
- **Privacy**: All external links open outside the encrypted profile vault

## Fragment Navigation

### Main Navigation Flow
```
Profile/Diary/Medications ↔ Assistant
         ↕                    ↕
      Resources          Support Resources
         ↕                    ↕
      Tools             Drug Reference
```

### Guidelines
- Use AndroidX Navigation Component
- Define all routes in `nav_graph.xml`
- Use Safe Args for type-safe argument passing
- Handle back navigation properly
- Clear backstack when appropriate

## Performance Optimization

### RecyclerView Best Practices
1. **Use ViewHolder pattern**: Recycle views efficiently
2. **Implement DiffUtil**: Minimize layout updates
3. **Load images asynchronously**: Prevent UI blocking
4. **Set RecyclerView pool size**: Share pools between adapters
5. **Use notifyItemChanged** for single item updates

### Fragment Lifecycle
1. **onCreateView**: Inflate layout only
2. **onViewCreated**: Initialize views and setup observers
3. **onStart**: Resume any resources
4. **onPause**: Pause animations/tasks
5. **onDestroyView**: Clean up views (set binding = null)

## Testing Fragments

### Unit Tests
```java
@Test
public void testFragmentInitialization() {
    // Use FragmentScenario for testing
}
```

### Espresso Tests
```java
@Test
public void testUserInteraction() {
    // Use Espresso for UI testing
}
```

## Security Considerations

### Profile/PIN Screen
- **DO NOT MODIFY** - Handles sensitive authentication
- Biometric unlock integration
- Secure PIN verification

### Data Handling
- Never expose profile data in fragments without encryption
- Use ServiceLocator to access repositories
- Respect private mode settings
- Filter sensitive data when needed

### Navigation Security
- Validate all navigation arguments
- Clear sensitive data from backstack
- Prevent unauthorized fragment access

## Future Improvements

1. **Jetpack Compose**: Migrate to Compose for modern UI
2. **Animation API**: Add smooth transitions between fragments
3. **Accessibility**: Improve screen reader support
4. **Dark mode**: Full dark theme support
5. **Responsive layouts**: Better tablet/landscape support

