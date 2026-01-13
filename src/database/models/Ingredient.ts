import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, relation, children } from '@nozbe/watermelondb/decorators';
import { Recipe } from './Recipe';
import { Photo } from './Photo';

export class Ingredient extends Model {
  static table = 'ingredients';
  static associations = {
    recipe: { type: 'belongs_to' as const, key: 'recipe_id' },
    photos: { type: 'has_many' as const, foreignKey: 'ingredient_id' },
  };

  @field('recipe_id') recipeId!: string;
  @field('name') name!: string;
  @field('quantity') quantity!: number;
  @field('unit') unit!: string;
  @field('notes') notes?: string;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @relation('recipes', 'recipe_id') recipe!: Recipe;
  @children('photos') photos!: Photo[];
}