package com.recipemanager.data.cloud

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.Recipe
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class CloudSyncManagerTest : FunSpec({
    
    test("should queue sync operation successfully") {
        // Create test database
        val databaseDriverFactory = DatabaseDriverFactory()
        val database = RecipeDatabase(databaseDriverFactory.createDriver())
        
        // Create mock Firebase services
        val mockAuth = object : FirebaseAuth {
            override val currentUser = flowOf<FirebaseUser?>(null)
            override suspend fun signInWithEmailAndPassword(email: String, password: String) = 
                Result.failure<FirebaseUser>(Exception("Not implemented"))
            override suspend fun createUserWithEmailAndPassword(email: String, password: String) = 
                Result.failure<FirebaseUser>(Exception("Not implemented"))
            override suspend fun signInAnonymously() = 
                Result.failure<FirebaseUser>(Exception("Not implemented"))
            override suspend fun signOut() = Result.success(Unit)
            override suspend fun getIdToken() = Result.failure<String>(Exception("Not implemented"))
        }
        
        val mockStorage = object : FirebaseStorage {
            override suspend fun uploadPhoto(photo: com.recipemanager.domain.model.Photo, data: ByteArray) = 
                Result.failure<String>(Exception("Not implemented"))
            override suspend fun downloadPhoto(cloudUrl: String) = 
                Result.failure<ByteArray>(Exception("Not implemented"))
            override suspend fun deletePhoto(cloudUrl: String) = Result.success(Unit)
            override suspend fun getDownloadUrl(storagePath: String) = 
                Result.failure<String>(Exception("Not implemented"))
        }
        
        val mockFirestore = object : FirebaseFirestore {
            override suspend fun saveRecipe(recipe: Recipe, userId: String) = Result.success(Unit)
            override suspend fun getRecipe(recipeId: String, userId: String) = 
                Result.success<Recipe?>(null)
            override fun getUserRecipes(userId: String) = flowOf<List<Recipe>>(emptyList())
            override suspend fun deleteRecipe(recipeId: String, userId: String) = Result.success(Unit)
        }
        
        // Create sync manager
        val syncManager = CloudSyncManager(
            auth = mockAuth,
            storage = mockStorage,
            firestore = mockFirestore,
            database = database
        )
        
        // Create test operation
        val testRecipe = Recipe(
            id = "test-recipe-1",
            title = "Test Recipe",
            description = "A test recipe",
            ingredients = emptyList(),
            steps = emptyList(),
            preparationTime = 30,
            cookingTime = 45,
            servings = 4,
            tags = listOf("test"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val syncOperation = SyncOperation(
            type = SyncOperationType.CREATE,
            entityType = SyncEntityType.RECIPE,
            entityId = testRecipe.id,
            data = Json.encodeToString(Recipe.serializer(), testRecipe)
        )
        
        // Queue the operation
        val result = syncManager.queueOperation(syncOperation)
        
        // Verify operation was queued successfully
        result.isSuccess shouldBe true
        
        // Verify operation is in the database
        val queueItems = database.syncQueueQueries.selectAllSyncQueueItems().executeAsList()
        queueItems.size shouldBe 1
        queueItems.first().entityId shouldBe testRecipe.id
        queueItems.first().operation shouldBe SyncOperationType.CREATE.name
        
        // Cleanup
        syncManager.cleanup()
    }
    
    test("should handle sync operation types correctly") {
        val operation = SyncOperation(
            type = SyncOperationType.UPDATE,
            entityType = SyncEntityType.RECIPE,
            entityId = "test-id",
            data = "{}"
        )
        
        operation.type shouldBe SyncOperationType.UPDATE
        operation.entityType shouldBe SyncEntityType.RECIPE
        operation.entityId shouldBe "test-id"
    }
    
    test("should handle conflict resolution data") {
        val conflict = ConflictResolution(
            operationId = "test-op-1",
            entityType = SyncEntityType.RECIPE,
            localData = "local",
            cloudData = "cloud",
            conflictType = ConflictType.CONCURRENT_UPDATE
        )
        
        conflict.operationId shouldBe "test-op-1"
        conflict.conflictType shouldBe ConflictType.CONCURRENT_UPDATE
    }
})