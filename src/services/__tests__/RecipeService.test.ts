import * as fc from 'fast-check';
import { RecipeService } from '../RecipeService';
import { Recipe, Ingredient, CookingStep, PhotoStage, SyncStatus } from '../../types/Recipe';
import { createTestDatabase, resetTestDatabase } from '../../database/testDatabase';
import { Database } from '@nozbe/watermelondb';

describe('RecipeService Property Tests', () => {
  let recipeService: RecipeService;
  let testDatabase: Database;

  beforeEach(async () => {
    testDatabase = createTestDatabase();
    await resetTestDatabase(testDatabase);
    recipeService = new RecipeService(testDatabase);
  });

  afterEach(async () => {
    // Clean up database connections
    if (testDatabase) {
      try {
        // WatermelonDB doesn't have a close method, just let it be garbage collected
        testDatabase = null as any;
      } catch (error) {
        // Ignore cleanup errors
      }
    }
  });

  /**
   * Feature: recipe-manager, Property 1: Recipe Creation Completeness
   * For any valid recipe data containing title, ingredients, and cooking steps, 
   * creating and then retrieving the recipe should return all provided data with proper validation
   * Validates: Requirements 1.1, 1.4
   */
  test('Property 1: Recipe Creation Completeness', async () => {
    await fc.assert(
      fc.asyncProperty(
        // Generate valid recipe data
        fc.record({
          title: fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
          description: fc.option(fc.string({ maxLength: 500 }), { nil: undefined }),
          ingredients: fc.array(
            fc.record({
              id: fc.string({ minLength: 1 }),
              name: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
              quantity: fc.float({ min: Math.fround(0.1), max: Math.fround(1000) }).filter(n => !isNaN(n) && isFinite(n)),
              unit: fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
              notes: fc.option(fc.string({ maxLength: 200 }), { nil: undefined }),
              photos: fc.constant([]) // Photos will be handled in photo management task
            }),
            { minLength: 1, maxLength: 20 }
          ),
          steps: fc.array(
            fc.record({
              id: fc.string({ minLength: 1 }),
              stepNumber: fc.integer({ min: 1, max: 50 }),
              instruction: fc.string({ minLength: 1, maxLength: 500 }).filter(s => s.trim().length > 0),
              duration: fc.option(fc.integer({ min: 1, max: 480 }), { nil: undefined }),
              temperature: fc.option(fc.integer({ min: 50, max: 500 }), { nil: undefined }),
              photos: fc.constant([]), // Photos will be handled in photo management task
              timerRequired: fc.boolean()
            }),
            { minLength: 1, maxLength: 30 }
          ),
          preparationTime: fc.integer({ min: 1, max: 480 }),
          cookingTime: fc.integer({ min: 1, max: 480 }),
          servings: fc.integer({ min: 1, max: 20 }),
          tags: fc.array(fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0), { maxLength: 10 }),
          parentRecipeId: fc.option(fc.string({ minLength: 1 }), { nil: undefined })
        }),
        async (recipeData) => {
          // Create recipe
          const createdRecipe = await recipeService.createRecipe(recipeData);
          
          // Retrieve recipe
          const retrievedRecipe = await recipeService.getRecipe(createdRecipe.id);
          
          // Verify recipe was created and can be retrieved
          expect(retrievedRecipe).toBeDefined();
          expect(retrievedRecipe!.id).toBeDefined();
          expect(retrievedRecipe!.createdAt).toBeInstanceOf(Date);
          expect(retrievedRecipe!.updatedAt).toBeInstanceOf(Date);
          expect(retrievedRecipe!.version).toBe(1);
          
          // Verify all provided data is preserved
          expect(retrievedRecipe!.title).toBe(recipeData.title);
          expect(retrievedRecipe!.description).toBe(recipeData.description);
          expect(retrievedRecipe!.preparationTime).toBe(recipeData.preparationTime);
          expect(retrievedRecipe!.cookingTime).toBe(recipeData.cookingTime);
          expect(retrievedRecipe!.servings).toBe(recipeData.servings);
          expect(retrievedRecipe!.tags).toEqual(recipeData.tags);
          expect(retrievedRecipe!.parentRecipeId).toBe(recipeData.parentRecipeId);
          
          // Verify ingredients are preserved
          expect(retrievedRecipe!.ingredients).toHaveLength(recipeData.ingredients.length);
          recipeData.ingredients.forEach((ingredient, index) => {
            const retrievedIngredient = retrievedRecipe!.ingredients[index];
            expect(retrievedIngredient.name).toBe(ingredient.name);
            expect(retrievedIngredient.quantity).toBe(ingredient.quantity);
            expect(retrievedIngredient.unit).toBe(ingredient.unit);
            expect(retrievedIngredient.notes).toBe(ingredient.notes);
          });
          
          // Verify cooking steps are preserved
          expect(retrievedRecipe!.steps).toHaveLength(recipeData.steps.length);
          
          // Sort both arrays by stepNumber for comparison since database may return in different order
          const expectedSteps = [...recipeData.steps].sort((a, b) => a.stepNumber - b.stepNumber);
          const actualSteps = [...retrievedRecipe!.steps].sort((a, b) => a.stepNumber - b.stepNumber);
          
          expectedSteps.forEach((step, index) => {
            const retrievedStep = actualSteps[index];
            expect(retrievedStep.stepNumber).toBe(step.stepNumber);
            expect(retrievedStep.instruction).toBe(step.instruction);
            expect(retrievedStep.duration).toBe(step.duration);
            expect(retrievedStep.temperature).toBe(step.temperature);
            expect(retrievedStep.timerRequired).toBe(step.timerRequired);
          });
        }
      ),
      { numRuns: 100 }
    );
  });

  /**
   * Feature: recipe-manager, Property 2: Recipe Update Preservation
   * For any existing recipe, updating any field should preserve the original creation date 
   * while correctly updating all modified fields
   * Validates: Requirements 1.2
   */
  test('Property 2: Recipe Update Preservation', async () => {
    await fc.assert(
      fc.asyncProperty(
        // Generate initial recipe data
        fc.record({
          title: fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
          description: fc.option(fc.string({ maxLength: 500 }), { nil: undefined }),
          ingredients: fc.array(
            fc.record({
              id: fc.string({ minLength: 1 }),
              name: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
              quantity: fc.float({ min: Math.fround(0.1), max: Math.fround(1000) }).filter(n => !isNaN(n) && isFinite(n)),
              unit: fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
              notes: fc.option(fc.string({ maxLength: 200 }), { nil: undefined }),
              photos: fc.constant([])
            }),
            { minLength: 1, maxLength: 5 }
          ),
          steps: fc.array(
            fc.record({
              id: fc.string({ minLength: 1 }),
              stepNumber: fc.integer({ min: 1, max: 10 }),
              instruction: fc.string({ minLength: 1, maxLength: 500 }).filter(s => s.trim().length > 0),
              duration: fc.option(fc.integer({ min: 1, max: 480 }), { nil: undefined }),
              temperature: fc.option(fc.integer({ min: 50, max: 500 }), { nil: undefined }),
              photos: fc.constant([]),
              timerRequired: fc.boolean()
            }),
            { minLength: 1, maxLength: 5 }
          ),
          preparationTime: fc.integer({ min: 1, max: 480 }),
          cookingTime: fc.integer({ min: 1, max: 480 }),
          servings: fc.integer({ min: 1, max: 20 }),
          tags: fc.array(fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0), { maxLength: 5 }),
          parentRecipeId: fc.option(fc.string({ minLength: 1 }), { nil: undefined })
        }),
        // Generate update data
        fc.record({
          title: fc.option(fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0), { nil: undefined }),
          description: fc.option(fc.string({ maxLength: 500 }), { nil: undefined }),
          preparationTime: fc.option(fc.integer({ min: 1, max: 480 }), { nil: undefined }),
          cookingTime: fc.option(fc.integer({ min: 1, max: 480 }), { nil: undefined }),
          servings: fc.option(fc.integer({ min: 1, max: 20 }), { nil: undefined }),
          tags: fc.option(fc.array(fc.string({ minLength: 1, maxLength: 30 }), { maxLength: 5 }), { nil: undefined })
        }),
        async (initialData, updateData) => {
          // Create initial recipe
          const createdRecipe = await recipeService.createRecipe(initialData);
          const originalCreatedAt = createdRecipe.createdAt;
          
          // Wait a small amount to ensure updatedAt changes
          await new Promise(resolve => setTimeout(resolve, 10));
          
          // Update recipe
          const updatedRecipe = await recipeService.updateRecipe(createdRecipe.id, updateData);
          
          // Verify update was successful
          expect(updatedRecipe).toBeDefined();
          expect(updatedRecipe!.id).toBe(createdRecipe.id);
          
          // Verify creation date is preserved
          expect(updatedRecipe!.createdAt.getTime()).toBe(originalCreatedAt.getTime());
          
          // Verify updated date changed
          expect(updatedRecipe!.updatedAt.getTime()).toBeGreaterThan(originalCreatedAt.getTime());
          
          // Verify updated fields
          if (updateData.title !== undefined) {
            expect(updatedRecipe!.title).toBe(updateData.title);
          } else {
            expect(updatedRecipe!.title).toBe(initialData.title);
          }
          
          if (updateData.description !== undefined) {
            expect(updatedRecipe!.description).toBe(updateData.description);
          } else {
            expect(updatedRecipe!.description).toBe(initialData.description);
          }
          
          if (updateData.preparationTime !== undefined) {
            expect(updatedRecipe!.preparationTime).toBe(updateData.preparationTime);
          } else {
            expect(updatedRecipe!.preparationTime).toBe(initialData.preparationTime);
          }
          
          if (updateData.cookingTime !== undefined) {
            expect(updatedRecipe!.cookingTime).toBe(updateData.cookingTime);
          } else {
            expect(updatedRecipe!.cookingTime).toBe(initialData.cookingTime);
          }
          
          if (updateData.servings !== undefined) {
            expect(updatedRecipe!.servings).toBe(updateData.servings);
          } else {
            expect(updatedRecipe!.servings).toBe(initialData.servings);
          }
          
          if (updateData.tags !== undefined) {
            expect(updatedRecipe!.tags).toEqual(updateData.tags);
          } else {
            expect(updatedRecipe!.tags).toEqual(initialData.tags);
          }
        }
      ),
      { numRuns: 50 }
    );
  });

  /**
   * Feature: recipe-manager, Property 3: Recipe Deletion Consistency
   * For any recipe in the system, deleting it should make it completely inaccessible 
   * through all retrieval methods
   * Validates: Requirements 1.3
   */
  test('Property 3: Recipe Deletion Consistency', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          title: fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
          ingredients: fc.array(
            fc.record({
              id: fc.string({ minLength: 1 }),
              name: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
              quantity: fc.float({ min: Math.fround(0.1), max: Math.fround(1000) }).filter(n => !isNaN(n) && isFinite(n)),
              unit: fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
              photos: fc.constant([])
            }),
            { minLength: 1, maxLength: 5 }
          ),
          steps: fc.array(
            fc.record({
              id: fc.string({ minLength: 1 }),
              stepNumber: fc.integer({ min: 1, max: 10 }),
              instruction: fc.string({ minLength: 1, maxLength: 500 }).filter(s => s.trim().length > 0),
              photos: fc.constant([]),
              timerRequired: fc.boolean()
            }),
            { minLength: 1, maxLength: 5 }
          ),
          preparationTime: fc.integer({ min: 1, max: 480 }),
          cookingTime: fc.integer({ min: 1, max: 480 }),
          servings: fc.integer({ min: 1, max: 20 }),
          tags: fc.array(fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0), { maxLength: 5 })
        }),
        async (recipeData) => {
          // Create recipe
          const createdRecipe = await recipeService.createRecipe(recipeData);
          
          // Verify recipe exists
          const beforeDeletion = await recipeService.getRecipe(createdRecipe.id);
          expect(beforeDeletion).toBeDefined();
          
          // Delete recipe
          const deleteResult = await recipeService.deleteRecipe(createdRecipe.id);
          expect(deleteResult).toBe(true);
          
          // Verify recipe is no longer accessible
          const afterDeletion = await recipeService.getRecipe(createdRecipe.id);
          expect(afterDeletion).toBeNull();
          
          // Verify recipe is not in search results
          const searchResults = await recipeService.searchRecipes(recipeData.title);
          expect(searchResults.find(r => r.id === createdRecipe.id)).toBeUndefined();
          
          // Verify recipe is not in all recipes
          const allRecipes = await recipeService.getAllRecipes();
          expect(allRecipes.find(r => r.id === createdRecipe.id)).toBeUndefined();
        }
      ),
      { numRuns: 50 }
    );
  });

  /**
   * Feature: recipe-manager, Property 4: Search Result Relevance
   * For any search query and recipe collection, all returned results should contain 
   * the search term in title, ingredients, or tags
   * Validates: Requirements 1.5
   */
  test('Property 4: Search Result Relevance', async () => {
    await fc.assert(
      fc.asyncProperty(
        // Generate multiple recipes with known searchable content
        fc.array(
          fc.record({
            title: fc.string({ minLength: 1, maxLength: 100 }).filter(s => s.trim().length > 0),
            ingredients: fc.array(
              fc.record({
                id: fc.string({ minLength: 1 }),
                name: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
                quantity: fc.float({ min: Math.fround(0.1), max: Math.fround(1000) }).filter(n => !isNaN(n) && isFinite(n)),
                unit: fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
                photos: fc.constant([])
              }),
              { minLength: 1, maxLength: 3 }
            ),
            steps: fc.array(
              fc.record({
                id: fc.string({ minLength: 1 }),
                stepNumber: fc.integer({ min: 1, max: 5 }),
                instruction: fc.string({ minLength: 1, maxLength: 200 }).filter(s => s.trim().length > 0),
                photos: fc.constant([]),
                timerRequired: fc.boolean()
              }),
              { minLength: 1, maxLength: 3 }
            ),
            preparationTime: fc.integer({ min: 1, max: 120 }),
            cookingTime: fc.integer({ min: 1, max: 120 }),
            servings: fc.integer({ min: 1, max: 10 }),
            tags: fc.array(fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0), { maxLength: 3 })
          }),
          { minLength: 2, maxLength: 5 }
        ),
        fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
        async (recipesData, searchTerm) => {
          // Create all recipes
          const createdRecipes = await Promise.all(
            recipesData.map(data => recipeService.createRecipe(data))
          );
          
          // Perform search
          const searchResults = await recipeService.searchRecipes(searchTerm);
          
          // Use the same trimming logic as the service for consistency
          const trimmedSearchTerm = searchTerm.toLowerCase().trim();
          
          // Verify all results contain the search term
          searchResults.forEach(recipe => {
            const titleMatch = recipe.title.toLowerCase().includes(trimmedSearchTerm);
            const tagMatch = recipe.tags.some(tag => tag.toLowerCase().includes(trimmedSearchTerm));
            const ingredientMatch = recipe.ingredients.some(ing => 
              ing.name.toLowerCase().includes(trimmedSearchTerm)
            );
            
            expect(titleMatch || tagMatch || ingredientMatch).toBe(true);
          });
        }
      ),
      { numRuns: 30 }
    );
  });

  // Test validation requirements (Requirements 1.4)
  test('Recipe validation - empty title should fail', async () => {
    await expect(recipeService.createRecipe({
      title: '',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Recipe title is required');
  });

  test('Recipe validation - no ingredients should fail', async () => {
    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Recipe must contain at least one ingredient');
  });

  test('Recipe validation - no cooking steps should fail', async () => {
    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Recipe must contain at least one cooking step');
  });

  test('Recipe validation - invalid ingredient data should fail', async () => {
    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: '', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Ingredient 1 must have a name');

    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 0, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Ingredient 1 must have a positive quantity');

    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: '', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Ingredient 1 must have a unit');
  });

  test('Recipe validation - invalid cooking step data should fail', async () => {
    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: '', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Cooking step 1 must have an instruction');

    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 0, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Cooking step 1 must have a positive step number');
  });

  test('Recipe validation - invalid numeric fields should fail', async () => {
    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: -1,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Preparation time must be non-negative');

    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: -1,
      servings: 4,
      tags: []
    })).rejects.toThrow('Cooking time must be non-negative');

    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 0,
      tags: []
    })).rejects.toThrow('Servings must be positive');

    // Test NaN validation
    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: Number.NaN, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: 1, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Ingredient 1 must have a positive quantity');

    await expect(recipeService.createRecipe({
      title: 'Test Recipe',
      ingredients: [{ id: '1', name: 'test', quantity: 1, unit: 'cup', photos: [] }],
      steps: [{ id: '1', stepNumber: Number.NaN, instruction: 'test', photos: [], timerRequired: false }],
      preparationTime: 10,
      cookingTime: 20,
      servings: 4,
      tags: []
    })).rejects.toThrow('Cooking step 1 must have a positive step number');
  });
});