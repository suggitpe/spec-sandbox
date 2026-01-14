package com.recipemanager.data.repository

import com.recipemanager.data.storage.PhotoStorage
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.domain.model.SyncStatus
import com.recipemanager.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * Implementation of PhotoRepository using SQLDelight for local storage.
 * Handles photo capture, storage, optimization, and stage associations.
 */
class PhotoRepositoryImpl(
    private val database: RecipeDatabase,
    private val photoStorage: PhotoStorage
) : PhotoRepository {
    
    /**
     * Save a photo to local storage and database.
     * Optimizes and compresses the photo before storing.
     */
    override suspend fun savePhoto(photo: Photo): Result<Photo> = withContext(Dispatchers.IO) {
        try {
            // Optimize and store the photo file
            val optimizedPath = photoStorage.savePhoto(photo.localPath)
            
            // Create photo with optimized path
            val optimizedPhoto = photo.copy(localPath = optimizedPath)
            
            // Insert into database
            database.photoQueries.insertPhoto(
                id = optimizedPhoto.id,
                localPath = optimizedPhoto.localPath,
                cloudUrl = optimizedPhoto.cloudUrl,
                caption = optimizedPhoto.caption,
                stage = optimizedPhoto.stage.name,
                timestamp = optimizedPhoto.timestamp.epochSeconds,
                syncStatus = optimizedPhoto.syncStatus.name
            )
            
            Result.success(optimizedPhoto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retrieve a photo by its ID.
     */
    override suspend fun getPhoto(id: String): Result<Photo?> = withContext(Dispatchers.IO) {
        try {
            val photoEntity = database.photoQueries.selectPhotoById(id).executeAsOneOrNull()
            val photo = photoEntity?.let { mapToPhoto(it) }
            Result.success(photo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retrieve all photos associated with a specific recipe stage.
     */
    override suspend fun getPhotosByStage(recipeId: String, stage: PhotoStage): Result<List<Photo>> = 
        withContext(Dispatchers.IO) {
            try {
                val photoEntities = database.photoQueries.selectPhotosByStage(stage.name).executeAsList()
                val photos = photoEntities.map { mapToPhoto(it) }
                Result.success(photos)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Delete a photo from storage and database.
     * Includes cleanup of the physical file.
     */
    override suspend fun deletePhoto(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get photo to retrieve file path
            val photoEntity = database.photoQueries.selectPhotoById(id).executeAsOneOrNull()
            
            if (photoEntity != null) {
                // Delete physical file
                photoStorage.deletePhoto(photoEntity.localPath)
                
                // Delete from database (cascade will handle associations)
                database.photoQueries.deletePhoto(id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe photos for a specific recipe with real-time updates.
     */
    override fun observePhotos(recipeId: String): Flow<List<Photo>> = flow {
        // For now, emit all photos (can be filtered by recipe associations later)
        val photoEntities = database.photoQueries.selectAllPhotos().executeAsList()
        val photos = photoEntities.map { mapToPhoto(it) }
        emit(photos)
    }
    
    /**
     * Associate a photo with an ingredient.
     */
    suspend fun associatePhotoWithIngredient(photoId: String, ingredientId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                database.photoIngredientQueries.insertPhotoIngredient(photoId, ingredientId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Associate a photo with a cooking step.
     */
    suspend fun associatePhotoWithCookingStep(photoId: String, stepId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                database.photoCookingStepQueries.insertPhotoCookingStep(photoId, stepId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get all photos associated with a specific ingredient.
     */
    suspend fun getPhotosByIngredientId(ingredientId: String): Result<List<Photo>> = 
        withContext(Dispatchers.IO) {
            try {
                val photoEntities = database.photoIngredientQueries
                    .selectPhotosByIngredientId(ingredientId)
                    .executeAsList()
                val photos = photoEntities.map { mapToPhoto(it) }
                Result.success(photos)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Get all photos associated with a specific cooking step.
     */
    suspend fun getPhotosByStepId(stepId: String): Result<List<Photo>> = 
        withContext(Dispatchers.IO) {
            try {
                val photoEntities = database.photoCookingStepQueries
                    .selectPhotosByStepId(stepId)
                    .executeAsList()
                val photos = photoEntities.map { mapToPhoto(it) }
                Result.success(photos)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Remove association between a photo and an ingredient.
     */
    suspend fun removePhotoIngredientAssociation(photoId: String, ingredientId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                database.photoIngredientQueries.deletePhotoIngredient(photoId, ingredientId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Remove association between a photo and a cooking step.
     */
    suspend fun removePhotoCookingStepAssociation(photoId: String, stepId: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                database.photoCookingStepQueries.deletePhotoCookingStep(photoId, stepId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Map database entity to domain Photo model.
     */
    private fun mapToPhoto(entity: com.recipemanager.database.Photo): Photo {
        return Photo(
            id = entity.id,
            localPath = entity.localPath,
            cloudUrl = entity.cloudUrl,
            caption = entity.caption,
            stage = PhotoStage.valueOf(entity.stage),
            timestamp = Instant.fromEpochSeconds(entity.timestamp),
            syncStatus = SyncStatus.valueOf(entity.syncStatus)
        )
    }
}
