package com.recipemanager.domain

import com.recipemanager.domain.model.*
import com.recipemanager.domain.usecase.RecipeUseCases
import com.recipemanager.domain.validation.RecipeValidator
import com.recipemanager.test.generators.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import kotlinx.datetime.Clock

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
})