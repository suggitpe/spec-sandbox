import { schema } from '../schema';
import { migrations } from '../migrations';
import { Recipe, PhotoStage, PhotoSyncStatus, TimerStatus } from '../models';

describe('Database Setup', () => {
  test('should have valid schema configuration', () => {
    expect(schema).toBeDefined();
    expect(schema.version).toBe(2);
    expect(schema.tables).toBeDefined();
    
    // Check that schema has the expected structure
    expect(typeof schema.version).toBe('number');
    expect(typeof schema.tables).toBe('object');
  });

  test('should have valid migrations configuration', () => {
    expect(migrations).toBeDefined();
    expect(migrations.sortedMigrations).toBeDefined();
    expect(Array.isArray(migrations.sortedMigrations)).toBe(true);
    expect(migrations.sortedMigrations.length).toBeGreaterThan(0);
  });

  test('should verify enum values work correctly', () => {
    expect(PhotoStage.RAW_INGREDIENTS).toBe('raw_ingredients');
    expect(PhotoStage.PROCESSED_INGREDIENTS).toBe('processed_ingredients');
    expect(PhotoStage.COOKING_STEP).toBe('cooking_step');
    expect(PhotoStage.FINAL_RESULT).toBe('final_result');
    
    expect(PhotoSyncStatus.LOCAL_ONLY).toBe('local_only');
    expect(PhotoSyncStatus.SYNCING).toBe('syncing');
    expect(PhotoSyncStatus.SYNCED).toBe('synced');
    expect(PhotoSyncStatus.SYNC_FAILED).toBe('sync_failed');
    
    expect(TimerStatus.READY).toBe('ready');
    expect(TimerStatus.RUNNING).toBe('running');
    expect(TimerStatus.PAUSED).toBe('paused');
    expect(TimerStatus.COMPLETED).toBe('completed');
    expect(TimerStatus.CANCELLED).toBe('cancelled');
  });

  test('should have correct table associations', () => {
    expect(Recipe.table).toBe('recipes');
    expect(Recipe.associations).toBeDefined();
    expect(Recipe.associations.ingredients.type).toBe('has_many');
    expect(Recipe.associations.cooking_steps.type).toBe('has_many');
    expect(Recipe.associations.photos.type).toBe('has_many');
  });

  test('should verify database structure is complete', () => {
    // Verify all model classes are properly defined
    expect(Recipe).toBeDefined();
    expect(Recipe.table).toBe('recipes');
    
    // Verify all enums are properly exported
    expect(PhotoStage).toBeDefined();
    expect(PhotoSyncStatus).toBeDefined();
    expect(TimerStatus).toBeDefined();
    
    // Verify schema and migrations are properly configured
    expect(schema.version).toBe(2);
    expect(migrations.sortedMigrations.length).toBeGreaterThan(0);
  });
});