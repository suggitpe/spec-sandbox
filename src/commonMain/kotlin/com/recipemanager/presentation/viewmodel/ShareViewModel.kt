package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.PlatformShareService
import com.recipemanager.domain.service.ShareChannel
import com.recipemanager.domain.service.ShareService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShareState(
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
    private val recipeId: String,
    private val recipeRepository: RecipeRepository,
    private val shareService: ShareService,
    private val platformShareService: PlatformShareService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(ShareState())
    val state: StateFlow<ShareState> = _state.asStateFlow()

    init {
        loadRecipe()
        loadAvailableChannels()
    }

    private fun loadRecipe() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    if (recipe != null) {
                        _state.value = _state.value.copy(recipe = recipe)
                        exportRecipe(recipe)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Recipe not found"
                        )
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipe"
                    )
                }
        }
    }

    private fun exportRecipe(recipe: Recipe) {
        shareService.exportRecipe(recipe)
            .onSuccess { jsonData ->
                _state.value = _state.value.copy(
                    exportedData = jsonData,
                    isLoading = false
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to export recipe"
                )
            }
    }

    private fun loadAvailableChannels() {
        val channels = platformShareService.getAvailableChannels()
        _state.value = _state.value.copy(
            availableChannels = channels,
            selectedChannel = channels.firstOrNull()
        )
    }

    fun selectChannel(channel: ShareChannel) {
        _state.value = _state.value.copy(selectedChannel = channel)
    }

    fun shareRecipe() {
        val data = _state.value.exportedData ?: return
        val channel = _state.value.selectedChannel ?: return
        val recipe = _state.value.recipe ?: return
        
        scope.launch {
            _state.value = _state.value.copy(isSharing = true, error = null)
            
            platformShareService.shareRecipe(
                data = data,
                channel = channel,
                title = "Share Recipe: ${recipe.title}"
            )
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSharing = false,
                        shareSuccess = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSharing = false,
                        error = error.message ?: "Failed to share recipe"
                    )
                }
        }
    }

    fun copyToClipboard() {
        val data = _state.value.exportedData ?: return
        
        scope.launch {
            platformShareService.shareRecipe(
                data = data,
                channel = ShareChannel.CLIPBOARD,
                title = "Recipe Data"
            )
                .onSuccess {
                    _state.value = _state.value.copy(
                        shareSuccess = true,
                        error = null
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to copy to clipboard"
                    )
                }
        }
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(shareSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
