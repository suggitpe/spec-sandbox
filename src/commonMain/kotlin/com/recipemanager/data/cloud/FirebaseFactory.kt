package com.recipemanager.data.cloud

/**
 * Factory for creating Firebase service instances
 * Uses expect/actual pattern for platform-specific implementations
 */
expect class FirebaseFactory {
    
    /**
     * Initialize Firebase with configuration
     */
    fun initialize(config: FirebaseConfig)
    
    /**
     * Create Firebase Auth instance
     */
    fun createAuth(): FirebaseAuth
    
    /**
     * Create Firebase Storage instance
     */
    fun createStorage(): FirebaseStorage
    
    /**
     * Create Firebase Firestore instance
     */
    fun createFirestore(): FirebaseFirestore
}