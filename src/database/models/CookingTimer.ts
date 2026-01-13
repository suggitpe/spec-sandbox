import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, relation } from '@nozbe/watermelondb/decorators';
import { Recipe } from './Recipe';
import { CookingStep } from './CookingStep';

export enum TimerStatus {
  READY = 'ready',
  RUNNING = 'running',
  PAUSED = 'paused',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled'
}

export class CookingTimer extends Model {
  static table = 'cooking_timers';
  static associations = {
    recipe: { type: 'belongs_to' as const, key: 'recipe_id' },
    cooking_step: { type: 'belongs_to' as const, key: 'step_id' },
  };

  @field('recipe_id') recipeId!: string;
  @field('step_id') stepId!: string;
  @field('duration') duration!: number;
  @field('remaining_time') remainingTime!: number;
  @field('status') statusValue!: string;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @relation('recipes', 'recipe_id') recipe!: Recipe;
  @relation('cooking_steps', 'step_id') cookingStep!: CookingStep;

  // Helper getters and setters for enum
  get status(): TimerStatus {
    return this.statusValue as TimerStatus;
  }

  set status(value: TimerStatus) {
    this.statusValue = value;
  }
}