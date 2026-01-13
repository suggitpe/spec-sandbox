import { appSchema, tableSchema } from '@nozbe/watermelondb';

export const schema = appSchema({
  version: 2,
  tables: [
    // Recipes table
    tableSchema({
      name: 'recipes',
      columns: [
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'preparation_time', type: 'number' },
        { name: 'cooking_time', type: 'number' },
        { name: 'servings', type: 'number' },
        { name: 'tags', type: 'string' }, // JSON string array
        { name: 'version', type: 'number' },
        { name: 'parent_recipe_id', type: 'string', isOptional: true },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),

    // Ingredients table
    tableSchema({
      name: 'ingredients',
      columns: [
        { name: 'recipe_id', type: 'string', isIndexed: true },
        { name: 'name', type: 'string' },
        { name: 'quantity', type: 'number' },
        { name: 'unit', type: 'string' },
        { name: 'notes', type: 'string', isOptional: true },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),

    // Cooking steps table
    tableSchema({
      name: 'cooking_steps',
      columns: [
        { name: 'recipe_id', type: 'string', isIndexed: true },
        { name: 'step_number', type: 'number' },
        { name: 'instruction', type: 'string' },
        { name: 'duration', type: 'number', isOptional: true },
        { name: 'temperature', type: 'number', isOptional: true },
        { name: 'timer_required', type: 'boolean' },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),

    // Photos table
    tableSchema({
      name: 'photos',
      columns: [
        { name: 'recipe_id', type: 'string', isIndexed: true },
        { name: 'ingredient_id', type: 'string', isOptional: true, isIndexed: true },
        { name: 'cooking_step_id', type: 'string', isOptional: true, isIndexed: true },
        { name: 'local_path', type: 'string' },
        { name: 'cloud_url', type: 'string', isOptional: true },
        { name: 'caption', type: 'string', isOptional: true },
        { name: 'stage', type: 'string' }, // PhotoStage enum
        { name: 'sync_status', type: 'string' }, // SyncStatus enum
        { name: 'timestamp', type: 'number' },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),

    // Collections table
    tableSchema({
      name: 'collections',
      columns: [
        { name: 'name', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),

    // Recipe-Collection junction table (many-to-many)
    tableSchema({
      name: 'recipe_collections',
      columns: [
        { name: 'recipe_id', type: 'string', isIndexed: true },
        { name: 'collection_id', type: 'string', isIndexed: true },
        { name: 'created_at', type: 'number' },
      ],
    }),

    // Cooking timers table
    tableSchema({
      name: 'cooking_timers',
      columns: [
        { name: 'recipe_id', type: 'string', isIndexed: true },
        { name: 'step_id', type: 'string', isIndexed: true },
        { name: 'duration', type: 'number' },
        { name: 'remaining_time', type: 'number' },
        { name: 'status', type: 'string' }, // TimerStatus enum
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),

    // Sync queue table for offline operations
    tableSchema({
      name: 'sync_queue',
      columns: [
        { name: 'operation_type', type: 'string' },
        { name: 'table_name', type: 'string' },
        { name: 'record_id', type: 'string' },
        { name: 'data', type: 'string' }, // JSON string
        { name: 'retry_count', type: 'number' },
        { name: 'created_at', type: 'number' },
        { name: 'updated_at', type: 'number' },
      ],
    }),
  ],
});