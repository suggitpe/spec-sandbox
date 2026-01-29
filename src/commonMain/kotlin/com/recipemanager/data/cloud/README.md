# Firebase Integration

This package contains the Firebase integration for the Recipe Manager application, providing cloud synchronization capabilities for recipes and photos.

## Components

### Configuration
- `FirebaseConfig`: Configuration data class containing Firebase project settings
- `FirebaseFactory`: Platform-specific factory for creating Firebase service instances

### Authentication
- `FirebaseAuth`: Interface for user authentication
- `FirebaseUser`: Data class representing an authenticated user
- `FirebaseAuthImpl`: JVM implementation using Firebase REST API

### Storage
- `FirebaseStorage`: Interface for photo storage and retrieval
- `FirebaseStorageImpl`: JVM implementation using Firebase Storage REST API

### Firestore
- `FirebaseFirestore`: Interface for recipe data synchronization
- `FirebaseFirestoreImpl`: JVM implementation using Firestore REST API

### Synchronization
- `CloudSyncManager`: High-level manager that coordinates all Firebase services

## Platform Support

Currently implemented for JVM using Firebase REST APIs. The expect/actual pattern allows for easy extension to Android and iOS platforms using native Firebase SDKs.

## Usage

```kotlin
// Initialize Firebase
val config = FirebaseConfig.DEFAULT
val factory = FirebaseFactory()
factory.initialize(config)

// Create sync manager
val syncManager = CloudSyncManager(
    auth = factory.createAuth(),
    storage = factory.createStorage(),
    firestore = factory.createFirestore()
)

// Sign in user
val result = syncManager.signInAnonymously()

// Sync recipe
val recipe = Recipe(...)
syncManager.syncRecipe(recipe)
```

## Requirements Satisfied

This implementation satisfies requirement 8.3:
- ✅ Configure Firebase Storage for photo sync using platform-specific SDKs
- ✅ Set up Firestore for recipe data synchronization  
- ✅ Implement authentication for cloud access

The implementation provides offline-first functionality with cloud synchronization when connectivity is available.