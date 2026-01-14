package com.recipemanager.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * JVM implementation of PhotoStorage.
 * Handles photo optimization, compression, and file system operations.
 */
class PhotoStorageImpl(
    private val storageDirectory: String = "photos"
) : PhotoStorage {
    
    private val photoDir: File = File(storageDirectory).apply {
        if (!exists()) {
            mkdirs()
        }
    }
    
    /**
     * Save a photo to local storage with optimization.
     * For JVM implementation, we copy the file and simulate optimization.
     * In a real implementation, this would include image compression and resizing.
     */
    override suspend fun savePhoto(sourcePath: String): String = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        
        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Source photo does not exist: $sourcePath")
        }
        
        // Generate unique filename
        val extension = sourceFile.extension.ifEmpty { "jpg" }
        val uniqueFilename = "${UUID.randomUUID()}.$extension"
        val destinationFile = File(photoDir, uniqueFilename)
        
        // Copy file (in real implementation, this would include optimization)
        Files.copy(
            sourceFile.toPath(),
            destinationFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
        
        // Simulate optimization by checking file size
        // In production, this would resize/compress the image
        optimizePhoto(destinationFile)
        
        destinationFile.absolutePath
    }
    
    /**
     * Delete a photo from local storage.
     */
    override suspend fun deletePhoto(photoPath: String): Unit = withContext(Dispatchers.IO) {
        val file = File(photoPath)
        if (file.exists()) {
            file.delete()
        }
    }
    
    /**
     * Check if a photo exists at the given path.
     */
    override suspend fun photoExists(photoPath: String): Boolean = withContext(Dispatchers.IO) {
        File(photoPath).exists()
    }
    
    /**
     * Get the size of a photo file in bytes.
     */
    override suspend fun getPhotoSize(photoPath: String): Long? = withContext(Dispatchers.IO) {
        val file = File(photoPath)
        if (file.exists()) file.length() else null
    }
    
    /**
     * Optimize photo for mobile storage.
     * In a real implementation, this would:
     * - Resize images to reasonable dimensions (e.g., 1920x1080)
     * - Compress JPEG quality to 85%
     * - Convert to efficient formats
     * - Strip unnecessary metadata
     */
    private fun optimizePhoto(file: File) {
        // Placeholder for optimization logic
        // In production, use image processing libraries like:
        // - Skiko for Kotlin Multiplatform
        // - Platform-specific libraries (Android: Bitmap, iOS: UIImage)
        
        // For now, just verify the file is readable
        if (!file.canRead()) {
            throw IllegalStateException("Cannot read photo file: ${file.absolutePath}")
        }
    }
}
