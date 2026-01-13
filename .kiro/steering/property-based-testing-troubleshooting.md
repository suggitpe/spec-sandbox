# Property-Based Testing Troubleshooting Guide

This steering file documents common issues encountered when implementing and debugging property-based tests, along with their solutions.

## Database-Related Issues

### Issue: Database File Locking in Tests
**Problem**: Tests fail with `EBUSY: resource busy or locked` errors when using SQLite file databases.

**Root Cause**: The main database instance is imported and initialized even during tests, creating file locks that conflict with test database operations.

**Solution**:
```typescript
// In main database configuration (src/database/index.ts)
const adapter = new SQLiteAdapter({
  schema,
  migrations,
  dbName: process.env.NODE_ENV === 'test' ? ':memory:' : 'RecipeManager',
  jsi: process.env.NODE_ENV !== 'test', // Disable JSI in test environment
  // ... other config
});
```

**Best Practice**: Always use in-memory databases (`:memory:`) for tests to avoid file system conflicts.

### Issue: Deleted Records Still Accessible
**Problem**: Property tests for deletion fail because deleted records can still be retrieved.

**Root Cause**: WatermelonDB's `find()` method can return soft-deleted records.

**Solution**: Use queries instead of direct `find()` calls:
```typescript
// Instead of:
const record = await this.db.get('table').find(id);

// Use:
const records = await this.db.get('table')
  .query(Q.where('id', id))
  .fetch();
const record = records.length > 0 ? records[0] : null;
```

## Data Type Handling Issues

### Issue: Null vs Undefined Mismatch
**Problem**: Property tests fail with "Expected: undefined, Received: null" errors.

**Root Cause**: Database optional fields return `null`, but TypeScript interfaces expect `undefined`.

**Solution**: Convert `null` to `undefined` in data transformation layers:
```typescript
// Correct conversion that preserves empty strings
description: recipeRecord.description === null ? undefined : recipeRecord.description,

// Avoid this - it converts empty strings to undefined too
description: recipeRecord.description || undefined,
```

**Best Practice**: Be explicit about null/undefined conversion and preserve meaningful empty values.

### Issue: NaN Values in Property Tests
**Problem**: Property tests fail with "Expected: NaN, Received: 0" errors when generators create NaN values.

**Root Cause**: Fast-check generators can produce NaN values, but databases convert NaN to 0 during storage. Validation logic may not properly check for NaN.

**Solution**: Add explicit NaN validation and filter generators:
```typescript
// In validation logic
if (isNaN(ingredient.quantity) || ingredient.quantity <= 0) {
  throw new Error(`Ingredient ${index + 1} must have a positive quantity`);
}

// In property test generators
quantity: fc.float({ min: Math.fround(0.1), max: Math.fround(1000) }).filter(n => !isNaN(n) && isFinite(n)),
```

**Best Practice**: Always validate for NaN and Infinity in numeric fields, and filter generators to exclude invalid values.

## Property Test Design Issues

### Issue: Order-Dependent Test Failures
**Problem**: Tests fail when database returns data in different order than input.

**Root Cause**: Comparing arrays by index when the service applies sorting logic.

**Solution**: Sort both expected and actual arrays before comparison:
```typescript
// Sort both arrays by the same criteria for comparison
const expectedSteps = [...recipeData.steps].sort((a, b) => a.stepNumber - b.stepNumber);
const actualSteps = [...retrievedRecipe.steps].sort((a, b) => a.stepNumber - b.stepNumber);

expectedSteps.forEach((step, index) => {
  const retrievedStep = actualSteps[index];
  // ... assertions
});
```

### Issue: Search Relevance Failures
**Problem**: Search property tests fail because irrelevant results are returned.

**Root Cause**: Database queries may return false positives due to partial matches or indexing issues. Additionally, inconsistent string processing between the service and test can cause mismatches.

**Solution**: Add post-query filtering for search relevance and ensure consistent string processing:
```typescript
// Filter results to ensure they actually contain the search term
return recipes.filter(recipe => {
  const titleMatch = recipe.title.toLowerCase().includes(searchTerm);
  const tagMatch = recipe.tags.some(tag => tag.toLowerCase().includes(searchTerm));
  const ingredientMatch = recipe.ingredients.some(ing => 
    ing.name.toLowerCase().includes(searchTerm)
  );
  return titleMatch || tagMatch || ingredientMatch;
});
```

**Additional Fix**: Ensure test verification uses the same string processing as the service:
```typescript
// In tests, use the same trimming logic as the service
const trimmedSearchTerm = searchTerm.toLowerCase().trim();
const titleMatch = recipe.title.toLowerCase().includes(trimmedSearchTerm);
// ... rest of verification
```

## Running Property-Based Tests

### Environment Setup
Always run property-based tests with proper environment configuration:
```bash
# Set NODE_ENV to test to use in-memory database
$env:NODE_ENV="test"; npm test -- --testNamePattern="Property"
```

### Test Isolation
Ensure each property test runs in isolation:
```typescript
beforeEach(async () => {
  testDatabase = createTestDatabase();
  await resetTestDatabase(testDatabase);
  recipeService = new RecipeService(testDatabase);
});

afterEach(async () => {
  // Clean up database connections
  if (testDatabase) {
    testDatabase = null as any;
  }
});
```

## Debugging Property Test Failures

### 1. Analyze the Counter-Example
When a property test fails, examine the generated counter-example:
- Look for edge cases in the input data
- Check for boundary conditions (empty strings, null values, etc.)
- Identify patterns in the failing data

### 2. Reproduce with Specific Values
Create a unit test with the failing counter-example to debug more easily:
```typescript
test('Debug specific counter-example', async () => {
  const counterExample = { /* paste counter-example here */ };
  // Test with specific values to understand the failure
});
```

### 3. Check Data Flow
Verify data transformation at each layer:
- Input validation
- Database storage
- Data retrieval
- Type conversion
- Output formatting

## Common Property Patterns for Recipe Management

### Data Persistence Properties
```typescript
// Round-trip property: create -> retrieve should preserve data
// Update property: updates should preserve creation date
// Deletion property: deleted items should be inaccessible
```

### Search Properties
```typescript
// Relevance property: all results should contain search term
// Completeness property: all matching items should be returned
```

### Validation Properties
```typescript
// Rejection property: invalid data should be rejected
// Acceptance property: valid data should be accepted
```

## Performance Considerations

- Use smaller data sets in property tests (limit array sizes)
- Reduce the number of test runs for complex operations
- Use `numRuns: 50` instead of default 100 for database operations
- Consider timeout settings for long-running property tests

## Best Practices Summary

1. **Always use in-memory databases for tests**
2. **Handle null/undefined conversion explicitly**
3. **Use queries instead of direct finds for soft-deleted records**
4. **Sort arrays before comparison when order matters**
5. **Add post-query filtering for search operations**
6. **Set proper environment variables for test runs**
7. **Analyze counter-examples systematically**
8. **Keep property test data sets reasonably small**

This guide should help avoid the most common pitfalls when implementing property-based tests for database-backed services.