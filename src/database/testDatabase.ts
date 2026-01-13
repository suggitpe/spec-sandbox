import { Database } from '@nozbe/watermelondb';
import SQLiteAdapter from '@nozbe/watermelondb/adapters/sqlite';
import { schema } from './schema';
import { migrations } from './migrations';
import {
  Recipe,
  Ingredient,
  CookingStep,
  Photo,
  RecipeCollectionModel,
  RecipeCollection,
  CookingTimer,
  SyncQueue,
} from './models';

// Test database adapter configuration
const createTestAdapter = () => new SQLiteAdapter({
  schema,
  migrations,
  dbName: ':memory:', // Use in-memory database for tests
  jsi: false, // Disable JSI for Node.js environment
  onSetUpError: (error) => {
    console.error('Test database setup error:', error);
    throw error;
  },
});

// Create test database instance
export const createTestDatabase = (): Database => {
  return new Database({
    adapter: createTestAdapter(),
    modelClasses: [
      Recipe,
      Ingredient,
      CookingStep,
      Photo,
      RecipeCollectionModel,
      RecipeCollection,
      CookingTimer,
      SyncQueue,
    ],
  });
};

// Helper function to reset test database
export const resetTestDatabase = async (database: Database): Promise<void> => {
  try {
    await database.write(async () => {
      await database.unsafeResetDatabase();
    });
  } catch (error) {
    console.error('Failed to reset test database:', error);
    throw error;
  }
};