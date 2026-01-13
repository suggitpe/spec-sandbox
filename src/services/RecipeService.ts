import { Database, Q } from '@nozbe/watermelondb';
import { Recipe as RecipeModel, Ingredient as IngredientModel, CookingStep as CookingStepModel } from '../database/models';
import { Recipe, Ingredient, CookingStep } from '../types/Recipe';
import { database } from '../database';

export class RecipeService {
  private db: Database;

  constructor(db: Database = database) {
    this.db = db;
  }

  // Create a new recipe (Requirements 1.1, 1.4)
  async createRecipe(recipeData: Omit<Recipe, 'id' | 'createdAt' | 'updatedAt' | 'version'>): Promise<Recipe> {
    // Validate required fields per Requirements 1.4
    this.validateRecipeData(recipeData);

    const result = await this.db.write(async () => {
      // Create the recipe record
      const recipeRecord = await this.db.get<RecipeModel>('recipes').create((recipe) => {
        recipe.title = recipeData.title;
        recipe.description = recipeData.description;
        recipe.preparationTime = recipeData.preparationTime;
        recipe.cookingTime = recipeData.cookingTime;
        recipe.servings = recipeData.servings;
        recipe.tags = recipeData.tags;
        recipe.version = 1;
        recipe.parentRecipeId = recipeData.parentRecipeId;
      });

      // Create ingredient records
      const ingredientRecords = await Promise.all(
        recipeData.ingredients.map((ingredient) =>
          this.db.get<IngredientModel>('ingredients').create((ingredientRecord) => {
            ingredientRecord.recipeId = recipeRecord.id;
            ingredientRecord.name = ingredient.name;
            ingredientRecord.quantity = ingredient.quantity;
            ingredientRecord.unit = ingredient.unit;
            ingredientRecord.notes = ingredient.notes;
          })
        )
      );

      // Create cooking step records
      const stepRecords = await Promise.all(
        recipeData.steps.map((step) =>
          this.db.get<CookingStepModel>('cooking_steps').create((stepRecord) => {
            stepRecord.recipeId = recipeRecord.id;
            stepRecord.stepNumber = step.stepNumber;
            stepRecord.instruction = step.instruction;
            stepRecord.duration = step.duration;
            stepRecord.temperature = step.temperature;
            stepRecord.timerRequired = step.timerRequired;
          })
        )
      );

      return { recipeRecord, ingredientRecords, stepRecords };
    });

    // Convert to Recipe interface format
    return this.convertRecipeModelToInterface(result.recipeRecord, result.ingredientRecords, result.stepRecords);
  }

  // Get a recipe by ID (Requirements 1.1)
  async getRecipe(id: string): Promise<Recipe | null> {
    try {
      const recipeRecords = await this.db.get<RecipeModel>('recipes')
        .query(Q.where('id', id))
        .fetch();
      
      if (recipeRecords.length === 0) {
        return null;
      }
      
      const recipeRecord = recipeRecords[0];
      const ingredients = await this.db.get<IngredientModel>('ingredients')
        .query(Q.where('recipe_id', id))
        .fetch();
      const steps = await this.db.get<CookingStepModel>('cooking_steps')
        .query(Q.where('recipe_id', id))
        .fetch();
      
      return this.convertRecipeModelToInterface(recipeRecord, ingredients, steps);
    } catch (error) {
      return null;
    }
  }

  // Update an existing recipe (Requirements 1.2)
  async updateRecipe(id: string, updates: Partial<Omit<Recipe, 'id' | 'createdAt' | 'version'>>): Promise<Recipe | null> {
    try {
      const result = await this.db.write(async () => {
        const recipeRecord = await this.db.get<RecipeModel>('recipes').find(id);
        
        // Update recipe fields while preserving creation date
        const updatedRecipe = await recipeRecord.update((recipe) => {
          if (updates.title !== undefined) recipe.title = updates.title;
          if (updates.description !== undefined) recipe.description = updates.description;
          if (updates.preparationTime !== undefined) recipe.preparationTime = updates.preparationTime;
          if (updates.cookingTime !== undefined) recipe.cookingTime = updates.cookingTime;
          if (updates.servings !== undefined) recipe.servings = updates.servings;
          if (updates.tags !== undefined) recipe.tags = updates.tags;
          if (updates.parentRecipeId !== undefined) recipe.parentRecipeId = updates.parentRecipeId;
        });

        // Handle ingredient updates if provided
        let ingredientRecords = await this.db.get<IngredientModel>('ingredients')
          .query(Q.where('recipe_id', id))
          .fetch();
        if (updates.ingredients) {
          // Delete existing ingredients
          await Promise.all(ingredientRecords.map((ingredient: IngredientModel) => ingredient.markAsDeleted()));
          
          // Create new ingredients
          ingredientRecords = await Promise.all(
            updates.ingredients.map((ingredient) =>
              this.db.get<IngredientModel>('ingredients').create((ingredientRecord) => {
                ingredientRecord.recipeId = recipeRecord.id;
                ingredientRecord.name = ingredient.name;
                ingredientRecord.quantity = ingredient.quantity;
                ingredientRecord.unit = ingredient.unit;
                ingredientRecord.notes = ingredient.notes;
              })
            )
          );
        }

        // Handle cooking step updates if provided
        let stepRecords = await this.db.get<CookingStepModel>('cooking_steps')
          .query(Q.where('recipe_id', id))
          .fetch();
        if (updates.steps) {
          // Delete existing steps
          await Promise.all(stepRecords.map((step: CookingStepModel) => step.markAsDeleted()));
          
          // Create new steps
          stepRecords = await Promise.all(
            updates.steps.map((step) =>
              this.db.get<CookingStepModel>('cooking_steps').create((stepRecord) => {
                stepRecord.recipeId = recipeRecord.id;
                stepRecord.stepNumber = step.stepNumber;
                stepRecord.instruction = step.instruction;
                stepRecord.duration = step.duration;
                stepRecord.temperature = step.temperature;
                stepRecord.timerRequired = step.timerRequired;
              })
            )
          );
        }

        return { recipeRecord: updatedRecipe, ingredientRecords, stepRecords };
      });

      return this.convertRecipeModelToInterface(result.recipeRecord, result.ingredientRecords, result.stepRecords);
    } catch (error) {
      return null;
    }
  }

  // Delete a recipe (Requirements 1.3)
  async deleteRecipe(id: string): Promise<boolean> {
    try {
      await this.db.write(async () => {
        const recipeRecord = await this.db.get<RecipeModel>('recipes').find(id);
        
        // Delete related records first
        const ingredients = await this.db.get<IngredientModel>('ingredients')
          .query(Q.where('recipe_id', id))
          .fetch();
        const steps = await this.db.get<CookingStepModel>('cooking_steps')
          .query(Q.where('recipe_id', id))
          .fetch();
        const photos = await this.db.get('photos')
          .query(Q.where('recipe_id', id))
          .fetch();
        
        await Promise.all([
          ...ingredients.map((ingredient: IngredientModel) => ingredient.markAsDeleted()),
          ...steps.map((step: CookingStepModel) => step.markAsDeleted()),
          ...photos.map((photo: any) => photo.markAsDeleted())
        ]);
        
        // Delete the recipe itself
        await recipeRecord.markAsDeleted();
      });
      
      return true;
    } catch (error) {
      return false;
    }
  }

  // Get all recipes
  async getAllRecipes(): Promise<Recipe[]> {
    const recipeRecords = await this.db.get<RecipeModel>('recipes').query().fetch();
    
    const recipes = await Promise.all(
      recipeRecords.map(async (recipeRecord) => {
        const ingredients = await this.db.get<IngredientModel>('ingredients')
          .query(Q.where('recipe_id', recipeRecord.id))
          .fetch();
        const steps = await this.db.get<CookingStepModel>('cooking_steps')
          .query(Q.where('recipe_id', recipeRecord.id))
          .fetch();
        return this.convertRecipeModelToInterface(recipeRecord, ingredients, steps);
      })
    );
    
    return recipes;
  }

  // Search recipes by title, ingredients, or tags (Requirements 1.5)
  async searchRecipes(query: string): Promise<Recipe[]> {
    if (!query || query.trim() === '') {
      return this.getAllRecipes();
    }

    const searchTerm = query.toLowerCase().trim();
    
    // Search in recipe titles and tags
    const recipeRecords = await this.db.get<RecipeModel>('recipes')
      .query(
        Q.or(
          Q.where('title', Q.like(`%${Q.sanitizeLikeString(searchTerm)}%`)),
          Q.where('tags', Q.like(`%${Q.sanitizeLikeString(searchTerm)}%`))
        )
      )
      .fetch();

    // Search in ingredients
    const ingredientRecords = await this.db.get<IngredientModel>('ingredients')
      .query(Q.where('name', Q.like(`%${Q.sanitizeLikeString(searchTerm)}%`)))
      .fetch();

    // Get unique recipe IDs from ingredient matches
    const recipeIdsFromIngredients = [...new Set(ingredientRecords.map(ing => ing.recipeId))];
    
    // Fetch additional recipes that match by ingredients
    const additionalRecipeRecords = await Promise.all(
      recipeIdsFromIngredients
        .filter(id => !recipeRecords.some(recipe => recipe.id === id))
        .map(id => this.db.get<RecipeModel>('recipes').find(id).catch(() => null))
    );

    // Combine all matching recipes
    const allMatchingRecords = [
      ...recipeRecords,
      ...additionalRecipeRecords.filter(Boolean) as RecipeModel[]
    ];

    // Convert to Recipe interface format
    const recipes = await Promise.all(
      allMatchingRecords.map(async (recipeRecord) => {
        const ingredients = await this.db.get<IngredientModel>('ingredients')
          .query(Q.where('recipe_id', recipeRecord.id))
          .fetch();
        const steps = await this.db.get<CookingStepModel>('cooking_steps')
          .query(Q.where('recipe_id', recipeRecord.id))
          .fetch();
        return this.convertRecipeModelToInterface(recipeRecord, ingredients, steps);
      })
    );

    // Filter results to ensure they actually contain the search term
    // This is a safety check to ensure search relevance
    return recipes.filter(recipe => {
      const titleMatch = recipe.title.toLowerCase().includes(searchTerm);
      const tagMatch = recipe.tags.some(tag => tag.toLowerCase().includes(searchTerm));
      const ingredientMatch = recipe.ingredients.some(ing => 
        ing.name.toLowerCase().includes(searchTerm)
      );
      return titleMatch || tagMatch || ingredientMatch;
    });
  }

  // Validation logic (Requirements 1.4)
  private validateRecipeData(recipeData: Omit<Recipe, 'id' | 'createdAt' | 'updatedAt' | 'version'>): void {
    if (!recipeData.title || recipeData.title.trim() === '') {
      throw new Error('Recipe title is required');
    }
    
    if (!recipeData.ingredients || recipeData.ingredients.length === 0) {
      throw new Error('Recipe must contain at least one ingredient');
    }
    
    if (!recipeData.steps || recipeData.steps.length === 0) {
      throw new Error('Recipe must contain at least one cooking step');
    }

    // Validate ingredient data
    recipeData.ingredients.forEach((ingredient, index) => {
      if (!ingredient.name || ingredient.name.trim() === '') {
        throw new Error(`Ingredient ${index + 1} must have a name`);
      }
      if (isNaN(ingredient.quantity) || ingredient.quantity <= 0) {
        throw new Error(`Ingredient ${index + 1} must have a positive quantity`);
      }
      if (!ingredient.unit || ingredient.unit.trim() === '') {
        throw new Error(`Ingredient ${index + 1} must have a unit`);
      }
    });

    // Validate cooking step data
    recipeData.steps.forEach((step, index) => {
      if (!step.instruction || step.instruction.trim() === '') {
        throw new Error(`Cooking step ${index + 1} must have an instruction`);
      }
      if (isNaN(step.stepNumber) || step.stepNumber <= 0) {
        throw new Error(`Cooking step ${index + 1} must have a positive step number`);
      }
      if (step.duration !== undefined && (isNaN(step.duration) || step.duration < 0)) {
        throw new Error(`Cooking step ${index + 1} duration must be non-negative`);
      }
      if (step.temperature !== undefined && (isNaN(step.temperature) || step.temperature < 0)) {
        throw new Error(`Cooking step ${index + 1} temperature must be non-negative`);
      }
    });

    // Validate numeric fields
    if (isNaN(recipeData.preparationTime) || recipeData.preparationTime < 0) {
      throw new Error('Preparation time must be non-negative');
    }
    if (isNaN(recipeData.cookingTime) || recipeData.cookingTime < 0) {
      throw new Error('Cooking time must be non-negative');
    }
    if (isNaN(recipeData.servings) || recipeData.servings <= 0) {
      throw new Error('Servings must be positive');
    }
  }

  // Helper method to convert database models to interface format
  private convertRecipeModelToInterface(
    recipeRecord: RecipeModel,
    ingredients: IngredientModel[],
    steps: CookingStepModel[]
  ): Recipe {
    return {
      id: recipeRecord.id,
      title: recipeRecord.title,
      description: recipeRecord.description === null ? undefined : recipeRecord.description, // Convert null to undefined, preserve empty strings
      ingredients: ingredients.map(ing => ({
        id: ing.id,
        name: ing.name,
        quantity: ing.quantity,
        unit: ing.unit,
        notes: ing.notes === null ? undefined : ing.notes, // Convert null to undefined, preserve empty strings
        photos: [] // Photos will be handled in photo management task
      })),
      steps: steps
        .sort((a, b) => a.stepNumber - b.stepNumber)
        .map(step => ({
          id: step.id,
          stepNumber: step.stepNumber,
          instruction: step.instruction,
          duration: step.duration === null ? undefined : step.duration, // Convert null to undefined
          temperature: step.temperature === null ? undefined : step.temperature, // Convert null to undefined
          timerRequired: step.timerRequired,
          photos: [] // Photos will be handled in photo management task
        })),
      preparationTime: recipeRecord.preparationTime,
      cookingTime: recipeRecord.cookingTime,
      servings: recipeRecord.servings,
      tags: recipeRecord.tags,
      createdAt: recipeRecord.createdAt,
      updatedAt: recipeRecord.updatedAt,
      version: recipeRecord.version,
      parentRecipeId: recipeRecord.parentRecipeId === null ? undefined : recipeRecord.parentRecipeId // Convert null to undefined
    };
  }
}