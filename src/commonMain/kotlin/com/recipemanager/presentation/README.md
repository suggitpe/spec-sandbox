# Recipe Manager UI Layer

This package contains the presentation layer for the Recipe Manager application, built with Compose Multiplatform.

## Architecture

The UI follows the MVVM (Model-View-ViewModel) pattern with proper navigation and state management:

- **ViewModels**: Manage UI state and business logic
- **Screens**: Composable UI components
- **Navigation**: Compose Navigation with state persistence and deep linking support

## Navigation System

### Navigation Structure
The application uses Compose Navigation with the following features:
- **State Persistence**: Navigation state is preserved across app sessions
- **Deep Linking**: Support for shared recipe and collection deep links
- **Type-Safe Routes**: Centralized route definitions with parameter helpers

### Routes
All navigation routes are defined in `Routes.kt`:
- `RECIPE_LIST`: Main recipe list screen
- `RECIPE_DETAIL/{recipeId}`: Recipe detail view
- `RECIPE_FORM?recipeId={recipeId}`: Create/edit recipe form
- `COLLECTION_LIST`: Collection list screen
- `COLLECTION_DETAIL/{collectionId}`: Collection detail view
- `COOKING_MODE/{recipeId}`: Cooking assistance mode
- `PHOTO_MANAGEMENT/{recipeId}`: Photo management interface
- `IMPORT_RECIPE`: Recipe import screen
- `SHARE_RECIPE/{recipeId}`: Recipe sharing interface

### Deep Linking
Supports deep links for sharing:
- Recipe: `recipemanager://recipe/{recipeId}`
- Collection: `recipemanager://collection/{collectionId}`
- Web links: `https://recipemanager.app/recipe/{recipeId}`

### State Persistence
Navigation state is automatically saved and restored:
- Current route and back stack are preserved
- State survives app restarts and background/foreground transitions
- Platform-specific storage (file system on JVM, preferences on mobile)

## Components

### ViewModels

#### RecipeListViewModel
Manages the recipe list screen state including:
- Loading all recipes
- Searching recipes
- Deleting recipes
- Error handling

#### RecipeDetailViewModel
Manages the recipe detail screen state including:
- Loading a single recipe by ID
- Error handling

#### RecipeFormViewModel
Manages the recipe creation/editing form state including:
- Loading existing recipe for editing
- Form field updates (title, description, times, servings)
- Managing ingredients and cooking steps
- Managing tags
- Form validation
- Saving recipes (create or update)

### Screens

#### RecipeListScreen
Displays a list of all recipes with:
- Search bar for filtering recipes
- Recipe cards showing title, description, metadata (prep time, cook time, servings), and tags
- Floating action button to create new recipe
- Delete button for each recipe
- Empty state when no recipes exist
- Error message display

**Requirements Addressed:**
- 1.1: Recipe creation (via FAB navigation)
- 1.5: Recipe search functionality
- 7.1: Navigation structure
- 7.2: Large, readable text for cooking mode

#### RecipeDetailScreen
Displays full recipe details including:
- Recipe title and description
- Metadata card (prep time, cook time, servings)
- Tags
- Ingredients list with quantities and units
- Cooking steps with step numbers, instructions, duration, and temperature
- Edit button to navigate to form
- Back navigation

**Requirements Addressed:**
- 1.1: View recipe details
- 7.1: Navigation structure
- 7.2: Large, readable text for cooking mode

#### RecipeFormScreen
Form for creating or editing recipes with:
- Title and description fields
- Preparation time, cooking time, and servings inputs
- Tag management (add/remove)
- Ingredient management with dialog for adding (name, quantity, unit)
- Cooking step management with dialog for adding (instruction, duration, temperature)
- Form validation with error messages
- Save button with loading state
- Back navigation

**Requirements Addressed:**
- 1.1: Create new recipes
- 1.2: Edit existing recipes
- 7.1: Navigation structure
- 7.2: Clear, usable interface

### Navigation Components

#### NavigationStateManager
Manages navigation state and provides:
- State serialization/deserialization
- Navigation helper methods
- Deep link handling
- Back stack management

#### AppStatePersistenceManager
Handles state persistence across app sessions:
- Saves/loads navigation state
- Platform-specific storage implementation
- Error handling and fallback to default state

#### DeepLinkHandler
Processes deep links for shared content:
- Recipe deep link parsing and navigation
- Collection deep link parsing and navigation
- Web link support for cross-platform sharing
- Link generation for sharing features

## Usage

### Desktop (JVM)

The `Main.kt` file provides a desktop application entry point:

```kotlin
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Recipe Manager"
    ) {
        RecipeApp()
    }
}
```

### Integration

The main `RecipeApp` composable handles:
1. Dependency initialization (database, repositories, validators)
2. Navigation setup with state persistence
3. Deep link handling
4. State restoration on app start

Example navigation usage:
```kotlin
// Navigate to recipe detail
navController.navigate(Routes.recipeDetail("recipe123"))

// Navigate to recipe form for editing
navController.navigate(Routes.recipeForm("recipe456"))

// Navigate to recipe form for creation
navController.navigate(Routes.recipeForm())
```

## Features

### Search and Filtering
- Real-time search as user types
- Searches across recipe title, ingredients, and tags
- Shows filtered results or empty state

### Form Validation
- Required fields: title, at least one ingredient, at least one cooking step
- Numeric validation for times and servings
- Error messages displayed inline

### State Management
- ViewModels use Kotlin Flow for reactive state updates
- Loading states for async operations
- Error handling with user-friendly messages
- Success callbacks for navigation after save
- Navigation state persistence across app sessions

### Deep Linking
- Support for shared recipe links
- Automatic navigation to shared content
- Fallback handling for invalid links

## Material Design 3

All screens use Material Design 3 components:
- Material3 theme colors and typography
- Cards for list items and detail sections
- Outlined text fields for forms
- Floating action buttons
- Icon buttons
- Chips for tags
- Alert dialogs for adding items

## Requirements Addressed

- **7.1**: Navigation structure with clear visual indicators and deep linking support
- **7.5**: State preservation across app sessions and navigation transitions
- **8.1, 8.2**: Data persistence with automatic state saving and restoration

## Future Enhancements

- Photo management UI (Task 10.2)
- Cooking mode with timers (Task 10.3)
- Collection management (Task 10.4)
- Enhanced deep linking with parameter validation
- Navigation animations and transitions
