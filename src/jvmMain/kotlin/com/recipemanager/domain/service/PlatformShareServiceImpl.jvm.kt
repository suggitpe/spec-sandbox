package com.recipemanager.domain.service

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * JVM implementation of PlatformShareService.
 * Provides file system and clipboard sharing capabilities.
 */
class PlatformShareServiceImpl : PlatformShareService {
    
    override suspend fun shareRecipe(
        data: String,
        channel: ShareChannel,
        title: String?
    ): Result<Unit> {
        return try {
            when (channel) {
                ShareChannel.FILE_SYSTEM -> {
                    val fileName = title?.let { sanitizeFileName(it) } ?: "recipe"
                    val file = File("$fileName.json")
                    file.writeText(data)
                    Result.success(Unit)
                }
                
                ShareChannel.CLIPBOARD -> {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val selection = StringSelection(data)
                    clipboard.setContents(selection, selection)
                    Result.success(Unit)
                }
                
                else -> {
                    Result.failure(Exception("Sharing channel $channel not supported on JVM platform"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to share recipe: ${e.message}", e))
        }
    }
    
    override fun getAvailableChannels(): List<ShareChannel> {
        return listOf(
            ShareChannel.FILE_SYSTEM,
            ShareChannel.CLIPBOARD
        )
    }
    
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9-_]"), "_")
    }
}
