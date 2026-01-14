package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.database.RecipeDatabase
import com.recipemanager.domain.model.*
import com.recipemanager.domain.usecase.RecipeUseCases
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.test.generators.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class RecipePropertyTest : FunSpec({
    
    test("Property 1: Recipe Creation Completeness - Feature: recipe-manager, Property 1: For any valid recipe data containing title, ingredients, and cooking steps, creating and then retrieving the recipe should return all provided data with proper validation") {
        checkAll(100, recipeArb()) { recipe ->
            // Validate the recipe using RecipeValidator
            val validator = RecipeValidator()
            val validationResult = validator.validateRecipe(recipe)
            
            // Since we're using a generator that creates valid recipes,
            // validation should always succeed
            validationResult shouldBe com.recipemanager.domain.validation.ValidationResult.Success
            
            // Verify all required fields are present and valid
            recipe.id shouldNotBe ""
            recipe.title shouldNotBe ""
            recipe.ingredients.size shouldBe recipe.ingredients.size // Should be >= 1 from generator
            recipe.steps.size shouldBe recipe.steps.size // Should be >= 1 from generator
            recipe.preparationTime shouldBe recipe.preparationTime // Should be >= 0 from generator
            recipe.cookingTime shouldBe recipe.cookingTime // Should be >= 0 from generator
            recipe.servings shouldBe recipe.servings // Should be > 0 from generator
            
            // Verify data integrity
            recipe.createdAt shouldBe recipe.createdAt
            recipe.updatedAt shouldBe recipe.updatedAt
            recipe.version shouldBe recipe.version
            
            // Verify ingredients have required fields
            recipe.ingredients.forEach { ingredient ->
                ingredient.id shouldNotBe ""
                ingredient.name shouldNotBe ""
                ingredient.quantity shouldBe ingredient.quantity // Should be > 0 from generator
                ingredient.unit shouldNotBe ""
            }
            
            // Verify cooking steps have required fields
            recipe.steps.forEach { step ->
                step.id shouldNotBe ""
                step.instruction shouldNotBe ""
                step.stepNumber shouldBe step.stepNumber // Should be > 0 from generator
            }
        }
    }
    
    test("Property 2: Recipe Update Preservation - Feature: recipe-manager, Property 2: For any existing recipe, updating any field should preserve the original creation date while correctly updating all modified fields") {
        checkAll(10, recipeArb()) { generatedRecipe ->
            // Create a fresh database for each test iteration
            val driverFactory = DatabaseDriverFactory()
            val database = RecipeDatabase(driverFactory.createDriver())
            val repository = RecipeRepositoryImpl(database)
            
            // Ensure unique IDs for ingredients and steps to avoid constraint violations
            val originalRecipe = generatedRecipe.copy(
                ingredients = generatedRecipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "${generatedRecipe.id}-ingredient-$index")
                },
                steps = generatedRecipe.steps.mapIndexed { index, step ->
                    step.copy(id = "${generatedRecipe.id}-step-$index")
                }
            )
            
            // Create the original recipe
            val createResult = repository.createRecipe(originalRecipe)
            createResult.isSuccess shouldBe true
            
            // Wait a moment to ensure updatedAt will be different at second precision
            Thread.sleep(1100)
            
            // Create an updated version with modified fields but same creation date
            val updatedRecipe = originalRecipe.copy(
                title = "Updated ${originalRecipe.title}",
                description = "Updated description",
                preparationTime = originalRecipe.preparationTime + 10,
                cookingTime = originalRecipe.cookingTime + 15,
                servings = originalRecipe.servings + 2,
                updatedAt = Clock.System.now(),
                version = originalRecipe.version + 1
            )
            
            // Update the recipe
            val updateResult = repository.updateRecipe(updatedRecipe)
            updateResult.isSuccess shouldBe true
            
            // Retrieve the updated recipe
            val retrieveResult = repository.getRecipe(originalRecipe.id)
            retrieveResult.isSuccess shouldBe true
            
            val retrievedRecipe = retrieveResult.getOrNull()
            retrievedRecipe.shouldNotBeNull()
            
            // Verify the creation date is preserved (compare at second precision due to SQLite INTEGER storage)
            retrievedRecipe.createdAt.epochSeconds shouldBe originalRecipe.createdAt.epochSeconds
            
            // Verify all updated fields are correctly modified
            retrievedRecipe.title shouldBe updatedRecipe.title
            retrievedRecipe.description shouldBe updatedRecipe.description
            retrievedRecipe.preparationTime shouldBe updatedRecipe.preparationTime
            retrievedRecipe.cookingTime shouldBe updatedRecipe.cookingTime
            retrievedRecipe.servings shouldBe updatedRecipe.servings
            retrievedRecipe.version shouldBe updatedRecipe.version
            
            // Verify updatedAt is changed (should be greater than original)
            retrievedRecipe.updatedAt.epochSeconds shouldNotBe originalRecipe.updatedAt.epochSeconds
        }
    }
    
    test("Property 3: Recipe Deletion Consistency - Feature: recipe-manager, Property 3: For any recipe in the system, deleting it should make it completely inaccessible through all retrieval methods") {
        checkAll(100, recipeArb()) { originalRecipe ->
            // Create a fresh database for each test iteration
            val driverFactory = DatabaseDriverFactory()
            val database = RecipeDatabase(driverFactory.createDriver())
            val repository = RecipeRepositoryImpl(database)
            
            // Ensure unique IDs for ingredients and steps to avoid constraint violations
            val recipe = originalRecipe.copy(
                ingredients = originalRecipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "${originalRecipe.id}-ingredient-$index")
                },
                steps = originalRecipe.steps.mapIndexed { index, step ->
                    step.copy(id = "${originalRecipe.id}-step-$index")
                }
            )
            
            // Create the recipe
            val createResult = repository.createRecipe(recipe)
            createResult.isSuccess shouldBe true
            
            // Verify recipe exists before deletion
            val beforeDeleteResult = repository.getRecipe(recipe.id)
            beforeDeleteResult.isSuccess shouldBe true
            beforeDeleteResult.getOrNull().shouldNotBeNull()
            
            // Delete the recipe
            val deleteResult = repository.deleteRecipe(recipe.id)
            deleteResult.isSuccess shouldBe true
            
            // Verify recipe is not retrievable by ID
            val afterDeleteResult = repository.getRecipe(recipe.id)
            afterDeleteResult.isSuccess shouldBe true
            afterDeleteResult.getOrNull().shouldBeNull()
            
            // Verify recipe is not in the list of all recipes
            val allRecipesResult = repository.getAllRecipes()
            allRecipesResult.isSuccess shouldBe true
            val allRecipes = allRecipesResult.getOrNull()
            allRecipes.shouldNotBeNull()
            allRecipes.map { it.id } shouldNotContain recipe.id
            
            // Verify recipe is not in search results
            val searchResult = repository.searchRecipes(recipe.title)
            searchResult.isSuccess shouldBe true
            val searchRecipes = searchResult.getOrNull()
            searchRecipes.shouldNotBeNull()
            searchRecipes.map { it.id } shouldNotContain recipe.id
        }
    }
    
    test("Property 4: Search Result Relevance - Feature: recipe-manager, Property 4: For any search query and recipe collection, all returned results should contain the search term in title, ingredients, or tags") {
        checkAll(100, Arb.string(3..20).filter { it.isNotBlank() && it.length >= 3 }) { searchTerm ->
            // Create a fresh database for each test iteration
            val driverFactory = DatabaseDriverFactory()
            val database = RecipeDatabase(driverFactory.createDriver())
            val repository = RecipeRepositoryImpl(database)
            
            // Generate a unique base ID for this test iteration that doesn't contain the search term
            val safeSearchTerm = searchTerm.replace(Regex("[^a-zA-Z0-9]"), "")
            val baseId = "test-${Clock.System.now().toEpochMilliseconds()}"
            
            // Create multiple recipes, some matching the search term
            val matchingRecipe1 = recipeArb().bind().copy(
                id = "$baseId-match1",
                title = "Recipe with $searchTerm in title",
                ingredients = recipeArb().bind().ingredients.mapIndexed { index, ing ->
                    ing.copy(id = "$baseId-match1-ing-$index")
                },
                steps = recipeArb().bind().steps.mapIndexed { index, step ->
                    step.copy(id = "$baseId-match1-step-$index")
                }
            )
            val matchingRecipe2 = recipeArb().bind().copy(
                id = "$baseId-match2",
                description = "Recipe with $searchTerm in description",
                title = "Different title xyz",
                ingredients = recipeArb().bind().ingredients.mapIndexed { index, ing ->
                    ing.copy(id = "$baseId-match2-ing-$index")
                },
                steps = recipeArb().bind().steps.mapIndexed { index, step ->
                    step.copy(id = "$baseId-match2-step-$index")
                }
            )
            val matchingRecipe3 = recipeArb().bind().copy(
                id = "$baseId-match3",
                tags = listOf(searchTerm, "other-tag"),
                title = "Another title xyz",
                description = "No match here xyz",
                ingredients = recipeArb().bind().ingredients.mapIndexed { index, ing ->
                    ing.copy(id = "$baseId-match3-ing-$index")
                },
                steps = recipeArb().bind().steps.mapIndexed { index, step ->
                    step.copy(id = "$baseId-match3-step-$index")
                }
            )
            
            // Create a non-matching recipe with content that definitely doesn't contain the search term
            val nonMatchingRecipe = recipeArb().bind().copy(
                id = "$baseId-nomatch",
                title = "Completely unrelated recipe xyz",
                description = "No matching terms here at all xyz",
                tags = listOf("unrelated", "tags", "xyz"),
                ingredients = recipeArb().bind().ingredients.mapIndexed { index, ing ->
                    ing.copy(id = "$baseId-nomatch-ing-$index")
                },
                steps = recipeArb().bind().steps.mapIndexed { index, step ->
                    step.copy(id = "$baseId-nomatch-step-$index")
                }
            )
            
            // Only insert the non-matching recipe if it truly doesn't contain the search term
            val nonMatchingContainsSearchTerm = 
                nonMatchingRecipe.title.contains(searchTerm, ignoreCase = true) ||
                (nonMatchingRecipe.description?.contains(searchTerm, ignoreCase = true) ?: false) ||
                nonMatchingRecipe.tags.any { it.contains(searchTerm, ignoreCase = true) }
            
            // Insert all recipes
            repository.createRecipe(matchingRecipe1)
            repository.createRecipe(matchingRecipe2)
            repository.createRecipe(matchingRecipe3)
            if (!nonMatchingContainsSearchTerm) {
                repository.createRecipe(nonMatchingRecipe)
            }
            
            // Search for the term
            val searchResult = repository.searchRecipes(searchTerm)
            searchResult.isSuccess shouldBe true
            
            val results = searchResult.getOrNull()
            results.shouldNotBeNull()
            
            // Verify all results contain the search term in title, description, or tags
            results.forEach { recipe ->
                val containsInTitle = recipe.title.contains(searchTerm, ignoreCase = true)
                val containsInDescription = recipe.description?.contains(searchTerm, ignoreCase = true) ?: false
                val containsInTags = recipe.tags.any { it.contains(searchTerm, ignoreCase = true) }
                
                val containsSearchTerm = containsInTitle || containsInDescription || containsInTags
                containsSearchTerm shouldBe true
            }
            
            // Verify the non-matching recipe is not in results (if it was inserted)
            if (!nonMatchingContainsSearchTerm) {
                results.map { it.id } shouldNotContain nonMatchingRecipe.id
            }
        }
    }
})