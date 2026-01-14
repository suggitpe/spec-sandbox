package com.recipemanager.domain.service

import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.domain.model.Recipe
import kotlinx.datetime.Clock

/**
 * Metadata for tracking shared recipe information.
 */
data class SharedRecipeMetadata(
    val originalRecipeId: String,
    val sharedAt: kotlinx.datetime.Instant,
    val sharedBy: String? = null,
    val shareNote: String? = null
)

/**
 * Manager for creating independent copies of recipes.
 * Ensures that shared recipes are completely independent from their originals.
 */
class RecipeCopyManager {
    
    /**
     * Creates an independent copy of a recipe with new IDs for all components.
     * The copied recipe will have:
     * - New recipe ID
     * - New IDs for all ingredients
     * - New IDs for all cooking steps
     * - New creation and update timestamps
     * - Version reset to 1
     * - No parent recipe link (independent copy)
     * 
     * @param original The original recipe to copy
     * @param metadata Optional metadata about the sharing context
     * @return The independent copy of the recipe
     */
    fun createIndependentCopy(
        original: Recipe,
        metadata: SharedRecipeMetadata? = null
    ): Recipe {
        val now = Clock.System.now()
        
        // Create new ingredients with new IDs
        val copiedIngredients = original.ingredients.map { ingredient ->
            ingredient.copy(
                id = generateNewId(),
                // Photos are copied by reference - they remain the same
                photos = ingredient.photos.map { photo ->
                    photo.copy(id = generateNewId())
                }
            )
        }
        
        // Create new cooking steps with new IDs
        val copiedSteps = original.steps.map { step ->
            step.copy(
                id = generateNewId(),
                // Photos are copied by reference - they remain the same
                photos = step.photos.map { photo ->
                    photo.copy(id = generateNewId())
                }
            )
        }
        
        // Create description with share note if provided
        val description = if (metadata?.shareNote != null) {
            val originalDesc = original.description ?: ""
            if (originalDesc.isNotEmpty()) {
                "$originalDesc\n\nShared: ${metadata.shareNote}"
            } else {
                "Shared: ${metadata.shareNote}"
            }
        } else {
            original.description
        }
        
        // Create the independent copy
        return original.copy(
            id = generateNewId(),
            description = description,
            ingredients = copiedIngredients,
            steps = copiedSteps,
            createdAt = now,
            updatedAt = now,
            version = 1,
            parentRecipeId = null // Independent copy has no parent
        )
    }

    /**
     * Creates a copy of a recipe for upgrading/modifying.
     * Unlike independent copies, this maintains a link to the parent recipe.
     * 
     * @param original The original recipe to copy
     * @param upgradeNote Optional note explaining the upgrade
     * @return The upgraded copy of the recipe
     */
    fun createUpgradeCopy(
        original: Recipe,
        upgradeNote: String? = null
    ): Recipe {
        val now = Clock.System.now()
        
        // Create new ingredients with new IDs
        val copiedIngredients = original.ingredients.map { ingredient ->
            ingredient.copy(
                id = generateNewId(),
                photos = ingredient.photos.map { photo ->
                    photo.copy(id = generateNewId())
                }
            )
        }
        
        // Create new cooking steps with new IDs
        val copiedSteps = original.steps.map { step ->
            step.copy(
                id = generateNewId(),
                photos = step.photos.map { photo ->
                    photo.copy(id = generateNewId())
                }
            )
        }
        
        // Create description with upgrade note if provided
        val description = if (upgradeNote != null) {
            val originalDesc = original.description ?: ""
            if (originalDesc.isNotEmpty()) {
                "$originalDesc\n\nUpgrade: $upgradeNote"
            } else {
                "Upgrade: $upgradeNote"
            }
        } else {
            original.description
        }
        
        // Create the upgrade copy with parent link
        return original.copy(
            id = generateNewId(),
            description = description,
            ingredients = copiedIngredients,
            steps = copiedSteps,
            createdAt = now,
            updatedAt = now,
            version = original.version + 1,
            parentRecipeId = original.id // Maintains link to parent
        )
    }

    /**
     * Checks if a recipe is an independent copy (no parent link).
     * 
     * @param recipe The recipe to check
     * @return True if the recipe is independent, false otherwise
     */
    fun isIndependentCopy(recipe: Recipe): Boolean {
        return recipe.parentRecipeId == null
    }

    /**
     * Checks if a recipe is an upgraded version (has parent link).
     * 
     * @param recipe The recipe to check
     * @return True if the recipe is an upgrade, false otherwise
     */
    fun isUpgradedVersion(recipe: Recipe): Boolean {
        return recipe.parentRecipeId != null
    }

    private fun generateNewId(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
