import { schemaMigrations, createTable, addColumns } from '@nozbe/watermelondb/Schema/migrations';

export const migrations = schemaMigrations({
  migrations: [
    // Migration 2: Initial schema (WatermelonDB requires migrations to start from version 2)
    {
      toVersion: 2,
      steps: [
        createTable({
          name: 'recipes',
          columns: [
            { name: 'title', type: 'string' },
            { name: 'description', type: 'string', isOptional: true },
            { name: 'preparation_time', type: 'number' },
            { name: 'cooking_time', type: 'number' },
            { name: 'servings', type: 'number' },
            { name: 'tags', type: 'string' },
            { name: 'version', type: 'number' },
            { name: 'parent_recipe_id', type: 'string', isOptional: true },
            { name: 'created_at', type: 'number' },
            { name: 'updated_at', type: 'number' },
          ],
        }),
        createTable({
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
        createTable({
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
        createTable({
          name: 'photos',
          columns: [
            { name: 'recipe_id', type: 'string', isIndexed: true },
            { name: 'ingredient_id', type: 'string', isOptional: true, isIndexed: true },
            { name: 'cooking_step_id', type: 'string', isOptional: true, isIndexed: true },
            { name: 'local_path', type: 'string' },
            { name: 'cloud_url', type: 'string', isOptional: true },
            { name: 'caption', type: 'string', isOptional: true },
            { name: 'stage', type: 'string' },
            { name: 'sync_status', type: 'string' },
            { name: 'timestamp', type: 'number' },
            { name: 'created_at', type: 'number' },
            { name: 'updated_at', type: 'number' },
          ],
        }),
        createTable({
          name: 'collections',
          columns: [
            { name: 'name', type: 'string' },
            { name: 'description', type: 'string', isOptional: true },
            { name: 'created_at', type: 'number' },
            { name: 'updated_at', type: 'number' },
          ],
        }),
        createTable({
          name: 'recipe_collections',
          columns: [
            { name: 'recipe_id', type: 'string', isIndexed: true },
            { name: 'collection_id', type: 'string', isIndexed: true },
            { name: 'created_at', type: 'number' },
          ],
        }),
        createTable({
          name: 'cooking_timers',
          columns: [
            { name: 'recipe_id', type: 'string', isIndexed: true },
            { name: 'step_id', type: 'string', isIndexed: true },
            { name: 'duration', type: 'number' },
            { name: 'remaining_time', type: 'number' },
            { name: 'status', type: 'string' },
            { name: 'created_at', type: 'number' },
            { name: 'updated_at', type: 'number' },
          ],
        }),
        createTable({
          name: 'sync_queue',
          columns: [
            { name: 'operation_type', type: 'string' },
            { name: 'table_name', type: 'string' },
            { name: 'record_id', type: 'string' },
            { name: 'data', type: 'string' },
            { name: 'retry_count', type: 'number' },
            { name: 'created_at', type: 'number' },
            { name: 'updated_at', type: 'number' },
          ],
        }),
      ],
    },
    // Future migrations can be added here
    // Example:
    // {
    //   toVersion: 2,
    //   steps: [
    //     addColumns({
    //       table: 'recipes',
    //       columns: [
    //         { name: 'difficulty_level', type: 'string', isOptional: true },
    //       ],
    //     }),
    //   ],
    // },
  ],
});