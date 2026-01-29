package com.recipemanager.data.cloud

import kotlinx.coroutines.flow.Flow

/**
 * Firebase Authentication interface for user management
 */
interface FirebaseAuth {
    
    /**
     * Current authenticated user
     */
    val currentUser: Flow<FirebaseUser?>
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    
    /**
     * Create user with email and password
     */
    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    
    /**
     * Sign in anonymously for guest access
     */
    suspend fun signInAnonymously(): Result<FirebaseUser>
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Get current authentication token
     */
    suspend fun getIdToken(): Result<String>
}

/**
 * Firebase user representation
 */
data class FirebaseUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean,
    val displayName: String? = null
)