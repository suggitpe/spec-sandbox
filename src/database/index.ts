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

// Database adapter configuration
const adapter = new SQLiteAdapter({
  schema,
  migrations,
  dbName: process.env.NODE_ENV === 'test' ? ':memory:' : 'RecipeManager',
  jsi: process.env.NODE_ENV !== 'test', // Disable JSI in test environment
  onSetUpError: (error) => {
    console.error('Database setup error:', error);
    // In production, you might want to handle this more gracefully
    // For example, by clearing the database and starting fresh
  },
});

// Database instance
export const database = new Database({
  adapter,
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

// Database initialization function
export const initializeDatabase = async (): Promise<Database> => {
  try {
    // The database will automatically run migrations on first access
    // This is handled by WatermelonDB internally
    console.log('Database initialized successfully');
    return database;
  } catch (error) {
    console.error('Failed to initialize database:', error);
    throw error;
  }
};

// Helper function to reset database (useful for development/testing)
export const resetDatabase = async (): Promise<void> => {
  try {
    await database.write(async () => {
      await database.unsafeResetDatabase();
    });
    console.log('Database reset successfully');
  } catch (error) {
    console.error('Failed to reset database:', error);
    throw error;
  }
};

// Export database instance and models for use throughout the app
export { database as default };
export * from './models';
export { schema } from './schema';
export { migrations } from './migrations';