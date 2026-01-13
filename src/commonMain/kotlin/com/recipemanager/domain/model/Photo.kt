package com.recipemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val id: String,
    val localPath: String,
    val cloudUrl: String? = null,
    val caption: String? = null,
    val stage: PhotoStage,
    val timestamp: Instant,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
)

@Serializable
enum class PhotoStage {
    RAW_INGREDIENTS,
    PROCESSED_INGREDIENTS,
    COOKING_STEP,
    FINAL_RESULT
}

@Serializable
enum class SyncStatus {
    LOCAL_ONLY,
    SYNCING,
    SYNCED,
    SYNC_FAILED
}