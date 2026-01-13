package com.recipemanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val notes: String? = null,
    val photos: List<Photo> = emptyList()
)