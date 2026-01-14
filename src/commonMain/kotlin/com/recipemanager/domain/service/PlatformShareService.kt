package com.recipemanager.domain.service

/**
 * Enum representing different sharing channels available on the platform.
 */
enum class ShareChannel {
    DIRECT_MESSAGE,
    EMAIL,
    SOCIAL_MEDIA,
    FILE_SYSTEM,
    CLIPBOARD
}

/**
 * Platform-specific interface for sharing recipes through various channels.
 * Implementations should be provided for each platform (Android, iOS, JVM).
 */
interface PlatformShareService {
    /**
     * Shares recipe data through the specified channel.
     * 
     * @param data The recipe data to share (typically JSON string)
     * @param channel The sharing channel to use
     * @param title Optional title for the share action
     * @return Result indicating success or failure
     */
    suspend fun shareRecipe(
        data: String,
        channel: ShareChannel,
        title: String? = null
    ): Result<Unit>

    /**
     * Gets the list of available sharing channels on the current platform.
     * 
     * @return List of available sharing channels
     */
    fun getAvailableChannels(): List<ShareChannel>
}
