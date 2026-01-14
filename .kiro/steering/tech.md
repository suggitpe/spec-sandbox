# Technology Stack

## Build System

- **Gradle**: Kotlin DSL (build.gradle.kts)
- **Kotlin**: 1.9.21
- **Gradle Wrapper**: Use `./gradlew` (Unix) or `gradlew.bat` (Windows)

## Core Technologies

- **Kotlin Multiplatform Mobile (KMM)**: Shared business logic across platforms
- **SQLDelight 2.0.1**: Type-safe SQL database with platform-specific drivers
- **Kotlinx Coroutines 1.7.3**: Asynchronous programming
- **Kotlinx Serialization 1.6.2**: JSON serialization/deserialization
- **Kotlinx DateTime 0.5.0**: Cross-platform date/time handling

## Testing

- **Kotest 5.8.0**: Testing framework with property-based testing support
  - `kotest-framework-engine`: Core test framework
  - `kotest-assertions-core`: Assertion matchers
  - `kotest-property`: Property-based testing
  - `kotest-runner-junit5`: JUnit 5 integration
- **JUnit Platform**: Test execution on JVM

## Common Commands

### Build & Test
```bash
# Run all tests (default task - includes clean build)
./gradlew

# Run all tests explicitly
./gradlew test
./gradlew testAll

# Run JVM tests only (no clean)
./gradlew jvmTest

# Clean build artifacts
./gradlew clean

# Build without tests
./gradlew build -x test
```

### Development
```bash
# Generate SQLDelight code
./gradlew generateCommonMainRecipeDatabaseInterface

# Check for dependency updates
./gradlew dependencyUpdates

# View all tasks
./gradlew tasks
```

## Platform Targets

- **JVM**: Primary development and testing target
- **Android**: Planned (not yet configured)
- **iOS**: Planned (not yet configured)

## Database

- **SQLDelight**: Type-safe SQL with compile-time verification
- **SQLite**: Underlying database engine
- **Schema Location**: `src/commonMain/sqldelight/com/recipemanager/database/`
- **Generated Package**: `com.recipemanager.database`

## Gradle Configuration Notes

- Default task runs clean build + all tests
- JUnit Platform required for Kotest on JVM
- SQLDelight generates Kotlin code from `.sq` files
- Parallel builds and caching enabled for performance
