# Recipe Manager UI Layer

This package contains the presentation layer for the Recipe Manager application, built with Compose Multiplatform.

## Architecture

The UI follows the MVVM (Model-View-ViewModel) pattern:

- **ViewModels**: Manage UI state and business logic
- **Screens**: Composable UI components
- **Navigation**: Simple screen-based navigation

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
- 7.2: Clear, usable interface

### Navigation

The `RecipeApp` composable provides simple screen-based navigation:
- `Screen.RecipeList`: Main recipe list
- `Screen.RecipeDetail(recipeId)`: Recipe detail view
- `Screen.RecipeForm(recipeId?)`: Create (null) or edit (with ID) recipe

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

To use these screens in your application:

1. Initialize dependencies (DatabaseManager, RecipeRepository, RecipeValidator)
2. Create ViewModels with the repository and validator
3. Use the screen composables with appropriate callbacks for navigation

Example:
```kotlin
val recipeListViewModel = RecipeListViewModel(recipeRepository)

RecipeListScreen(
    viewModel = recipeListViewModel,
    onRecipeClick = { recipeId -> /* Navigate to detail */ },
    onCreateRecipe = { /* Navigate to form */ }
)
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

## Material Design 3

All screens use Material Design 3 components:
- Material3 theme colors and typography
- Cards for list items and detail sections
- Outlined text fields for forms
- Floating action buttons
- Icon buttons
- Chips for tags
- Alert dialogs for adding items

## Future Enhancements

- Photo management UI (Task 10.2)
- Cooking mode with timers (Task 10.3)
- Collection management (Task 10.4)
- Navigation state persistence (Task 11.1)
- Deep linking support
