package com.recipemanager.presentation.navigation

import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * JVM implementation of StatePersistence using file system storage.
 * Implements Requirements 7.5 (state preservation) and 8.1, 8.2 (data persistence).
 */
class JvmStatePersistence(
    private val storageDirectory: String = System.getProperty("user.home") + "/.recipemanager/state"
) : StatePersistence {
    
    init {
        // Ensure storage directory exists
        val dir = File(storageDirectory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
    
    override suspend fun saveState(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(storageDirectory, "$key.state")
            file.writeText(value)
        } catch (e: IOException) {
            // Log error but don't crash the app
            println("Failed to save state for key '$key': ${e.message}")
        }
    }
    
    override suspend fun loadState(key: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(storageDirectory, "$key.state")
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            // Log error and return null
            println("Failed to load state for key '$key': ${e.message}")
            null
        }
    }
    
    override suspend fun clearState(key: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(storageDirectory, "$key.state")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: IOException) {
            // Log error but don't crash the app
            println("Failed to clear state for key '$key': ${e.message}")
        }
    }
    
    /**
     * Clear all persisted state files.
     */
    suspend fun clearAllState() = withContext(Dispatchers.IO) {
        try {
            val dir = File(storageDirectory)
            if (dir.exists()) {
                dir.listFiles { _, name -> name.endsWith(".state") }?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: IOException) {
            println("Failed to clear all state: ${e.message}")
        }
    }
    
    /**
     * Get the size of all state files in bytes.
     */
    suspend fun getStateStorageSize(): Long = withContext(Dispatchers.IO) {
        try {
            val dir = File(storageDirectory)
            if (dir.exists()) {
                dir.listFiles { _, name -> name.endsWith(".state") }
                    ?.sumOf { it.length() } ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}