package com.recipemanager.data.storage

/**
 * Interface for photo storage operations.
 * Handles photo optimization, compression, and file system operations.
 */
interface PhotoStorage {
    /**
     * Save a photo to local storage with optimization and compression.
     * @param sourcePath The original photo file path
     * @return The path to the optimized and stored photo
     */
    suspend fun savePhoto(sourcePath: String): String
    
    /**
     * Delete a photo from local storage.
     * @param photoPath The path to the photo file
     */
    suspend fun deletePhoto(photoPath: String)
    
    /**
     * Check if a photo exists at the given path.
     * @param photoPath The path to check
     * @return true if the photo exists, false otherwise
     */
    suspend fun photoExists(photoPath: String): Boolean
    
    /**
     * Get the size of a photo file in bytes.
     * @param photoPath The path to the photo file
     * @return The file size in bytes, or null if file doesn't exist
     */
    suspend fun getPhotoSize(photoPath: String): Long?
}
