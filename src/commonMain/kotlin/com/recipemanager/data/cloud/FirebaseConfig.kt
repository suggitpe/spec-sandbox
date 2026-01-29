package com.recipemanager.data.cloud

import kotlinx.serialization.Serializable

/**
 * Firebase configuration for the Recipe Manager application
 */
@Serializable
data class FirebaseConfig(
    val projectId: String,
    val apiKey: String,
    val authDomain: String,
    val storageBucket: String,
    val messagingSenderId: String,
    val appId: String
) {
    companion object {
        // Default configuration for development
        // In production, this should be loaded from secure configuration
        val DEFAULT = FirebaseConfig(
            projectId = "recipe-manager-dev",
            apiKey = "your-api-key-here",
            authDomain = "recipe-manager-dev.firebaseapp.com",
            storageBucket = "recipe-manager-dev.appspot.com",
            messagingSenderId = "123456789",
            appId = "1:123456789:web:abcdef123456"
        )
    }
}