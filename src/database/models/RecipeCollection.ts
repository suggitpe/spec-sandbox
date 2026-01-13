import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, relation } from '@nozbe/watermelondb/decorators';
import { Recipe } from './Recipe';
import { RecipeCollectionModel } from './Collection';

export class RecipeCollection extends Model {
  static table = 'recipe_collections';
  static associations = {
    recipe: { type: 'belongs_to' as const, key: 'recipe_id' },
    collection: { type: 'belongs_to' as const, key: 'collection_id' },
  };

  @field('recipe_id') recipeId!: string;
  @field('collection_id') collectionId!: string;
  @readonly @date('created_at') createdAt!: Date;

  @relation('recipes', 'recipe_id') recipe!: Recipe;
  @relation('collections', 'collection_id') recipeCollection!: RecipeCollectionModel;
}