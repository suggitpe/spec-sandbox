import { Model } from '@nozbe/watermelondb';
import { field, date, readonly, children } from '@nozbe/watermelondb/decorators';
import { RecipeCollection } from './RecipeCollection';

export class RecipeCollectionModel extends Model {
  static table = 'collections';
  static associations = {
    recipe_collections: { type: 'has_many' as const, foreignKey: 'collection_id' },
  };

  @field('name') name!: string;
  @field('description') description?: string;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @children('recipe_collections') recipeCollections!: RecipeCollection[];
}