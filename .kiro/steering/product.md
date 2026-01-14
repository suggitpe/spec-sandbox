# Recipe Manager Product Overview

Recipe Manager is a Kotlin Multiplatform mobile application for creating, managing, and sharing cooking recipes with integrated cooking assistance features.

## Core Features

- **Recipe Management**: Create, edit, delete, and organize recipes with ingredients, cooking steps, preparation/cooking times, and servings
- **Photo Documentation**: Associate photos with specific ingredients and cooking steps
- **Recipe Sharing**: Share recipes across multiple platforms
- **Recipe Versioning**: Create upgraded versions and track recipe history through parent-child relationships
- **Cooking Timers**: Multiple concurrent timers with notifications
- **Recipe Collections**: Organize recipes into curated collections
- **Offline-First**: Full functionality without internet connectivity
- **Cloud Sync**: Automatic synchronization when connectivity is available

## Target Platforms

- Android (primary)
- iOS (planned)
- JVM (development/testing)

## Architecture Philosophy

- **Clean Architecture**: Strict separation between domain, data, and presentation layers
- **Offline-First**: Local database as source of truth with eventual sync
- **Type Safety**: Leverage Kotlin's type system and SQLDelight for compile-time safety
- **Testability**: Property-based testing for universal business rules, unit tests for specific cases
