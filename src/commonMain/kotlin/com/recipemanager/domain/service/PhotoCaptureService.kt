package com.recipemanager.domain.service

import com.recipemanager.domain.model.Photo
import com.recipemanager.domain.model.PhotoStage
import com.recipemanager.domain.model.SyncStatus
import com.recipemanager.domain.repository.PhotoRepository
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * Service for capturing and managing photos.
 * Coordinates photo capture, optimization, and storage.
 */
class PhotoCaptureService(
    private val photoRepository: PhotoRepository,
    private val photoCaptureProvider: PhotoCaptureProvider
) {
    
    /**
     * Capture a new photo using the device camera.
     * @param stage The recipe stage this photo represents
     * @param caption Optional caption for the photo
     * @return Result containing the captured and saved Photo
     */
    suspend fun capturePhoto(
        stage: PhotoStage,
        caption: String? = null
    ): Result<Photo> {
        return try {
            // Capture photo using platform-specific provider
            val capturedPath = photoCaptureProvider.capturePhoto()
            
            // Create photo model
            val photo = Photo(
                id = UUID.randomUUID().toString(),
                localPath = capturedPath,
                cloudUrl = null,
                caption = caption,
                stage = stage,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.LOCAL_ONLY
            )
            
            // Save photo (includes optimization)
            photoRepository.savePhoto(photo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import a photo from the device gallery.
     * @param sourcePath Path to the photo in the gallery
     * @param stage The recipe stage this photo represents
     * @param caption Optional caption for the photo
     * @return Result containing the imported and saved Photo
     */
    suspend fun importPhoto(
        sourcePath: String,
        stage: PhotoStage,
        caption: String? = null
    ): Result<Photo> {
        return try {
            val photo = Photo(
                id = UUID.randomUUID().toString(),
                localPath = sourcePath,
                cloudUrl = null,
                caption = caption,
                stage = stage,
                timestamp = Clock.System.now(),
                syncStatus = SyncStatus.LOCAL_ONLY
            )
            
            // Save photo (includes optimization)
            photoRepository.savePhoto(photo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update photo caption.
     */
    suspend fun updatePhotoCaption(photoId: String, caption: String): Result<Photo> {
        return try {
            val photoResult = photoRepository.getPhoto(photoId)
            if (photoResult.isFailure) {
                return Result.failure(photoResult.exceptionOrNull()!!)
            }
            
            val photo = photoResult.getOrNull()
                ?: return Result.failure(IllegalArgumentException("Photo not found: $photoId"))
            
            val updatedPhoto = photo.copy(caption = caption)
            photoRepository.savePhoto(updatedPhoto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a photo and cleanup storage.
     */
    suspend fun deletePhoto(photoId: String): Result<Unit> {
        return photoRepository.deletePhoto(photoId)
    }
}

/**
 * Platform-specific photo capture provider.
 * Implementations handle camera access and gallery selection.
 */
interface PhotoCaptureProvider {
    /**
     * Capture a photo using the device camera.
     * @return Path to the captured photo file
     */
    suspend fun capturePhoto(): String
    
    /**
     * Select a photo from the device gallery.
     * @return Path to the selected photo file
     */
    suspend fun selectFromGallery(): String
}
