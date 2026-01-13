package com.recipemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RecipeCollection(
    val id: String,
    val name: String,
    val description: String? = null,
    val recipeIds: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)