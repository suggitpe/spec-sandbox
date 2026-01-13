import { Model } from '@nozbe/watermelondb';
import { field, date, readonly } from '@nozbe/watermelondb/decorators';

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete'
}

export class SyncQueue extends Model {
  static table = 'sync_queue';

  @field('operation_type') operationTypeValue!: string;
  @field('table_name') tableName!: string;
  @field('record_id') recordId!: string;
  @field('data') dataJson!: string;
  @field('retry_count') retryCount!: number;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  // Helper getters and setters for enum
  get operationType(): OperationType {
    return this.operationTypeValue as OperationType;
  }

  set operationType(value: OperationType) {
    this.operationTypeValue = value;
  }

  // Helper methods for data
  get data(): any {
    try {
      return JSON.parse(this.dataJson || '{}');
    } catch {
      return {};
    }
  }

  set data(value: any) {
    this.dataJson = JSON.stringify(value);
  }
}