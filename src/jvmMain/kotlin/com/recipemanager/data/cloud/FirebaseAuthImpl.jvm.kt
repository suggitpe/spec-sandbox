package com.recipemanager.data.cloud

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JVM implementation of Firebase Auth using REST API
 */
class FirebaseAuthImpl(
    private val config: FirebaseConfig,
    private val httpClient: HttpClient
) : FirebaseAuth {
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    override val currentUser: Flow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private var idToken: String? = null
    
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val response = httpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword") {
                parameter("key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(SignInRequest(email, password, true))
            }
            
            if (response.status.isSuccess()) {
                val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())
                val user = FirebaseUser(
                    uid = authResponse.localId,
                    email = authResponse.email,
                    isAnonymous = false
                )
                idToken = authResponse.idToken
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Authentication failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val response = httpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signUp") {
                parameter("key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(SignUpRequest(email, password, true))
            }
            
            if (response.status.isSuccess()) {
                val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())
                val user = FirebaseUser(
                    uid = authResponse.localId,
                    email = authResponse.email,
                    isAnonymous = false
                )
                idToken = authResponse.idToken
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("User creation failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val response = httpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signUp") {
                parameter("key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(AnonymousSignInRequest(true))
            }
            
            if (response.status.isSuccess()) {
                val authResponse = Json.decodeFromString<AuthResponse>(response.bodyAsText())
                val user = FirebaseUser(
                    uid = authResponse.localId,
                    email = null,
                    isAnonymous = true
                )
                idToken = authResponse.idToken
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Anonymous sign-in failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            idToken = null
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getIdToken(): Result<String> {
        return idToken?.let { Result.success(it) } 
            ?: Result.failure(Exception("No authenticated user"))
    }
}
@Serializable
private data class SignInRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class SignUpRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class AnonymousSignInRequest(
    val returnSecureToken: Boolean
)

@Serializable
private data class AuthResponse(
    val localId: String,
    val email: String? = null,
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String
)