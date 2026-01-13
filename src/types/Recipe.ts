export interface Recipe {
  id: string;
  title: string;
  description?: string;
  ingredients: Ingredient[];
  steps: CookingStep[];
  preparationTime: number;
  cookingTime: number;
  servings: number;
  tags: string[];
  createdAt: Date;
  updatedAt: Date;
  version: number;
  parentRecipeId?: string; // For recipe upgrades
}

export interface Ingredient {
  id: string;
  name: string;
  quantity: number;
  unit: string;
  notes?: string;
  photos: Photo[];
}

export interface CookingStep {
  id: string;
  stepNumber: number;
  instruction: string;
  duration?: number; // in minutes
  temperature?: number;
  photos: Photo[];
  timerRequired: boolean;
}

export interface Photo {
  id: string;
  localPath: string;
  cloudUrl?: string;
  caption?: string;
  stage: PhotoStage;
  timestamp: Date;
  syncStatus: SyncStatus;
}

export enum PhotoStage {
  RAW_INGREDIENTS = 'raw_ingredients',
  PROCESSED_INGREDIENTS = 'processed_ingredients',
  COOKING_STEP = 'cooking_step',
  FINAL_RESULT = 'final_result'
}

export enum SyncStatus {
  LOCAL_ONLY = 'local_only',
  SYNCING = 'syncing',
  SYNCED = 'synced',
  SYNC_FAILED = 'sync_failed'
}

export interface CookingTimer {
  id: string;
  recipeId: string;
  stepId: string;
  duration: number; // in seconds
  remainingTime: number;
  status: TimerStatus;
  createdAt: Date;
}

export enum TimerStatus {
  READY = 'ready',
  RUNNING = 'running',
  PAUSED = 'paused',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled'
}