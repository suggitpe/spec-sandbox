package com.recipemanager.data.cloud

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * JVM implementation of FirebaseFactory using REST APIs
 */
actual class FirebaseFactory {
    
    private var config: FirebaseConfig? = null
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    actual fun initialize(config: FirebaseConfig) {
        this.config = config
    }
    
    actual fun createAuth(): FirebaseAuth {
        requireNotNull(config) { "Firebase must be initialized before creating services" }
        return FirebaseAuthImpl(config!!, httpClient)
    }
    
    actual fun createStorage(): FirebaseStorage {
        requireNotNull(config) { "Firebase must be initialized before creating services" }
        return FirebaseStorageImpl(config!!, httpClient)
    }
    
    actual fun createFirestore(): FirebaseFirestore {
        requireNotNull(config) { "Firebase must be initialized before creating services" }
        return FirebaseFirestoreImpl(config!!, httpClient)
    }
}