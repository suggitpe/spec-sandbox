package com.recipemanager.data.cloud

import com.recipemanager.domain.model.Recipe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JVM implementation of Firebase Firestore using REST API
 */
class FirebaseFirestoreImpl(
    private val config: FirebaseConfig,
    private val httpClient: HttpClient
) : FirebaseFirestore {
    
    private val baseUrl = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents"
    
    override suspend fun saveRecipe(recipe: Recipe, userId: String): Result<Unit> {
        return try {
            val documentPath = "$baseUrl/users/$userId/recipes/${recipe.id}"
            val firestoreDocument = convertRecipeToFirestoreDocument(recipe)
            
            val response = httpClient.patch(documentPath) {
                contentType(ContentType.Application.Json)
                setBody(firestoreDocument)
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Recipe save failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecipe(recipeId: String, userId: String): Result<Recipe?> {
        return try {
            val documentPath = "$baseUrl/users/$userId/recipes/$recipeId"
            
            val response = httpClient.get(documentPath)
            
            if (response.status.isSuccess()) {
                val firestoreDocument = Json.decodeFromString<FirestoreDocument>(response.bodyAsText())
                val recipe = convertFirestoreDocumentToRecipe(firestoreDocument)
                Result.success(recipe)
            } else if (response.status == HttpStatusCode.NotFound) {
                Result.success(null)
            } else {
                Result.failure(Exception("Recipe retrieval failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getUserRecipes(userId: String): Flow<List<Recipe>> = flow {
        try {
            val collectionPath = "$baseUrl/users/$userId/recipes"
            
            val response = httpClient.get(collectionPath)
            
            if (response.status.isSuccess()) {
                val queryResponse = Json.decodeFromString<QueryResponse>(response.bodyAsText())
                val recipes = queryResponse.documents?.mapNotNull { doc ->
                    try {
                        convertFirestoreDocumentToRecipe(doc)
                    } catch (e: Exception) {
                        null // Skip invalid documents
                    }
                } ?: emptyList()
                
                emit(recipes)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    override suspend fun deleteRecipe(recipeId: String, userId: String): Result<Unit> {
        return try {
            val documentPath = "$baseUrl/users/$userId/recipes/$recipeId"
            
            val response = httpClient.delete(documentPath)
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Recipe deletion failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun convertRecipeToFirestoreDocument(recipe: Recipe): FirestoreDocument {
        return FirestoreDocument(
            fields = mapOf(
                "id" to FirestoreValue.StringValue(recipe.id),
                "title" to FirestoreValue.StringValue(recipe.title),
                "description" to FirestoreValue.StringValue(recipe.description ?: ""),
                "preparationTime" to FirestoreValue.IntegerValue(recipe.preparationTime.toString()),
                "cookingTime" to FirestoreValue.IntegerValue(recipe.cookingTime.toString()),
                "servings" to FirestoreValue.IntegerValue(recipe.servings.toString()),
                "tags" to FirestoreValue.ArrayValue(
                    FirestoreArrayValue(recipe.tags.map { FirestoreValue.StringValue(it) })
                ),
                "createdAt" to FirestoreValue.TimestampValue(recipe.createdAt.toString()),
                "updatedAt" to FirestoreValue.TimestampValue(recipe.updatedAt.toString()),
                "version" to FirestoreValue.IntegerValue(recipe.version.toString()),
                "parentRecipeId" to FirestoreValue.StringValue(recipe.parentRecipeId ?: "")
            )
        )
    }
    
    private fun convertFirestoreDocumentToRecipe(document: FirestoreDocument): Recipe {
        // This is a simplified conversion - in a real implementation,
        // you would need to properly handle all the Recipe fields including ingredients and steps
        // For now, returning a basic Recipe structure
        val fields = document.fields
        return Recipe(
            id = (fields["id"] as? FirestoreValue.StringValue)?.stringValue ?: "",
            title = (fields["title"] as? FirestoreValue.StringValue)?.stringValue ?: "",
            description = (fields["description"] as? FirestoreValue.StringValue)?.stringValue?.takeIf { it.isNotEmpty() },
            ingredients = emptyList(), // TODO: Implement proper conversion
            steps = emptyList(), // TODO: Implement proper conversion
            preparationTime = (fields["preparationTime"] as? FirestoreValue.IntegerValue)?.integerValue?.toIntOrNull() ?: 0,
            cookingTime = (fields["cookingTime"] as? FirestoreValue.IntegerValue)?.integerValue?.toIntOrNull() ?: 0,
            servings = (fields["servings"] as? FirestoreValue.IntegerValue)?.integerValue?.toIntOrNull() ?: 1,
            tags = emptyList(), // TODO: Implement proper conversion
            createdAt = kotlinx.datetime.Clock.System.now(), // TODO: Parse timestamp
            updatedAt = kotlinx.datetime.Clock.System.now(), // TODO: Parse timestamp
            version = (fields["version"] as? FirestoreValue.IntegerValue)?.integerValue?.toIntOrNull() ?: 1,
            parentRecipeId = (fields["parentRecipeId"] as? FirestoreValue.StringValue)?.stringValue?.takeIf { it.isNotEmpty() }
        )
    }
}

@Serializable
private data class FirestoreDocument(
    val fields: Map<String, FirestoreValue>
)

@Serializable
private sealed class FirestoreValue {
    @Serializable
    data class StringValue(val stringValue: String) : FirestoreValue()
    
    @Serializable
    data class IntegerValue(val integerValue: String) : FirestoreValue()
    
    @Serializable
    data class TimestampValue(val timestampValue: String) : FirestoreValue()
    
    @Serializable
    data class ArrayValue(val arrayValue: FirestoreArrayValue) : FirestoreValue()
}

@Serializable
private data class FirestoreArrayValue(
    val values: List<FirestoreValue>
)

@Serializable
private data class QueryResponse(
    val documents: List<FirestoreDocument>? = null
)