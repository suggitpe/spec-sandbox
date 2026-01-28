package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeCollection
import com.recipemanager.domain.repository.CollectionRepository
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CollectionDetailState(
    val collectionId: String? = null,
    val collection: RecipeCollection? = null,
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val lastLoadTime: Long = 0L
)

class CollectionDetailViewModel(
    private val collectionRepository: CollectionRepository,
    statePersistence: StatePersistence? = null
) : BaseViewModel<CollectionDetailState>(
    initialState = CollectionDetailState(),
    statePersistence = statePersistence,
    stateKey = "collection_detail"
) {
    
    override fun onInitialize() {
        // If we have a collection ID and haven't loaded recently, load it
        if (currentState.collectionId != null) {
            val shouldRefresh = currentState.collection == null || 
                (System.currentTimeMillis() - currentState.lastLoadTime) > 60_000 // 1 minute
            
            if (shouldRefresh) {
                loadCollection(currentState.collectionId!!)
            }
        }
    }
    
    fun setCollectionId(collectionId: String) {
        currentState = currentState.copy(collectionId = collectionId)
        loadCollection(collectionId)
    }

    fun loadCollection(collectionId: String) {
        currentState = currentState.copy(collectionId = collectionId)
        
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            collectionRepository.getCollection(collectionId)
                .onSuccess { collection ->
                    if (collection != null) {
                        currentState = currentState.copy(
                            collection = collection,
                            editName = collection.name,
                            editDescription = collection.description ?: "",
                            lastLoadTime = System.currentTimeMillis()
                        )
                        loadRecipes(collectionId)
                    } else {
                        setError("Collection not found")
                        setLoading(false)
                    }
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load collection")
                    setLoading(false)
                }
        }
    }

    private fun loadRecipes(collectionId: String) {
        viewModelScope.launch {
            collectionRepository.getRecipesInCollection(collectionId)
                .onSuccess { recipes ->
                    currentState = currentState.copy(
                        recipes = recipes,
                        isLoading = false
                    )
                    setLoading(false)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load recipes")
                    setLoading(false)
                }
        }
    }

    fun startEditing() {
        currentState = currentState.copy(isEditing = true)
    }

    fun cancelEditing() {
        val collection = currentState.collection
        currentState = currentState.copy(
            isEditing = false,
            editName = collection?.name ?: "",
            editDescription = collection?.description ?: ""
        )
    }

    fun updateName(name: String) {
        currentState = currentState.copy(editName = name)
    }

    fun updateDescription(description: String) {
        currentState = currentState.copy(editDescription = description)
    }

    fun saveChanges() {
        val collection = currentState.collection ?: return
        
        viewModelScope.launch {
            val updatedCollection = collection.copy(
                name = currentState.editName,
                description = currentState.editDescription.takeIf { it.isNotBlank() },
                updatedAt = Clock.System.now()
            )
            
            collectionRepository.updateCollection(updatedCollection)
                .onSuccess {
                    currentState = currentState.copy(
                        collection = updatedCollection,
                        isEditing = false
                    )
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to update collection")
                }
        }
    }

    fun removeRecipe(recipeId: String) {
        val collectionId = currentState.collectionId ?: return
        
        viewModelScope.launch {
            collectionRepository.removeRecipeFromCollection(recipeId, collectionId)
                .onSuccess {
                    loadCollection(collectionId)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to remove recipe")
                }
        }
    }
    
    fun refreshCollection() {
        currentState.collectionId?.let { collectionId ->
            loadCollection(collectionId)
        }
    }
    
    override fun onAppResumed() {
        // Refresh collection when app comes back to foreground
        refreshCollection()
    }
    
    override fun serializeState(state: CollectionDetailState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): CollectionDetailState? {
        return try {
            Json.decodeFromString<CollectionDetailState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
