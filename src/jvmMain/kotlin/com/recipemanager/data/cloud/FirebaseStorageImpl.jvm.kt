package com.recipemanager.data.cloud

import com.recipemanager.domain.model.Photo
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

/**
 * JVM implementation of Firebase Storage using REST API
 */
class FirebaseStorageImpl(
    private val config: FirebaseConfig,
    private val httpClient: HttpClient
) : FirebaseStorage {
    
    override suspend fun uploadPhoto(photo: Photo, data: ByteArray): Result<String> {
        return try {
            val storagePath = "photos/${photo.id}/${UUID.randomUUID()}.jpg"
            val uploadUrl = "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o"
            
            val response = httpClient.post("$uploadUrl?name=$storagePath") {
                contentType(ContentType.Image.JPEG)
                setBody(data)
            }
            
            if (response.status.isSuccess()) {
                val uploadResponse = Json.decodeFromString<UploadResponse>(response.bodyAsText())
                val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/${uploadResponse.name}?alt=media"
                Result.success(downloadUrl)
            } else {
                Result.failure(Exception("Photo upload failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun downloadPhoto(cloudUrl: String): Result<ByteArray> {
        return try {
            val response = httpClient.get(cloudUrl)
            
            if (response.status.isSuccess()) {
                Result.success(response.readBytes())
            } else {
                Result.failure(Exception("Photo download failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deletePhoto(cloudUrl: String): Result<Unit> {
        return try {
            // Extract storage path from URL
            val storagePath = extractStoragePathFromUrl(cloudUrl)
            val deleteUrl = "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/$storagePath"
            
            val response = httpClient.delete(deleteUrl)
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Photo deletion failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDownloadUrl(storagePath: String): Result<String> {
        return try {
            val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/$storagePath?alt=media"
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractStoragePathFromUrl(cloudUrl: String): String {
        // Extract storage path from Firebase Storage URL
        // Format: https://firebasestorage.googleapis.com/v0/b/bucket/o/path?alt=media
        val regex = Regex("https://firebasestorage\\.googleapis\\.com/v0/b/[^/]+/o/([^?]+)")
        return regex.find(cloudUrl)?.groupValues?.get(1) ?: throw IllegalArgumentException("Invalid cloud URL")
    }
}

@Serializable
private data class UploadResponse(
    val name: String,
    val bucket: String,
    val generation: String,
    val metageneration: String,
    val contentType: String,
    val timeCreated: String,
    val updated: String,
    val size: String
)