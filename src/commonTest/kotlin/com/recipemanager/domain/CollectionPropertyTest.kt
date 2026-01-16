package com.recipemanager.domain

import com.recipemanager.data.database.DatabaseDriverFactory
import com.recipemanager.data.database.DatabaseManager
import com.recipemanager.data.repository.CollectionRepositoryImpl
import com.recipemanager.data.repository.RecipeRepositoryImpl
import com.recipemanager.test.generators.recipeArb
import com.recipemanager.test.generators.recipeCollectionArb
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class CollectionPropertyTest : FunSpec({
    
    lateinit var collectionRepository: CollectionRepositoryImpl
    lateinit var recipeRepository: RecipeRepositoryImpl
    
    beforeEach {
        val databaseManager = DatabaseManager(DatabaseDriverFactory())
        val database = databaseManager.initialize()
        collectionRepository = CollectionRepositoryImpl(database)
        recipeRepository = RecipeRepositoryImpl(database)
    }
    
    test("Property 14: Collection-Recipe Relationship Integrity - Feature: recipe-manager, Property 14: For any recipe added to a collection, it should remain accessible through both the main library and the collection, and removing it from the collection should not affect main library access") {
        checkAll(100, recipeArb(), recipeCollectionArb()) { recipe, collection ->
            // Ensure unique IDs to avoid database conflicts
            val uniqueRecipe = recipe.copy(
                id = java.util.UUID.randomUUID().toString(),
                ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "${java.util.UUID.randomUUID()}_ingredient_$index")
                },
                steps = recipe.steps.mapIndexed { index, step ->
                    step.copy(id = "${java.util.UUID.randomUUID()}_step_$index")
                }
            )
            
            // Create the recipe in the main library
            val createRecipeResult = recipeRepository.createRecipe(uniqueRecipe)
            createRecipeResult.isSuccess shouldBe true
            
            // Create the collection
            val uniqueCollection = collection.copy(
                id = java.util.UUID.randomUUID().toString(),
                recipeIds = emptyList()
            )
            val createCollectionResult = collectionRepository.createCollection(uniqueCollection)
            createCollectionResult.isSuccess shouldBe true
            
            // Add recipe to collection
            val addResult = collectionRepository.addRecipeToCollection(uniqueRecipe.id, uniqueCollection.id)
            addResult.isSuccess shouldBe true
            
            // Verify recipe is accessible through main library
            val recipeFromLibrary = recipeRepository.getRecipe(uniqueRecipe.id).getOrThrow()
            recipeFromLibrary shouldNotBe null
            recipeFromLibrary?.id shouldBe uniqueRecipe.id
            recipeFromLibrary?.title shouldBe uniqueRecipe.title
            
            // Verify recipe is accessible through collection
            val recipesInCollection = collectionRepository.getRecipesInCollection(uniqueCollection.id).getOrThrow()
            recipesInCollection shouldHaveSize 1
            recipesInCollection[0].id shouldBe uniqueRecipe.id
            recipesInCollection[0].title shouldBe uniqueRecipe.title
            
            // Remove recipe from collection
            val removeResult = collectionRepository.removeRecipeFromCollection(uniqueRecipe.id, uniqueCollection.id)
            removeResult.isSuccess shouldBe true
            
            // Verify recipe is still accessible through main library
            val recipeStillInLibrary = recipeRepository.getRecipe(uniqueRecipe.id).getOrThrow()
            recipeStillInLibrary shouldNotBe null
            recipeStillInLibrary?.id shouldBe uniqueRecipe.id
            recipeStillInLibrary?.title shouldBe uniqueRecipe.title
            
            // Verify recipe is no longer in collection
            val recipesAfterRemoval = collectionRepository.getRecipesInCollection(uniqueCollection.id).getOrThrow()
            recipesAfterRemoval shouldHaveSize 0
        }
    }
    
    test("Property 15: Multi-Collection Membership - Feature: recipe-manager, Property 15: For any recipe, it should be possible to add it to multiple collections simultaneously without data duplication or conflicts") {
        checkAll(100, recipeArb(), Arb.list(recipeCollectionArb(), 2..5)) { recipe, collections ->
            // Ensure unique IDs to avoid database conflicts
            val uniqueRecipe = recipe.copy(
                id = java.util.UUID.randomUUID().toString(),
                ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(id = "${java.util.UUID.randomUUID()}_ingredient_$index")
                },
                steps = recipe.steps.mapIndexed { index, step ->
                    step.copy(id = "${java.util.UUID.randomUUID()}_step_$index")
                }
            )
            
            // Create the recipe in the main library
            val createRecipeResult = recipeRepository.createRecipe(uniqueRecipe)
            createRecipeResult.isSuccess shouldBe true
            
            // Create all collections (empty initially)
            val createdCollections = collections.mapIndexed { index, collection ->
                val uniqueCollection = collection.copy(
                    id = "${java.util.UUID.randomUUID()}_$index", // Ensure unique IDs
                    recipeIds = emptyList()
                )
                val createResult = collectionRepository.createCollection(uniqueCollection)
                createResult.isSuccess shouldBe true
                uniqueCollection
            }
            
            // Add recipe to all collections
            createdCollections.forEach { collection ->
                val addResult = collectionRepository.addRecipeToCollection(uniqueRecipe.id, collection.id)
                addResult.isSuccess shouldBe true
            }
            
            // Verify recipe is in all collections
            createdCollections.forEach { collection ->
                val recipesInCollection = collectionRepository.getRecipesInCollection(collection.id).getOrThrow()
                recipesInCollection shouldHaveSize 1
                recipesInCollection[0].id shouldBe uniqueRecipe.id
                recipesInCollection[0].title shouldBe uniqueRecipe.title
            }
            
            // Verify recipe appears in getCollectionsForRecipe
            val collectionsForRecipe = collectionRepository.getCollectionsForRecipe(uniqueRecipe.id).getOrThrow()
            collectionsForRecipe shouldHaveSize createdCollections.size
            
            // Verify all collection IDs are present
            val collectionIds = collectionsForRecipe.map { it.id }.toSet()
            createdCollections.forEach { collection ->
                collectionIds shouldContain collection.id
            }
            
            // Verify no data duplication - recipe should still be single instance in main library
            val recipeFromLibrary = recipeRepository.getRecipe(uniqueRecipe.id).getOrThrow()
            recipeFromLibrary shouldNotBe null
            recipeFromLibrary?.id shouldBe uniqueRecipe.id
            
            // Verify removing from one collection doesn't affect others
            val firstCollection = createdCollections.first()
            val removeResult = collectionRepository.removeRecipeFromCollection(uniqueRecipe.id, firstCollection.id)
            removeResult.isSuccess shouldBe true
            
            // Verify recipe is still in remaining collections
            val remainingCollections = createdCollections.drop(1)
            remainingCollections.forEach { collection ->
                val recipesInCollection = collectionRepository.getRecipesInCollection(collection.id).getOrThrow()
                recipesInCollection shouldHaveSize 1
                recipesInCollection[0].id shouldBe uniqueRecipe.id
            }
            
            // Verify recipe is still in main library
            val recipeStillInLibrary = recipeRepository.getRecipe(uniqueRecipe.id).getOrThrow()
            recipeStillInLibrary shouldNotBe null
            recipeStillInLibrary?.id shouldBe uniqueRecipe.id
        }
    }
})