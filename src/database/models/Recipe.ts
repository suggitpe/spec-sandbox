import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, children, relation } from '@nozbe/watermelondb/decorators';
import { Ingredient } from './Ingredient';
import { CookingStep } from './CookingStep';
import { Photo } from './Photo';

export class Recipe extends Model {
  static table = 'recipes';
  static associations = {
    ingredients: { type: 'has_many' as const, foreignKey: 'recipe_id' },
    cooking_steps: { type: 'has_many' as const, foreignKey: 'recipe_id' },
    photos: { type: 'has_many' as const, foreignKey: 'recipe_id' },
    recipe_collections: { type: 'has_many' as const, foreignKey: 'recipe_id' },
    cooking_timers: { type: 'has_many' as const, foreignKey: 'recipe_id' },
  };

  @field('title') title!: string;
  @field('description') description?: string;
  @field('preparation_time') preparationTime!: number;
  @field('cooking_time') cookingTime!: number;
  @field('servings') servings!: number;
  @field('tags') tagsJson!: string;
  @field('version') version!: number;
  @field('parent_recipe_id') parentRecipeId?: string;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @children('ingredients') ingredients!: Ingredient[];
  @children('cooking_steps') cookingSteps!: CookingStep[];
  @children('photos') photos!: Photo[];

  // Helper methods for tags
  get tags(): string[] {
    try {
      return JSON.parse(this.tagsJson || '[]');
    } catch {
      return [];
    }
  }

  set tags(value: string[]) {
    this.tagsJson = JSON.stringify(value);
  }
}