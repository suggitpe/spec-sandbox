package com.recipemanager.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String? = null,
    val ingredients: List<Ingredient>,
    val steps: List<CookingStep>,
    val preparationTime: Int, // in minutes
    val cookingTime: Int, // in minutes
    val servings: Int,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int = 1,
    val parentRecipeId: String? = null // For recipe upgrades
)