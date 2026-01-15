package com.recipemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a version entry in the recipe version history.
 * Tracks metadata about recipe upgrades and modifications.
 */
@Serializable
data class RecipeVersion(
    val id: String,
    val recipeId: String,
    val version: Int,
    val parentRecipeId: String?,
    val upgradeNotes: String?,
    val createdAt: Instant,
    val createdBy: String? = null // For future multi-user support
)
