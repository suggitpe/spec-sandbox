package com.recipemanager.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * JVM implementation of PhotoCaptureProvider.
 * This is a mock implementation for testing purposes.
 * In production Android/iOS, this would use platform-specific camera APIs.
 */
class PhotoCaptureProviderImpl(
    private val tempDirectory: String = "temp_photos"
) : PhotoCaptureProvider {
    
    private val tempDir: File = File(tempDirectory).apply {
        if (!exists()) {
            mkdirs()
        }
    }
    
    /**
     * Mock photo capture - creates a temporary file.
     * In production, this would launch the camera and return the captured photo path.
     */
    override suspend fun capturePhoto(): String = withContext(Dispatchers.IO) {
        // Create a mock photo file
        val photoFile = File(tempDir, "captured_${UUID.randomUUID()}.jpg")
        
        // Write some mock data to simulate a photo
        Files.write(photoFile.toPath(), "MOCK_PHOTO_DATA".toByteArray())
        
        photoFile.absolutePath
    }
    
    /**
     * Mock gallery selection - creates a temporary file.
     * In production, this would open the gallery picker and return the selected photo path.
     */
    override suspend fun selectFromGallery(): String = withContext(Dispatchers.IO) {
        // Create a mock photo file
        val photoFile = File(tempDir, "gallery_${UUID.randomUUID()}.jpg")
        
        // Write some mock data to simulate a photo
        Files.write(photoFile.toPath(), "MOCK_GALLERY_PHOTO_DATA".toByteArray())
        
        photoFile.absolutePath
    }
}
