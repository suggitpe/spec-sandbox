# Project Structure

## Source Sets

```
src/
├── commonMain/          # Shared code across all platforms
│   ├── kotlin/          # Kotlin source files
│   └── sqldelight/      # SQLDelight schema files (.sq)
├── commonTest/          # Shared tests (Kotest property & unit tests)
├── androidMain/         # Android-specific implementations
├── iosMain/             # iOS-specific implementations
└── jvmMain/             # JVM-specific implementations
```

## Package Organization

```
com.recipemanager/
├── domain/                          # Business logic (platform-independent)
│   ├── model/                       # Domain entities
│   │   ├── Recipe.kt
│   │   ├── Ingredient.kt
│   │   ├── CookingStep.kt
│   │   ├── CookingTimer.kt
│   │   ├── Photo.kt
│   │   └── RecipeCollection.kt
│   ├── repository/                  # Repository interfaces
│   │   ├── RecipeRepository.kt
│   │   ├── PhotoRepository.kt
│   │   └── TimerRepository.kt
│   ├── usecase/                     # Business use cases
│   │   ├── RecipeUseCases.kt
│   │   ├── ImportRecipeUseCase.kt
│   │   └── ShareRecipeUseCase.kt
│   ├── service/                     # Domain services
│   │   ├── PhotoAssociationService.kt
│   │   ├── PhotoCaptureService.kt
│   │   ├── RecipeCopyManager.kt
│   │   ├── ShareService.kt
│   │   └── PlatformShareService.kt
│   └── validation/                  # Business validation
│       └── RecipeValidator.kt
├── data/                            # Data layer implementations
│   ├── database/                    # Database drivers (expect/actual)
│   │   ├── DatabaseDriverFactory.kt
│   │   └── DatabaseManager.kt
│   ├── repository/                  # Repository implementations
│   │   ├── RecipeRepositoryImpl.kt
│   │   └── PhotoRepositoryImpl.kt
│   └── storage/                     # File storage
│       └── PhotoStorage.kt
└── di/                              # Dependency injection
    └── AppModule.kt
```

## SQLDelight Schema

```
src/commonMain/sqldelight/com/recipemanager/database/
├── Recipe.sq                        # Recipe table and queries
├── Ingredient.sq                    # Ingredient table and queries
├── CookingStep.sq                   # Cooking step table and queries
├── CookingTimer.sq                  # Timer table and queries
├── Photo.sq                         # Photo table and queries
├── PhotoIngredient.sq               # Photo-ingredient associations
├── PhotoCookingStep.sq              # Photo-step associations
├── Collection.sq                    # Collection table and queries
├── RecipeCollection.sq              # Recipe-collection associations
└── SyncQueue.sq                     # Sync queue for offline changes
```

## Test Organization

```
src/commonTest/kotlin/com/recipemanager/
├── domain/                          # Property-based tests
│   ├── RecipePropertyTest.kt
│   ├── PhotoPropertyTest.kt
│   └── RecipeSharingPropertyTest.kt
└── test/                            # Test utilities
    ├── generators/                  # Kotest Arbitraries
    │   └── RecipeGenerators.kt
    └── TestRunner.kt
```

## Architecture Layers

### Domain Layer (Pure Kotlin)
- No platform dependencies
- Business models with `@Serializable` annotation
- Repository interfaces (not implementations)
- Use cases encapsulate business logic
- Validation rules

### Data Layer (Platform-Specific)
- Repository implementations
- Database drivers using expect/actual pattern
- Platform-specific storage (photos, files)
- Network clients (future)

### DI Layer
- Manual dependency injection
- Factory pattern for creating instances
- Platform-specific initialization

## Key Conventions

- **Immutable Models**: All domain models are immutable data classes
- **Result Type**: Use `Result<T>` for operations that can fail
- **Flow**: Use `Flow<T>` for reactive data streams
- **Suspend Functions**: Use `suspend` for async operations
- **Expect/Actual**: Platform-specific code uses expect/actual pattern
- **Package by Feature**: Group related functionality together
