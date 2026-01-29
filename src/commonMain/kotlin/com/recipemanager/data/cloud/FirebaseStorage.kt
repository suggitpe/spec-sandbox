package com.recipemanager.data.cloud

import com.recipemanager.domain.model.Photo

/**
 * Firebase Storage interface for photo management
 */
interface FirebaseStorage {
    
    /**
     * Upload a photo to Firebase Storage
     * @param photo The photo to upload
     * @param data The photo data as ByteArray
     * @return Result containing the cloud URL on success
     */
    suspend fun uploadPhoto(photo: Photo, data: ByteArray): Result<String>
    
    /**
     * Download a photo from Firebase Storage
     * @param cloudUrl The cloud URL of the photo
     * @return Result containing the photo data as ByteArray
     */
    suspend fun downloadPhoto(cloudUrl: String): Result<ByteArray>
    
    /**
     * Delete a photo from Firebase Storage
     * @param cloudUrl The cloud URL of the photo to delete
     * @return Result indicating success or failure
     */
    suspend fun deletePhoto(cloudUrl: String): Result<Unit>
    
    /**
     * Get download URL for a photo
     * @param storagePath The storage path of the photo
     * @return Result containing the download URL
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String>
}