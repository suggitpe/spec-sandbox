package com.recipemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a complete snapshot of a recipe at a specific version.
 * Stores the full recipe data to enable complete rollback functionality.
 */
@Serializable
data class RecipeSnapshot(
    val id: String,
    val versionId: String,
    val recipe: Recipe,
    val createdAt: Instant
)
