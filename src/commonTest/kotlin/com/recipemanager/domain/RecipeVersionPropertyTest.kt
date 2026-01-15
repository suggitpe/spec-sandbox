package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.data.repository.RecipeSnapshotRepositoryImpl
import com.recipemanager.data.repository.RecipeVersionRepositoryImpl
import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.service.RecipeVersionManager
import com.recipemanager.test.generators.recipeArb
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for recipe versioning functionality.
 * Validates Requirements 4.1, 4.4, 4.5.
 */
class RecipeVersionPropertyTest : FunSpec({
    
    test("Property 9: Recipe Version Linking - Feature: recipe-manager, Property 9: For any recipe upgrade, the system should maintain a traceable link to the parent recipe while treating the upgrade as an independent entity") {
        checkAll(100, recipeArb(), Arb.string(0..200)) { originalRecipe, upgradeNotes ->
            // Create a fresh database for each test iteration
            val databaseDriverFactory = DatabaseDriverFactory()
            val databaseManager = DatabaseManager(databaseDriverFactory)
            val database = databaseManager.initialize()
            
            val recipeRepository = RecipeRepositoryImpl(database)
            val versionRepository = RecipeVersionRepositoryImpl(database)
            val snapshotRepository = RecipeSnapshotRepositoryImpl(database)
            
            val versionManager = RecipeVersionManager(
                recipeRepository,
                versionRepository,
                snapshotRepository
            )
            
            // Ensure unique IDs for ingredients and steps to avoid constraint violations
            val parentRecipe = originalRecipe.copy(
                version = 1,
                parentRecipeId = null,
                ingredients = originalRecipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "${originalRecipe.id}-ingredient-$index")
                },
                steps = originalRecipe.steps.mapIndexed { index, step ->
                    step.copy(id = "${originalRecipe.id}-step-$index")
                }
            )
            
            // Create the parent recipe
            val createResult = recipeRepository.createRecipe(parentRecipe)
            createResult.isSuccess shouldBe true
            
            // Create initial version and snapshot
            val initialVersionResult = versionManager.createInitialVersion(parentRecipe)
            initialVersionResult.isSuccess shouldBe true
            
            // Create an upgraded version
            val upgradeResult = versionManager.upgradeRecipe(
                parentRecipe = parentRecipe,
                upgradeNotes = upgradeNotes
            ) { recipe ->
                recipe.copy(
                    title = "Upgraded ${recipe.title}",
                    servings = recipe.servings + 1
                )
            }
            
            upgradeResult.isSuccess shouldBe true
            val upgradedRecipe = upgradeResult.getOrThrow()
            
            // Verify the upgraded recipe maintains a link to the parent
            upgradedRecipe.parentRecipeId shouldBe parentRecipe.id
            
            // Verify the upgraded recipe is independent (has its own ID)
            upgradedRecipe.id shouldNotBe parentRecipe.id
            
            // Verify version is incremented
            upgradedRecipe.version shouldBe parentRecipe.version + 1
            
            // Verify the parent recipe can be retrieved through the link
            val parentRetrievalResult = versionManager.getParentRecipe(upgradedRecipe)
            parentRetrievalResult.isSuccess shouldBe true
            
            val retrievedParent = parentRetrievalResult.getOrNull()
            retrievedParent.shouldNotBeNull()
            retrievedParent.id shouldBe parentRecipe.id
            
            // Verify the upgraded recipe is treated as independent
            // (modifications to parent should not affect upgraded recipe)
            val modifiedParent = parentRecipe.copy(
                title = "Modified Parent Title",
                servings = parentRecipe.servings + 10
            )
            val updateResult = recipeRepository.updateRecipe(modifiedParent)
            updateResult.isSuccess shouldBe true
            
            // Retrieve the upgraded recipe again
            val upgradedRetrievalResult = recipeRepository.getRecipe(upgradedRecipe.id)
            upgradedRetrievalResult.isSuccess shouldBe true
            
            val retrievedUpgraded = upgradedRetrievalResult.getOrNull()
            retrievedUpgraded.shouldNotBeNull()
            
            // Verify upgraded recipe data is unchanged
            retrievedUpgraded.title shouldBe upgradedRecipe.title
            retrievedUpgraded.servings shouldBe upgradedRecipe.servings
            retrievedUpgraded.title shouldNotBe modifiedParent.title
            retrievedUpgraded.servings shouldNotBe modifiedParent.servings
            
            // Verify version history is maintained
            val historyResult = versionManager.getVersionHistory(upgradedRecipe.id)
            historyResult.isSuccess shouldBe true
            
            val history = historyResult.getOrNull()
            history.shouldNotBeNull()
            history.size shouldBe 1
            history[0].parentRecipeId shouldBe parentRecipe.id
            history[0].version shouldBe upgradedRecipe.version
        }
    }
    
    test("Property 10: Version History Round-Trip - Feature: recipe-manager, Property 10: For any recipe with multiple versions, reverting to a previous version and then viewing it should restore the exact state of that historical version") {
        checkAll(100, recipeArb(), Arb.int(1..5)) { originalRecipe, numVersions ->
            // Create a fresh database for each test iteration
            val databaseDriverFactory = DatabaseDriverFactory()
            val databaseManager = DatabaseManager(databaseDriverFactory)
            val database = databaseManager.initialize()
            
            val recipeRepository = RecipeRepositoryImpl(database)
            val versionRepository = RecipeVersionRepositoryImpl(database)
            val snapshotRepository = RecipeSnapshotRepositoryImpl(database)
            
            val versionManager = RecipeVersionManager(
                recipeRepository,
                versionRepository,
                snapshotRepository
            )
            
            // Ensure unique IDs for ingredients and steps to avoid constraint violations
            val baseRecipe = originalRecipe.copy(
                version = 1,
                parentRecipeId = null,
                ingredients = originalRecipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "${originalRecipe.id}-ingredient-$index")
                },
                steps = originalRecipe.steps.mapIndexed { index, step ->
                    step.copy(id = "${originalRecipe.id}-step-$index")
                }
            )
            
            // Create the base recipe
            val createResult = recipeRepository.createRecipe(baseRecipe)
            createResult.isSuccess shouldBe true
            
            // Create initial version and snapshot
            val initialVersionResult = versionManager.createInitialVersion(baseRecipe)
            initialVersionResult.isSuccess shouldBe true
            
            // Store the original recipe data for comparison
            val originalTitle = baseRecipe.title
            val originalDescription = baseRecipe.description
            val originalPreparationTime = baseRecipe.preparationTime
            val originalCookingTime = baseRecipe.cookingTime
            val originalServings = baseRecipe.servings
            val originalTags = baseRecipe.tags
            val originalIngredientCount = baseRecipe.ingredients.size
            val originalStepCount = baseRecipe.steps.size
            
            // Create multiple upgraded versions
            var currentRecipe = baseRecipe
            repeat(numVersions) { versionIndex ->
                val upgradeResult = versionManager.upgradeRecipe(
                    parentRecipe = currentRecipe,
                    upgradeNotes = "Upgrade $versionIndex"
                ) { recipe ->
                    recipe.copy(
                        title = "Version ${versionIndex + 2} ${recipe.title}",
                        servings = recipe.servings + 1,
                        preparationTime = recipe.preparationTime + 5
                    )
                }
                
                upgradeResult.isSuccess shouldBe true
                currentRecipe = upgradeResult.getOrThrow()
            }
            
            // Verify we have multiple versions
            currentRecipe.version shouldBe baseRecipe.version + numVersions
            
            // Revert to the original version (version 1)
            val revertResult = versionManager.revertToVersion(
                currentRecipeId = currentRecipe.id,
                targetVersion = 1
            )
            
            revertResult.isSuccess shouldBe true
            val revertedRecipe = revertResult.getOrThrow()
            
            // Verify the reverted recipe has a new ID (it's a new recipe in the chain)
            revertedRecipe.id shouldNotBe baseRecipe.id
            revertedRecipe.id shouldNotBe currentRecipe.id
            
            // Verify the reverted recipe restores the exact state of version 1
            revertedRecipe.title shouldBe originalTitle
            revertedRecipe.description shouldBe originalDescription
            revertedRecipe.preparationTime shouldBe originalPreparationTime
            revertedRecipe.cookingTime shouldBe originalCookingTime
            revertedRecipe.servings shouldBe originalServings
            revertedRecipe.tags shouldBe originalTags
            
            // Verify ingredients are restored to original count
            revertedRecipe.ingredients.size shouldBe originalIngredientCount
            
            // Verify steps are restored to original count
            revertedRecipe.steps.size shouldBe originalStepCount
            
            // Verify the reverted recipe maintains the version chain
            revertedRecipe.parentRecipeId shouldBe currentRecipe.id
            revertedRecipe.version shouldBe currentRecipe.version + 1
            
            // Verify we can retrieve the reverted recipe
            val retrievalResult = recipeRepository.getRecipe(revertedRecipe.id)
            retrievalResult.isSuccess shouldBe true
            
            val retrievedRecipe = retrievalResult.getOrNull()
            retrievedRecipe.shouldNotBeNull()
            
            // Verify retrieved recipe matches the reverted recipe
            retrievedRecipe.title shouldBe revertedRecipe.title
            retrievedRecipe.description shouldBe revertedRecipe.description
            retrievedRecipe.preparationTime shouldBe revertedRecipe.preparationTime
            retrievedRecipe.cookingTime shouldBe revertedRecipe.cookingTime
            retrievedRecipe.servings shouldBe revertedRecipe.servings
            retrievedRecipe.tags shouldBe revertedRecipe.tags
            retrievedRecipe.ingredients.size shouldBe revertedRecipe.ingredients.size
            retrievedRecipe.steps.size shouldBe revertedRecipe.steps.size
            
            // Verify version history includes the reversion
            val historyResult = versionManager.getVersionHistory(revertedRecipe.id)
            historyResult.isSuccess shouldBe true
            
            val history = historyResult.getOrNull()
            history.shouldNotBeNull()
            history.size shouldBe 1
            history[0].upgradeNotes shouldBe "Reverted to version 1"
        }
    }
})
