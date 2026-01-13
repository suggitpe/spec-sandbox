# Recipe Manager - Kotlin Multiplatform Mobile Application

A Kotlin Multiplatform mobile application for creating, managing, and sharing cooking recipes with integrated cooking assistance features.

## Features

- **Recipe Management**: Create, edit, delete, and organize cooking recipes
- **Photo Documentation**: Associate photos with specific cooking stages
- **Recipe Sharing**: Share recipes with friends across multiple platforms
- **Recipe Versioning**: Create upgraded versions and track recipe history
- **Cooking Timers**: Multiple concurrent timers with notifications
- **Recipe Collections**: Organize recipes into curated collections
- **Offline-First**: Full functionality without internet connectivity
- **Cloud Sync**: Automatic synchronization when connectivity is available

## Technology Stack

- **Framework**: Kotlin Multiplatform Mobile (KMM)
- **UI**: Compose Multiplatform
- **Database**: SQLDelight with SQLite
- **Serialization**: Kotlinx Serialization
- **Networking**: Ktor Client
- **Coroutines**: Kotlinx Coroutines
- **Date/Time**: Kotlinx DateTime
- **Testing**: Kotlin Test with Property-Based Testing
- **Logging**: Kermit

## Project Structure

```
src/
├── commonMain/kotlin/com/recipemanager/
│   ├── data/                    # Data layer implementation
│   │   ├── database/           # Database drivers and factories
│   │   └── repository/         # Repository implementations
│   ├── di/                     # Dependency injection
│   ├── domain/                 # Business logic layer
│   │   ├── model/              # Domain models
│   │   ├── repository/         # Repository interfaces
│   │   ├── usecase/            # Use cases
│   │   └── validation/         # Business validation
│   └── presentation/           # UI layer
├── commonMain/sqldelight/      # SQLDelight database schema
├── commonTest/kotlin/          # Shared tests
├── androidMain/kotlin/         # Android-specific code
└── iosMain/kotlin/             # iOS-specific code
```

## Getting Started

### Prerequisites

- **JDK 11 or higher**
- **Android Studio** (for Android development)
- **Xcode** (for iOS development, macOS only)
- **Kotlin Multiplatform Mobile plugin** for Android Studio

### Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. For iOS development, ensure Xcode is installed and configured

### Building

#### Android
```bash
./gradlew assembleDebug
```

#### iOS
```bash
./gradlew linkDebugFrameworkIosX64
```

### Testing

Run all tests:
```bash
./gradlew test
```

Run property-based tests specifically:
```bash
./gradlew testDebugUnitTest --tests "*PropertyTests*"
```

## Architecture

### Clean Architecture Layers

1. **Domain Layer** (`domain/`)
   - Business models and entities
   - Repository interfaces
   - Use cases (business logic)
   - Validation rules

2. **Data Layer** (`data/`)
   - Repository implementations
   - Database access (SQLDelight)
   - Network clients (Ktor)
   - Platform-specific drivers

3. **Presentation Layer** (`presentation/`)
   - Compose UI components
   - ViewModels (to be implemented)
   - Navigation (to be implemented)

### Key Design Patterns

- **Repository Pattern**: Abstracts data access
- **Use Case Pattern**: Encapsulates business logic
- **Dependency Injection**: Manual DI with factory pattern
- **Clean Architecture**: Separation of concerns across layers

## Core Models

### Recipe
```kotlin
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String? = null,
    val ingredients: List<Ingredient>,
    val steps: List<CookingStep>,
    val preparationTime: Int, // in minutes
    val cookingTime: Int, // in minutes
    val servings: Int,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int = 1,
    val parentRecipeId: String? = null
)
```

### Ingredient
```kotlin
@Serializable
data class Ingredient(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val notes: String? = null,
    val photos: List<Photo> = emptyList()
)
```

### CookingStep
```kotlin
@Serializable
data class CookingStep(
    val id: String,
    val stepNumber: Int,
    val instruction: String,
    val duration: Int? = null, // in minutes
    val temperature: Int? = null,
    val photos: List<Photo> = emptyList(),
    val timerRequired: Boolean = false
)
```

## Testing Strategy

### Property-Based Testing
The project uses property-based testing to validate universal properties:

- **Property 1**: Recipe Creation Completeness
- **Property 2**: Recipe Update Preservation  
- **Property 3**: Recipe Deletion Consistency
- **Property 4**: Search Result Relevance

Each property test runs 100 iterations with randomly generated data to ensure comprehensive coverage.

### Unit Testing
Traditional unit tests complement property tests for:
- Specific edge cases
- Error conditions
- Integration points

## Development Guidelines

### Code Style
- Follow Kotlin coding conventions
- Use meaningful names for classes and functions
- Write comprehensive KDoc for public APIs
- Prefer immutable data structures

### Testing Requirements
- All new domain logic must have property-based tests
- Repository implementations should have integration tests
- UI components should have unit tests
- Minimum 100 iterations for property tests

### Database Schema
- Use SQLDelight for type-safe SQL
- Define schema in `.sq` files
- Use migrations for schema changes
- Prefer normalized data structure

## Platform-Specific Features

### Android
- SQLite database with Android driver
- Android-specific UI components (future)
- Background services for timers (future)

### iOS
- SQLite database with native driver
- iOS-specific UI components (future)
- Background app refresh for sync (future)

## Future Enhancements

- [ ] Photo capture and management
- [ ] Recipe sharing functionality
- [ ] Cloud synchronization
- [ ] Push notifications for timers
- [ ] Recipe collections management
- [ ] Advanced search and filtering
- [ ] Recipe import/export
- [ ] Offline-first architecture
- [ ] Performance optimizations

## Contributing

1. Follow the existing architecture patterns
2. Write tests for new functionality
3. Update documentation for API changes
4. Ensure all tests pass before submitting
5. Use conventional commit messages

## License

This project is licensed under the MIT License.