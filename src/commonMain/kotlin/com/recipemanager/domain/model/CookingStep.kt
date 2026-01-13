package com.recipemanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CookingStep(
    val id: String,
    val stepNumber: Int,
    val instruction: String,
    val duration: Int? = null, // in minutes
    val temperature: Int? = null,
    val photos: List<Photo> = emptyList(),
    val timerRequired: Boolean = false
)