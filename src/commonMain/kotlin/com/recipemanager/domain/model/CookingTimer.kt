package com.recipemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CookingTimer(
    val id: String,
    val recipeId: String,
    val stepId: String,
    val duration: Int, // in seconds
    val remainingTime: Int,
    val status: TimerStatus,
    val createdAt: Instant
)

@Serializable
enum class TimerStatus {
    READY,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}