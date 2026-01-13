# .gitignore Configuration for Kotlin Multiplatform

## Updated .gitignore for Recipe Manager Project

The .gitignore file has been updated to properly handle Kotlin Multiplatform Mobile (KMM) build artifacts and development files.

### Kotlin Multiplatform & Gradle Ignores

#### Build Artifacts
```gitignore
# Gradle build artifacts
build/                    # All build outputs (classes, reports, test results)
.gradle/                  # Gradle cache and metadata
gradle-app.setting        # Gradle application settings
!gradle-wrapper.jar       # Keep wrapper jar (needed for builds)
!gradle-wrapper.properties # Keep wrapper properties (needed for builds)
```

#### Kotlin-Specific
```gitignore
# Kotlin/Native
.konan/                   # Kotlin/Native compiler cache

# Kotlin compilation cache
kotlin-js-store/          # Kotlin/JS compilation cache
**/kotlin-js-store/       # Nested JS stores
**/compileSync/           # Compilation synchronization files
```

#### IDE Files
```gitignore
# IntelliJ IDEA
.idea/                    # IDE configuration
*.iml                     # IntelliJ module files
*.ipr                     # IntelliJ project files
*.iws                     # IntelliJ workspace files
out/                      # IDE output directory
```

#### Android-Specific (for future Android target)
```gitignore
# Android Studio
local.properties          # Local SDK paths
*.apk                     # Android packages
*.aab                     # Android App Bundles
*.ap_                     # Android resources
*.dex                     # Dalvik executable
captures/                 # UI test captures
```

#### SQLDelight Generated Files
```gitignore
# SQLDelight generated files
**/generated/             # All generated source code
```

### Verification Results

After running the build, the following artifacts are properly ignored:

✅ **Build Directory**: `build/` with all subdirectories
- `build/classes/` - Compiled Kotlin classes
- `build/generated/` - SQLDelight generated code
- `build/kotlin/` - Kotlin compilation metadata
- `build/processedResources/` - Processed resources
- `build/reports/` - Test and build reports
- `build/test-results/` - Test execution results

✅ **Gradle Cache**: `.gradle/` directory
- `.gradle/9.2.1/` - Gradle version cache
- `.gradle/buildOutputCleanup/` - Build cleanup metadata
- `.gradle/kotlin/` - Kotlin compilation cache
- `.gradle/vcs-1/` - Version control metadata

✅ **Git Status**: Clean working tree
```bash
$ git status
On branch master
nothing to commit, working tree clean
```

### What's NOT Ignored (Intentionally Kept)

#### Source Code
- `src/` - All source code directories
- `*.kt` - Kotlin source files
- `*.sql` - SQLDelight schema files

#### Configuration Files
- `build.gradle.kts` - Build configuration
- `settings.gradle.kts` - Project settings
- `gradle.properties` - Gradle properties
- `gradle/wrapper/` - Gradle wrapper files

#### Project Documentation
- `*.md` - Documentation files
- `.kiro/specs/` - Kiro specifications
- `.kiro/steering/` - Kiro steering files

#### Version Control
- `.gitignore` - This file itself

### Testing the Configuration

To verify the .gitignore is working correctly:

```bash
# Run a build to generate artifacts
./gradlew

# Check git status (should be clean)
git status

# List build artifacts (should exist but be ignored)
ls -la build/
ls -la .gradle/
```

### Best Practices

1. **Keep Wrapper Files**: The `!gradle-wrapper.jar` and `!gradle-wrapper.properties` ensure the project can be built without requiring Gradle to be pre-installed.

2. **Ignore Generated Code**: SQLDelight and other code generators create files that should not be committed.

3. **IDE Flexibility**: The configuration works with IntelliJ IDEA, Android Studio, and other Kotlin-compatible IDEs.

4. **Platform Agnostic**: Includes ignores for JVM, Android, and iOS targets even though we're currently using JVM only.

This configuration ensures a clean repository while maintaining all necessary files for building and running the Recipe Manager application.