package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.PlatformShareService
import com.recipemanager.domain.service.ShareChannel
import com.recipemanager.domain.service.ShareService
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ShareState(
    val recipeId: String? = null,
    val recipe: Recipe? = null,
    val exportedData: String? = null,
    val availableChannels: List<ShareChannel> = emptyList(),
    val selectedChannel: ShareChannel? = null,
    val isLoading: Boolean = false,
    val isSharing: Boolean = false,
    val shareSuccess: Boolean = false,
    val error: String? = null
)

class ShareViewModel(
    private val recipeRepository: RecipeRepository,
    private val shareService: ShareService,
    private val platformShareService: PlatformShareService,
    statePersistence: StatePersistence? = null
) : BaseViewModel<ShareState>(
    initialState = ShareState(),
    statePersistence = statePersistence,
    stateKey = "share"
) {
    
    override fun onInitialize() {
        loadAvailableChannels()
        
        // If we have a recipe ID in restored state, load it
        currentState.recipeId?.let { recipeId ->
            loadRecipe(recipeId)
        }
    }
    
    fun setRecipeId(recipeId: String) {
        currentState = currentState.copy(recipeId = recipeId)
        loadRecipe(recipeId)
    }

    private fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    if (recipe != null) {
                        currentState = currentState.copy(recipe = recipe)
                        exportRecipe(recipe)
                    } else {
                        setError("Recipe not found")
                        setLoading(false)
                    }
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load recipe")
                    setLoading(false)
                }
        }
    }

    private fun exportRecipe(recipe: Recipe) {
        shareService.exportRecipe(recipe)
            .onSuccess { jsonData ->
                currentState = currentState.copy(
                    exportedData = jsonData,
                    isLoading = false
                )
                setLoading(false)
            }
            .onFailure { error ->
                setError(error.message ?: "Failed to export recipe")
                setLoading(false)
            }
    }

    private fun loadAvailableChannels() {
        val channels = platformShareService.getAvailableChannels()
        currentState = currentState.copy(
            availableChannels = channels,
            selectedChannel = channels.firstOrNull()
        )
    }

    fun selectChannel(channel: ShareChannel) {
        currentState = currentState.copy(selectedChannel = channel)
    }

    fun shareRecipe() {
        val data = currentState.exportedData ?: return
        val channel = currentState.selectedChannel ?: return
        val recipe = currentState.recipe ?: return
        
        viewModelScope.launch {
            currentState = currentState.copy(isSharing = true)
            setError(null)
            
            platformShareService.shareRecipe(
                data = data,
                channel = channel,
                title = "Share Recipe: ${recipe.title}"
            )
                .onSuccess {
                    currentState = currentState.copy(
                        isSharing = false,
                        shareSuccess = true
                    )
                }
                .onFailure { error ->
                    currentState = currentState.copy(isSharing = false)
                    setError(error.message ?: "Failed to share recipe")
                }
        }
    }

    fun copyToClipboard() {
        val data = currentState.exportedData ?: return
        
        viewModelScope.launch {
            platformShareService.shareRecipe(
                data = data,
                channel = ShareChannel.CLIPBOARD,
                title = "Recipe Data"
            )
                .onSuccess {
                    currentState = currentState.copy(shareSuccess = true)
                    setError(null)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to copy to clipboard")
                }
        }
    }

    fun clearSuccess() {
        currentState = currentState.copy(shareSuccess = false)
    }
    
    override fun serializeState(state: ShareState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): ShareState? {
        return try {
            Json.decodeFromString<ShareState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
