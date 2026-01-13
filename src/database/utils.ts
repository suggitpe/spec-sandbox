import { Q } from '@nozbe/watermelondb';
import { database } from './index';
import { Recipe, Ingredient, CookingStep, Photo, RecipeCollectionModel, CookingTimer } from './models';

/**
 * Database utility functions for common operations
 */

// Recipe utilities
export const getRecipeById = async (id: string): Promise<Recipe | null> => {
  try {
    return await database.get<Recipe>('recipes').find(id);
  } catch {
    return null;
  }
};

export const getAllRecipes = async (): Promise<Recipe[]> => {
  return await database.get<Recipe>('recipes').query().fetch();
};

export const searchRecipes = async (searchTerm: string): Promise<Recipe[]> => {
  const recipes = await database.get<Recipe>('recipes').query().fetch();
  
  return recipes.filter(recipe => {
    const titleMatch = recipe.title.toLowerCase().includes(searchTerm.toLowerCase());
    const tagsMatch = recipe.tags.some(tag => 
      tag.toLowerCase().includes(searchTerm.toLowerCase())
    );
    return titleMatch || tagsMatch;
  });
};

// Collection utilities
export const getCollectionById = async (id: string): Promise<RecipeCollectionModel | null> => {
  try {
    return await database.get<RecipeCollectionModel>('collections').find(id);
  } catch {
    return null;
  }
};

export const getAllCollections = async (): Promise<RecipeCollectionModel[]> => {
  return await database.get<RecipeCollectionModel>('collections').query().fetch();
};

// Photo utilities
export const getPhotosByRecipeId = async (recipeId: string): Promise<Photo[]> => {
  return await database.get<Photo>('photos')
    .query(Q.where('recipe_id', recipeId))
    .fetch();
};

export const getPhotosByStage = async (recipeId: string, stage: string): Promise<Photo[]> => {
  return await database.get<Photo>('photos')
    .query(
      Q.where('recipe_id', recipeId),
      Q.where('stage', stage)
    )
    .fetch();
};

// Timer utilities
export const getActiveTimers = async (): Promise<CookingTimer[]> => {
  return await database.get<CookingTimer>('cooking_timers')
    .query(
      Q.where('status', Q.oneOf(['ready', 'running', 'paused']))
    )
    .fetch();
};

export const getTimersByRecipeId = async (recipeId: string): Promise<CookingTimer[]> => {
  return await database.get<CookingTimer>('cooking_timers')
    .query(Q.where('recipe_id', recipeId))
    .fetch();
};

// Database health check
export const checkDatabaseHealth = async (): Promise<boolean> => {
  try {
    // Try to perform a simple query to verify database is working
    await database.get<Recipe>('recipes').query().fetchCount();
    return true;
  } catch (error) {
    console.error('Database health check failed:', error);
    return false;
  }
};