package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.*
import com.recipemanager.domain.service.RecipeCopyManager
import com.recipemanager.domain.service.ShareService
import com.recipemanager.domain.service.SharedRecipeMetadata
import com.recipemanager.domain.usecase.ImportRecipeUseCase
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.test.generators.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll
import kotlinx.datetime.Clock

/**
 * Property-based tests for recipe sharing functionality.
 * Tests recipe sharing completeness and shared recipe independence.
 */
class RecipeSharingPropertyTest : FunSpec({
    
    test("Property 7: Recipe Sharing Completeness - Feature: recipe-manager, Property 7: For any recipe with associated photos and notes, sharing should transfer all recipe data, photos, and metadata to the recipient") {
        checkAll(100, recipeArb()) { originalRecipe ->
            // Create a fresh database and services for each test iteration
            val validator = RecipeValidator()
            val shareService = ShareService(validator)
            
            // Ensure the recipe has at least one ingredient and step with photos
            val recipeWithPhotos = originalRecipe.copy(
                ingredients = originalRecipe.ingredients.map { ingredient ->
                    ingredient.copy(
                        photos = if (ingredient.photos.isEmpty()) {
                            listOf(photoArb().bind().copy(stage = PhotoStage.RAW_INGREDIENTS))
                        } else {
                            ingredient.photos
                        }
                    )
                },
                steps = originalRecipe.steps.map { step ->
                    step.copy(
                        photos = if (step.photos.isEmpty()) {
                            listOf(photoArb().bind().copy(stage = PhotoStage.COOKING_STEP))
                        } else {
                            step.photos
                        }
                    )
                }
            )
            
            // Export the recipe (simulating sharing)
            val exportResult = shareService.exportRecipe(recipeWithPhotos)
            exportResult.isSuccess shouldBe true
            
            val jsonData = exportResult.getOrNull()
            jsonData.shouldNotBeNull()
            
            // Import the recipe (simulating recipient receiving it)
            val importResult = shareService.importRecipe(jsonData)
            importResult.isSuccess shouldBe true
            
            val importedRecipe = importResult.getOrNull()
            importedRecipe.shouldNotBeNull()
            
            // Verify all recipe data is transferred
            importedRecipe.title shouldBe recipeWithPhotos.title
            importedRecipe.description shouldBe recipeWithPhotos.description
            importedRecipe.preparationTime shouldBe recipeWithPhotos.preparationTime
            importedRecipe.cookingTime shouldBe recipeWithPhotos.cookingTime
            importedRecipe.servings shouldBe recipeWithPhotos.servings
            importedRecipe.tags shouldContainExactly recipeWithPhotos.tags
            
            // Verify all ingredients are transferred
            importedRecipe.ingredients shouldHaveSize recipeWithPhotos.ingredients.size
            importedRecipe.ingredients.forEachIndexed { index, ingredient ->
                val originalIngredient = recipeWithPhotos.ingredients[index]
                ingredient.name shouldBe originalIngredient.name
                ingredient.quantity shouldBe originalIngredient.quantity
                ingredient.unit shouldBe originalIngredient.unit
                ingredient.notes shouldBe originalIngredient.notes
                
                // Verify photos are transferred
                ingredient.photos shouldHaveSize originalIngredient.photos.size
                ingredient.photos.forEachIndexed { photoIndex, photo ->
                    val originalPhoto = originalIngredient.photos[photoIndex]
                    photo.localPath shouldBe originalPhoto.localPath
                    photo.cloudUrl shouldBe originalPhoto.cloudUrl
                    photo.caption shouldBe originalPhoto.caption
                    photo.stage shouldBe originalPhoto.stage
                }
            }
            
            // Verify all cooking steps are transferred
            importedRecipe.steps shouldHaveSize recipeWithPhotos.steps.size
            importedRecipe.steps.forEachIndexed { index, step ->
                val originalStep = recipeWithPhotos.steps[index]
                step.stepNumber shouldBe originalStep.stepNumber
                step.instruction shouldBe originalStep.instruction
                step.duration shouldBe originalStep.duration
                step.temperature shouldBe originalStep.temperature
                step.timerRequired shouldBe originalStep.timerRequired
                
                // Verify photos are transferred
                step.photos shouldHaveSize originalStep.photos.size
                step.photos.forEachIndexed { photoIndex, photo ->
                    val originalPhoto = originalStep.photos[photoIndex]
                    photo.localPath shouldBe originalPhoto.localPath
                    photo.cloudUrl shouldBe originalPhoto.cloudUrl
                    photo.caption shouldBe originalPhoto.caption
                    photo.stage shouldBe originalPhoto.stage
                }
            }
            
            // Verify metadata is transferred
            importedRecipe.version shouldBe recipeWithPhotos.version
            importedRecipe.parentRecipeId shouldBe recipeWithPhotos.parentRecipeId
        }
    }
    
    test("Property 8: Shared Recipe Independence - Feature: recipe-manager, Property 8: For any shared recipe, modifications made by the recipient should not affect the original recipe owned by the sender") {
        checkAll(100, recipeArb()) { originalRecipe ->
            // Create a fresh database and services for each test iteration
            val driverFactory = DatabaseDriverFactory()
            val database = RecipeDatabase(driverFactory.createDriver())
            val repository = RecipeRepositoryImpl(database)
            val validator = RecipeValidator()
            val shareService = ShareService(validator)
            val recipeCopyManager = RecipeCopyManager()
            val importUseCase = ImportRecipeUseCase(shareService, repository, recipeCopyManager)
            
            // Ensure unique IDs for ingredients and steps to avoid constraint violations
            val senderRecipe = originalRecipe.copy(
                id = "sender-${originalRecipe.id}",
                ingredients = originalRecipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "sender-${originalRecipe.id}-ingredient-$index")
                },
                steps = originalRecipe.steps.mapIndexed { index, step ->
                    step.copy(id = "sender-${originalRecipe.id}-step-$index")
                }
            )
            
            // Sender creates and saves the original recipe
            val createResult = repository.createRecipe(senderRecipe)
            createResult.isSuccess shouldBe true
            
            // Export the recipe (simulating sharing)
            val exportResult = shareService.exportRecipe(senderRecipe)
            exportResult.isSuccess shouldBe true
            
            val jsonData = exportResult.getOrNull()
            jsonData.shouldNotBeNull()
            
            // Recipient imports the recipe (creates independent copy)
            val importResult = importUseCase.importRecipe(jsonData, sharedBy = "sender-user")
            importResult.isSuccess shouldBe true
            
            val recipientRecipe = importResult.getOrNull()
            recipientRecipe.shouldNotBeNull()
            
            // Verify the recipient's recipe has a different ID (independence)
            recipientRecipe.id shouldNotBe senderRecipe.id
            
            // Verify the recipient's recipe has no parent link (independent copy)
            recipientRecipe.parentRecipeId shouldBe null
            
            // Verify all ingredient IDs are different (independence)
            recipientRecipe.ingredients.forEachIndexed { index, ingredient ->
                ingredient.id shouldNotBe senderRecipe.ingredients[index].id
            }
            
            // Verify all step IDs are different (independence)
            recipientRecipe.steps.forEachIndexed { index, step ->
                step.id shouldNotBe senderRecipe.steps[index].id
            }
            
            // Recipient modifies their copy
            val modifiedRecipientRecipe = recipientRecipe.copy(
                title = "Modified ${recipientRecipe.title}",
                description = "Modified by recipient",
                preparationTime = recipientRecipe.preparationTime + 30,
                cookingTime = recipientRecipe.cookingTime + 45,
                servings = recipientRecipe.servings + 5,
                updatedAt = Clock.System.now(),
                version = recipientRecipe.version + 1
            )
            
            val updateResult = repository.updateRecipe(modifiedRecipientRecipe)
            updateResult.isSuccess shouldBe true
            
            // Retrieve the sender's original recipe
            val senderRetrieveResult = repository.getRecipe(senderRecipe.id)
            senderRetrieveResult.isSuccess shouldBe true
            
            val retrievedSenderRecipe = senderRetrieveResult.getOrNull()
            retrievedSenderRecipe.shouldNotBeNull()
            
            // Verify the sender's recipe is unchanged
            retrievedSenderRecipe.id shouldBe senderRecipe.id
            retrievedSenderRecipe.title shouldBe senderRecipe.title
            retrievedSenderRecipe.description shouldBe senderRecipe.description
            retrievedSenderRecipe.preparationTime shouldBe senderRecipe.preparationTime
            retrievedSenderRecipe.cookingTime shouldBe senderRecipe.cookingTime
            retrievedSenderRecipe.servings shouldBe senderRecipe.servings
            retrievedSenderRecipe.version shouldBe senderRecipe.version
            
            // Verify the recipient's recipe has the modifications
            val recipientRetrieveResult = repository.getRecipe(recipientRecipe.id)
            recipientRetrieveResult.isSuccess shouldBe true
            
            val retrievedRecipientRecipe = recipientRetrieveResult.getOrNull()
            retrievedRecipientRecipe.shouldNotBeNull()
            retrievedRecipientRecipe.title shouldBe modifiedRecipientRecipe.title
            retrievedRecipientRecipe.description shouldBe modifiedRecipientRecipe.description
            retrievedRecipientRecipe.preparationTime shouldBe modifiedRecipientRecipe.preparationTime
            retrievedRecipientRecipe.cookingTime shouldBe modifiedRecipientRecipe.cookingTime
            retrievedRecipientRecipe.servings shouldBe modifiedRecipientRecipe.servings
            
            // Verify both recipes exist independently in the database
            val allRecipesResult = repository.getAllRecipes()
            allRecipesResult.isSuccess shouldBe true
            
            val allRecipes = allRecipesResult.getOrNull()
            allRecipes.shouldNotBeNull()
            allRecipes.any { it.id == senderRecipe.id } shouldBe true
            allRecipes.any { it.id == recipientRecipe.id } shouldBe true
        }
    }
})
