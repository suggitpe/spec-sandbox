import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, relation, children } from '@nozbe/watermelondb/decorators';
import { Recipe } from './Recipe';
import { Photo } from './Photo';
import { CookingTimer } from './CookingTimer';

export class CookingStep extends Model {
  static table = 'cooking_steps';
  static associations = {
    recipe: { type: 'belongs_to' as const, key: 'recipe_id' },
    photos: { type: 'has_many' as const, foreignKey: 'cooking_step_id' },
    cooking_timers: { type: 'has_many' as const, foreignKey: 'step_id' },
  };

  @field('recipe_id') recipeId!: string;
  @field('step_number') stepNumber!: number;
  @field('instruction') instruction!: string;
  @field('duration') duration?: number;
  @field('temperature') temperature?: number;
  @field('timer_required') timerRequired!: boolean;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @relation('recipes', 'recipe_id') recipe!: Recipe;
  @children('photos') photos!: Photo[];
  @children('cooking_timers') cookingTimers!: CookingTimer[];
}