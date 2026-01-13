import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, relation } from '@nozbe/watermelondb/decorators';
import { Recipe } from './Recipe';
import { Ingredient } from './Ingredient';
import { CookingStep } from './CookingStep';

export enum PhotoStage {
  RAW_INGREDIENTS = 'raw_ingredients',
  PROCESSED_INGREDIENTS = 'processed_ingredients',
  COOKING_STEP = 'cooking_step',
  FINAL_RESULT = 'final_result'
}

export enum PhotoSyncStatus {
  LOCAL_ONLY = 'local_only',
  SYNCING = 'syncing',
  SYNCED = 'synced',
  SYNC_FAILED = 'sync_failed'
}

export class Photo extends Model {
  static table = 'photos';
  static associations = {
    recipe: { type: 'belongs_to' as const, key: 'recipe_id' },
    ingredient: { type: 'belongs_to' as const, key: 'ingredient_id' },
    cooking_step: { type: 'belongs_to' as const, key: 'cooking_step_id' },
  };

  @field('recipe_id') recipeId!: string;
  @field('ingredient_id') ingredientId?: string;
  @field('cooking_step_id') cookingStepId?: string;
  @field('local_path') localPath!: string;
  @field('cloud_url') cloudUrl?: string;
  @field('caption') caption?: string;
  @field('stage') stageValue!: string;
  @field('sync_status') syncStatusValue!: string;
  @field('timestamp') timestamp!: number;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @relation('recipes', 'recipe_id') recipe!: Recipe;
  @relation('ingredients', 'ingredient_id') ingredient?: Ingredient;
  @relation('cooking_steps', 'cooking_step_id') cookingStep?: CookingStep;

  // Helper getters and setters for enums
  get stage(): PhotoStage {
    return this.stageValue as PhotoStage;
  }

  set stage(value: PhotoStage) {
    this.stageValue = value;
  }

  get photoSyncStatus(): PhotoSyncStatus {
    return this.syncStatusValue as PhotoSyncStatus;
  }

  set photoSyncStatus(value: PhotoSyncStatus) {
    this.syncStatusValue = value;
  }
}