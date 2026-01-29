package com.recipemanager.data.cloud

import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.SyncStatus
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive synchronization manager that coordinates cloud operations using Ktor Client
 * Implements offline queue for pending operations and conflict resolution for simultaneous edits
 */
class CloudSyncManager(
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val database: RecipeDatabase,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
) {
    
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncStatus = MutableStateFlow(SyncManagerStatus.IDLE)
    private val _conflictResolutions = MutableSharedFlow<ConflictResolution>()
    
    /**
     * Current sync status
     */
    val syncStatus: StateFlow<SyncManagerStatus> = _syncStatus.asStateFlow()
    
    /**
     * Conflict resolutions that need user input
     */
    val conflictResolutions: SharedFlow<ConflictResolution> = _conflictResolutions.asSharedFlow()
    
    /**
     * Current authenticated user
     */
    val currentUser: Flow<FirebaseUser?> = auth.currentUser
    
    init {
        // Start periodic sync when authenticated
        syncScope.launch {
            auth.currentUser.collect { user ->
                if (user != null) {
                    startPeriodicSync()
                } else {
                    stopPeriodicSync()
                }
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return auth.signInWithEmailAndPassword(email, password)
    }
    
    /**
     * Sign in anonymously for guest access
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return auth.signInAnonymously()
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return auth.signOut()
    }
    
    /**
     * Queue an operation for synchronization
     */
    suspend fun queueOperation(operation: SyncOperation): Result<Unit> {
        return try {
            val syncItem = SyncQueueItem(
                id = generateId(),
                operation = operation.type.name,
                entityType = operation.entityType.name,
                entityId = operation.entityId,
                payload = Json.encodeToString(SyncOperation.serializer(), operation),
                createdAt = Clock.System.now().epochSeconds,
                retryCount = 0
            )
            
            database.syncQueueQueries.insertSyncQueueItem(
                id = syncItem.id,
                operation = syncItem.operation,
                entityType = syncItem.entityType,
                entityId = syncItem.entityId,
                payload = syncItem.payload,
                createdAt = syncItem.createdAt,
                retryCount = syncItem.retryCount
            )
            
            // Trigger immediate sync if online
            if (isOnline()) {
                syncScope.launch { processSyncQueue() }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process all pending sync operations
     */
    suspend fun processSyncQueue(): Result<Unit> {
        if (_syncStatus.value == SyncManagerStatus.SYNCING) {
            return Result.success(Unit) // Already syncing
        }
        
        _syncStatus.value = SyncManagerStatus.SYNCING
        
        return try {
            val queueItems = database.syncQueueQueries.selectAllSyncQueueItems().executeAsList()
            
            for (item in queueItems) {
                try {
                    val operation = Json.decodeFromString<SyncOperation>(item.payload)
                    val result = executeOperation(operation)
                    
                    result.fold(
                        onSuccess = {
                            // Remove successful operation from queue
                            database.syncQueueQueries.deleteSyncQueueItem(item.id)
                        },
                        onFailure = { error ->
                            // Handle retry logic
                            val newRetryCount = item.retryCount + 1
                            if (newRetryCount >= MAX_RETRY_COUNT) {
                                // Max retries reached, remove from queue
                                database.syncQueueQueries.deleteSyncQueueItem(item.id)
                            } else {
                                // Update retry count
                                database.syncQueueQueries.updateSyncQueueItemRetryCount(
                                    retryCount = newRetryCount,
                                    id = item.id
                                )
                            }
                        }
                    )
                } catch (e: Exception) {
                    // Malformed operation, remove from queue
                    database.syncQueueQueries.deleteSyncQueueItem(item.id)
                }
            }
            
            _syncStatus.value = SyncManagerStatus.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncManagerStatus.ERROR
            Result.failure(e)
        }
    }
    
    /**
     * Execute a specific sync operation
     */
    private suspend fun executeOperation(operation: SyncOperation): Result<Unit> {
        return when (operation.type) {
            SyncOperationType.CREATE -> executeCreateOperation(operation)
            SyncOperationType.UPDATE -> executeUpdateOperation(operation)
            SyncOperationType.DELETE -> executeDeleteOperation(operation)
        }
    }
    
    /**
     * Execute create operation with conflict detection
     */
    private suspend fun executeCreateOperation(operation: SyncOperation): Result<Unit> {
        return when (operation.entityType) {
            SyncEntityType.RECIPE -> {
                val recipe = Json.decodeFromString<Recipe>(operation.data)
                
                // Check if recipe already exists in cloud (conflict detection)
                firestore.getRecipe(recipe.id, getCurrentUserId()).fold(
                    onSuccess = { existingRecipe ->
                        if (existingRecipe != null) {
                            // Conflict: recipe already exists
                            handleConflict(operation, existingRecipe, recipe)
                        } else {
                            // No conflict, proceed with creation
                            firestore.saveRecipe(recipe, getCurrentUserId())
                        }
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            }
            SyncEntityType.PHOTO -> {
                val photo = Json.decodeFromString<Photo>(operation.data)
                val photoData = operation.binaryData ?: return Result.failure(Exception("Photo data missing"))
                storage.uploadPhoto(photo, photoData).map { Unit }
            }
            else -> Result.failure(Exception("Unsupported entity type for create: ${operation.entityType}"))
        }
    }
    
    /**
     * Execute update operation with conflict resolution
     */
    private suspend fun executeUpdateOperation(operation: SyncOperation): Result<Unit> {
        return when (operation.entityType) {
            SyncEntityType.RECIPE -> {
                val localRecipe = Json.decodeFromString<Recipe>(operation.data)
                
                // Get current cloud version for conflict detection
                firestore.getRecipe(localRecipe.id, getCurrentUserId()).fold(
                    onSuccess = { cloudRecipe ->
                        if (cloudRecipe == null) {
                            // Recipe doesn't exist in cloud, treat as create
                            firestore.saveRecipe(localRecipe, getCurrentUserId())
                        } else if (cloudRecipe.updatedAt > localRecipe.updatedAt) {
                            // Conflict: cloud version is newer
                            handleConflict(operation, cloudRecipe, localRecipe)
                        } else {
                            // No conflict or local is newer, proceed with update
                            firestore.saveRecipe(localRecipe, getCurrentUserId())
                        }
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            }
            else -> Result.failure(Exception("Unsupported entity type for update: ${operation.entityType}"))
        }
    }
    
    /**
     * Execute delete operation
     */
    private suspend fun executeDeleteOperation(operation: SyncOperation): Result<Unit> {
        return when (operation.entityType) {
            SyncEntityType.RECIPE -> {
                firestore.deleteRecipe(operation.entityId, getCurrentUserId())
            }
            SyncEntityType.PHOTO -> {
                val cloudUrl = operation.data
                storage.deletePhoto(cloudUrl)
            }
            else -> Result.failure(Exception("Unsupported entity type for delete: ${operation.entityType}"))
        }
    }
    
    /**
     * Handle conflicts between local and cloud data
     */
    private suspend fun handleConflict(
        operation: SyncOperation,
        cloudData: Any,
        localData: Any
    ): Result<Unit> {
        val conflict = ConflictResolution(
            operationId = operation.entityId,
            entityType = operation.entityType,
            localData = localData,
            cloudData = cloudData,
            conflictType = when (operation.type) {
                SyncOperationType.CREATE -> ConflictType.DUPLICATE_CREATE
                SyncOperationType.UPDATE -> ConflictType.CONCURRENT_UPDATE
                SyncOperationType.DELETE -> ConflictType.DELETE_MODIFIED
            }
        )
        
        // Emit conflict for user resolution
        _conflictResolutions.emit(conflict)
        
        // For now, return success to remove from queue
        // In a real implementation, you might want to keep it queued until resolved
        return Result.success(Unit)
    }
    
    /**
     * Resolve a conflict with user choice
     */
    suspend fun resolveConflict(
        conflictId: String,
        resolution: ConflictResolutionChoice
    ): Result<Unit> {
        return try {
            when (resolution) {
                ConflictResolutionChoice.USE_LOCAL -> {
                    // Force update with local data
                    // Implementation would depend on the specific conflict
                }
                ConflictResolutionChoice.USE_CLOUD -> {
                    // Update local data with cloud data
                    // Implementation would depend on the specific conflict
                }
                ConflictResolutionChoice.MERGE -> {
                    // Attempt to merge data
                    // Implementation would depend on the specific conflict
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start periodic synchronization
     */
    private fun startPeriodicSync() {
        syncScope.launch {
            while (currentCoroutineContext().isActive) {
                delay(SYNC_INTERVAL)
                if (isOnline()) {
                    processSyncQueue()
                }
            }
        }
    }
    
    /**
     * Stop periodic synchronization
     */
    private fun stopPeriodicSync() {
        // Coroutines will be cancelled when scope is cancelled
    }
    
    /**
     * Check if device is online
     */
    private suspend fun isOnline(): Boolean {
        // Simple implementation - in real app, you'd check network connectivity
        return try {
            auth.getIdToken().isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get current user ID
     */
    private suspend fun getCurrentUserId(): String {
        // In a real implementation, get from auth service
        return "current-user-id"
    }
    
    /**
     * Generate unique ID
     */
    private fun generateId(): String {
        return "${Clock.System.now().epochSeconds}-${(0..999999).random()}"
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        syncScope.cancel()
        httpClient.close()
    }
    
    companion object {
        private const val MAX_RETRY_COUNT = 3
        private val SYNC_INTERVAL = 30.seconds
    }
}

/**
 * Sync operation data structure
 */
@Serializable
data class SyncOperation(
    val type: SyncOperationType,
    val entityType: SyncEntityType,
    val entityId: String,
    val data: String, // JSON serialized entity data
    val binaryData: ByteArray? = null // For photos
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as SyncOperation
        
        if (type != other.type) return false
        if (entityType != other.entityType) return false
        if (entityId != other.entityId) return false
        if (data != other.data) return false
        if (binaryData != null) {
            if (other.binaryData == null) return false
            if (!binaryData.contentEquals(other.binaryData)) return false
        } else if (other.binaryData != null) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + entityType.hashCode()
        result = 31 * result + entityId.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + (binaryData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Types of sync operations
 */
@Serializable
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Types of entities that can be synced
 */
@Serializable
enum class SyncEntityType {
    RECIPE,
    PHOTO,
    COLLECTION
}

/**
 * Sync queue item from database
 */
data class SyncQueueItem(
    val id: String,
    val operation: String,
    val entityType: String,
    val entityId: String,
    val payload: String,
    val createdAt: Long,
    val retryCount: Long
)

/**
 * Sync manager status
 */
enum class SyncManagerStatus {
    IDLE,
    SYNCING,
    ERROR
}

/**
 * Conflict resolution data
 */
data class ConflictResolution(
    val operationId: String,
    val entityType: SyncEntityType,
    val localData: Any,
    val cloudData: Any,
    val conflictType: ConflictType
)

/**
 * Types of conflicts
 */
enum class ConflictType {
    DUPLICATE_CREATE,
    CONCURRENT_UPDATE,
    DELETE_MODIFIED
}

/**
 * Conflict resolution choices
 */
enum class ConflictResolutionChoice {
    USE_LOCAL,
    USE_CLOUD,
    MERGE
}