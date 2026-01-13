---
inclusion: always
---

# Kotlin Multiplatform, Kotest & Cucumber Serenity Development Guidelines

This steering file provides guidelines for developing the Recipe Manager application using Kotlin Multiplatform Mobile (KMM) with Kotest for unit/property testing and Cucumber Serenity for BDD feature testing.

## Project Architecture

### Source Set Structure
```
src/
├── commonMain/kotlin/          # Shared business logic
├── commonTest/kotlin/          # Shared tests (Kotest unit & property tests)
├── androidMain/kotlin/         # Android-specific implementations
├── androidTest/kotlin/         # Android-specific tests
├── iosMain/kotlin/             # iOS-specific implementations
├── iosTest/kotlin/             # iOS-specific tests
└── test/                       # Feature tests (Cucumber Serenity)
    ├── features/               # Gherkin feature files
    ├── screenplay/             # Screenplay pattern implementation
    │   ├── actors/             # Actor definitions
    │   ├── abilities/          # Actor abilities (UI automation, API calls)
    │   ├── tasks/              # High-level business tasks
    │   ├── interactions/       # Low-level UI interactions
    │   ├── questions/          # Assertions and verifications
    │   └── ui/                 # UI element locators and page objects
    ├── steps/                  # Step definitions (glue code)
    └── runners/                # Test runners and configuration
```

### Package Organization
```
com.recipemanager/
├── domain/                     # Business logic (pure Kotlin)
│   ├── model/                  # Data classes and enums
│   ├── repository/             # Repository interfaces
│   ├── usecase/                # Business use cases
│   └── validation/             # Business validation rules
├── data/                       # Data layer implementations
│   ├── repository/             # Repository implementations
│   ├── database/               # Database drivers and factories
│   └── network/                # Network clients
├── presentation/               # UI layer (Compose Multiplatform)
│   ├── screens/                # Screen composables
│   ├── components/             # Reusable UI components
│   └── viewmodel/              # ViewModels for state management
└── di/                         # Dependency injection
```

## Kotlin Multiplatform Best Practices

### Data Classes and Serialization
```kotlin
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String? = null,
    val ingredients: List<Ingredient>,
    val steps: List<CookingStep>,
    val preparationTime: Int, // in minutes
    val cookingTime: Int, // in minutes
    val servings: Int,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int = 1,
    val parentRecipeId: String? = null
)
```

### Expected/Actual Pattern for Platform-Specific Code
```kotlin
// commonMain
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(RecipeDatabase.Schema, context, "recipe_database.db")
    }
}

// iosMain
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(RecipeDatabase.Schema, "recipe_database.db")
    }
}
```

### Coroutines and Flow Usage
```kotlin
class RecipeRepository {
    suspend fun createRecipe(recipe: Recipe): Result<Recipe> = withContext(Dispatchers.IO) {
        // Implementation
    }
    
    fun observeRecipes(): Flow<List<Recipe>> = flow {
        // Implementation
    }
}
```

## Kotest Testing Guidelines

### Test Structure and Organization
Use Kotest's descriptive test styles for better readability:

```kotlin
class RecipeServiceTest : FunSpec({
    
    describe("Recipe creation") {
        test("should create recipe with valid data") {
            // Test implementation
        }
        
        test("should fail with invalid title") {
            // Test implementation
        }
    }
    
    describe("Recipe search") {
        test("should return relevant results") {
            // Test implementation
        }
    }
})
```

### Kotest Assertions
Use Kotest matchers for expressive assertions:

```kotlin
// Basic assertions
result shouldBe expected
result shouldNotBe null
result.isSuccess shouldBe true

// Collection assertions
recipes shouldHaveSize 3
recipes shouldContain expectedRecipe
recipes.map { it.title } shouldContainExactly listOf("Recipe 1", "Recipe 2", "Recipe 3")

// String assertions
recipe.title shouldStartWith "Pasta"
recipe.description shouldMatch Regex(".*delicious.*")

// Exception assertions
shouldThrow<IllegalArgumentException> {
    validator.validateRecipe(invalidRecipe)
}

// Nullable assertions
recipe.description.shouldNotBeNull()
recipe.parentRecipeId.shouldBeNull()
```

### Property-Based Testing with Kotest
Use Kotest's property testing for comprehensive validation:

```kotlin
class RecipePropertyTest : FunSpec({
    
    test("Property 1: Recipe Creation Completeness") {
        checkAll(100, recipeArb()) { recipe ->
            // Create recipe
            val createResult = recipeService.createRecipe(recipe)
            createResult.isSuccess shouldBe true
            
            val createdRecipe = createResult.getOrThrow()
            
            // Retrieve recipe
            val retrieveResult = recipeService.getRecipe(createdRecipe.id)
            retrieveResult.isSuccess shouldBe true
            
            val retrievedRecipe = retrieveResult.getOrThrow()
            retrievedRecipe.shouldNotBeNull()
            
            // Verify all data is preserved
            retrievedRecipe.title shouldBe recipe.title
            retrievedRecipe.description shouldBe recipe.description
            retrievedRecipe.ingredients shouldContainExactly recipe.ingredients
            retrievedRecipe.steps.sortedBy { it.stepNumber } shouldContainExactly 
                recipe.steps.sortedBy { it.stepNumber }
        }
    }
})
```

### Custom Arbitraries (Generators)
Create reusable generators for domain objects:

```kotlin
fun recipeArb(): Arb<Recipe> = arbitrary { rs ->
    Recipe(
        id = Arb.string(1..50).bind(),
        title = Arb.string(1..100).filter { it.isNotBlank() }.bind(),
        description = Arb.string(0..500).orNull().bind(),
        ingredients = Arb.list(ingredientArb(), 1..10).bind(),
        steps = Arb.list(cookingStepArb(), 1..15).bind(),
        preparationTime = Arb.int(1..480).bind(),
        cookingTime = Arb.int(1..480).bind(),
        servings = Arb.int(1..20).bind(),
        tags = Arb.list(Arb.string(1..30), 0..5).bind(),
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        version = 1,
        parentRecipeId = Arb.string(1..50).orNull(0.8).bind()
    )
}

fun ingredientArb(): Arb<Ingredient> = arbitrary { rs ->
    Ingredient(
        id = Arb.string(1..50).bind(),
        name = Arb.string(1..50).filter { it.isNotBlank() }.bind(),
        quantity = Arb.double(0.1..1000.0).bind(),
        unit = Arb.element("cup", "tbsp", "tsp", "oz", "lb", "g", "kg").bind(),
        notes = Arb.string(0..200).orNull().bind(),
        photos = emptyList() // Photos handled separately
    )
}
```

### Test Configuration
Configure Kotest for consistent behavior:

```kotlin
// In test resources: kotest.properties
kotest.framework.timeout=60000
kotest.framework.invocation.timeout=30000
kotest.assertions.multi.line.diff=unified

// In test class
class RecipeTest : FunSpec({
    
    // Test configuration
    timeout = 30.seconds
    
    // Setup and teardown
    beforeEach {
        // Reset test database
    }
    
    afterEach {
        // Cleanup
    }
})
```

## SQLDelight Integration

### Schema Definition
```sql
-- In commonMain/sqldelight/
CREATE TABLE Recipe (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    preparationTime INTEGER NOT NULL,
    cookingTime INTEGER NOT NULL,
    servings INTEGER NOT NULL,
    tags TEXT NOT NULL, -- JSON array
    createdAt INTEGER NOT NULL, -- Unix timestamp
    updatedAt INTEGER NOT NULL, -- Unix timestamp
    version INTEGER NOT NULL DEFAULT 1,
    parentRecipeId TEXT
);

-- Queries
selectAllRecipes:
SELECT * FROM Recipe ORDER BY updatedAt DESC;

insertRecipe:
INSERT INTO Recipe (id, title, description, preparationTime, cookingTime, servings, tags, createdAt, updatedAt, version, parentRecipeId)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

### Repository Implementation
```kotlin
class RecipeRepositoryImpl(
    private val database: RecipeDatabase
) : RecipeRepository {
    
    override suspend fun createRecipe(recipe: Recipe): Result<Recipe> = withContext(Dispatchers.IO) {
        try {
            database.recipeQueries.insertRecipe(
                id = recipe.id,
                title = recipe.title,
                description = recipe.description,
                preparationTime = recipe.preparationTime.toLong(),
                cookingTime = recipe.cookingTime.toLong(),
                servings = recipe.servings.toLong(),
                tags = Json.encodeToString(recipe.tags),
                createdAt = recipe.createdAt.epochSeconds,
                updatedAt = recipe.updatedAt.epochSeconds,
                version = recipe.version.toLong(),
                parentRecipeId = recipe.parentRecipeId
            )
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Dependency Injection

### Manual DI with Factory Pattern
```kotlin
class AppModule(private val databaseDriverFactory: DatabaseDriverFactory) {
    
    private val database: RecipeDatabase by lazy {
        RecipeDatabase(databaseDriverFactory.createDriver())
    }
    
    private val recipeRepository: RecipeRepository by lazy {
        RecipeRepositoryImpl(database)
    }
    
    val recipeUseCases: RecipeUseCases by lazy {
        RecipeUseCases(recipeRepository, RecipeValidator())
    }
}
```

## Error Handling

### Result Pattern Usage
```kotlin
// Use Result<T> for operations that can fail
suspend fun createRecipe(recipe: Recipe): Result<Recipe> {
    return try {
        validator.validateRecipe(recipe)
        repository.createRecipe(recipe)
    } catch (e: ValidationException) {
        Result.failure(e)
    } catch (e: DatabaseException) {
        Result.failure(e)
    }
}

// In tests, verify both success and failure cases
test("should return failure for invalid recipe") {
    val invalidRecipe = Recipe(/* invalid data */)
    
    val result = recipeService.createRecipe(invalidRecipe)
    
    result.isFailure shouldBe true
    result.exceptionOrNull() shouldBeInstanceOf<ValidationException>()
}
```

## Performance Considerations

### Database Operations
- Use transactions for multiple related operations
- Implement proper indexing in SQLDelight schema
- Use Flow for reactive data updates
- Batch operations when possible

### Memory Management
- Use `@Parcelize` for Android data classes that need to be passed between activities
- Implement proper cleanup in ViewModels
- Use weak references for callbacks when necessary

## Platform-Specific Guidelines

### Android
- Use `Context` for database initialization
- Implement proper lifecycle handling in ViewModels
- Use Android-specific APIs for camera, notifications, etc.

### iOS
- Use proper memory management patterns
- Implement platform-specific UI adaptations
- Handle iOS-specific permissions and capabilities

## Testing Best Practices

### Testing Strategy Overview
1. **Unit Tests (Kotest)**: Fast, focused tests for individual components
2. **Property Tests (Kotest)**: Comprehensive validation of universal business rules  
3. **Feature Tests (Cucumber Serenity)**: End-to-end BDD scenarios using Screenplay pattern
4. **Integration Tests**: Test component interactions
5. **Mock Usage**: Minimize mocking, prefer real implementations in tests
6. **Test Data**: Use generators for comprehensive test coverage
7. **Assertions**: Use expressive Kotest matchers for clear test failures

## Cucumber Serenity with Screenplay Pattern

### Feature File Structure
Write business-readable scenarios in Gherkin syntax:

```gherkin
# features/recipe_management.feature
Feature: Recipe Management
  As a home cook
  I want to manage my recipes
  So that I can organize my cooking knowledge

  Background:
    Given the Recipe Manager app is launched
    And I am on the main screen

  Scenario: Creating a new recipe
    When I choose to create a new recipe
    And I enter the recipe title "Pasta Carbonara"
    And I add ingredient "Spaghetti" with quantity "400g"
    And I add ingredient "Eggs" with quantity "4 pieces"
    And I add cooking step "Boil pasta in salted water"
    And I save the recipe
    Then the recipe should be created successfully
    And I should see "Pasta Carbonara" in my recipe list

  Scenario: Searching for recipes
    Given I have recipes in my collection:
      | title           | tags        |
      | Pasta Carbonara | pasta, easy |
      | Beef Stew       | meat, slow  |
    When I search for "pasta"
    Then I should see 1 recipe in the results
    And the result should contain "Pasta Carbonara"

  Scenario Outline: Recipe validation
    When I try to create a recipe with title "<title>" and ingredients "<ingredients>"
    Then the recipe creation should "<result>"
    And I should see the message "<message>"

    Examples:
      | title    | ingredients | result | message                    |
      |          | Flour       | fail   | Recipe title is required   |
      | Pancakes |             | fail   | At least one ingredient required |
      | Pancakes | Flour, Eggs | succeed| Recipe created successfully |
```

### Screenplay Pattern Implementation

#### Actors
```kotlin
// screenplay/actors/Actor.kt
class RecipeManagerUser(name: String) : Actor(name) {
    
    companion object {
        fun named(name: String): RecipeManagerUser {
            return RecipeManagerUser(name).whoCan(
                BrowseTheApp.using(AppDriver.instance()),
                ManageRecipes.using(RecipeAPI.instance()),
                RememberThings.using(Memory())
            )
        }
    }
}
```

#### Abilities
```kotlin
// screenplay/abilities/BrowseTheApp.kt
class BrowseTheApp private constructor(private val driver: AppDriver) : Ability {
    
    companion object {
        fun using(driver: AppDriver): BrowseTheApp = BrowseTheApp(driver)
    }
    
    fun findElement(locator: By): WebElement = driver.findElement(locator)
    fun findElements(locator: By): List<WebElement> = driver.findElements(locator)
    fun tap(locator: By) = driver.tap(locator)
    fun enterText(locator: By, text: String) = driver.enterText(locator, text)
}

// screenplay/abilities/ManageRecipes.kt
class ManageRecipes private constructor(private val api: RecipeAPI) : Ability {
    
    companion object {
        fun using(api: RecipeAPI): ManageRecipes = ManageRecipes(api)
    }
    
    fun createRecipe(recipe: Recipe): Result<Recipe> = api.createRecipe(recipe)
    fun getRecipes(): List<Recipe> = api.getAllRecipes()
    fun searchRecipes(query: String): List<Recipe> = api.searchRecipes(query)
}
```

#### Tasks (High-level business actions)
```kotlin
// screenplay/tasks/CreateRecipe.kt
class CreateRecipe private constructor(
    private val title: String,
    private val ingredients: List<Ingredient>,
    private val steps: List<CookingStep>
) : Task {
    
    companion object {
        fun withDetails(title: String): CreateRecipeBuilder = CreateRecipeBuilder(title)
    }
    
    override fun <T : Actor> performAs(actor: T): T {
        return actor.attemptsTo(
            NavigateToCreateRecipe(),
            EnterRecipeTitle(title),
            *ingredients.map { AddIngredient(it.name, it.quantity, it.unit) }.toTypedArray(),
            *steps.map { AddCookingStep(it.instruction) }.toTypedArray(),
            SaveRecipe()
        )
    }
    
    class CreateRecipeBuilder(private val title: String) {
        private val ingredients = mutableListOf<Ingredient>()
        private val steps = mutableListOf<CookingStep>()
        
        fun withIngredient(name: String, quantity: String): CreateRecipeBuilder {
            ingredients.add(Ingredient(UUID.randomUUID().toString(), name, quantity.toDouble(), "g"))
            return this
        }
        
        fun withStep(instruction: String): CreateRecipeBuilder {
            steps.add(CookingStep(UUID.randomUUID().toString(), steps.size + 1, instruction))
            return this
        }
        
        fun build(): CreateRecipe = CreateRecipe(title, ingredients, steps)
    }
}

// screenplay/tasks/SearchForRecipes.kt
class SearchForRecipes private constructor(private val query: String) : Task {
    
    companion object {
        fun withQuery(query: String): SearchForRecipes = SearchForRecipes(query)
    }
    
    override fun <T : Actor> performAs(actor: T): T {
        return actor.attemptsTo(
            NavigateToSearch(),
            EnterSearchQuery(query),
            TapSearchButton()
        )
    }
}
```

#### Interactions (Low-level UI actions)
```kotlin
// screenplay/interactions/NavigateToCreateRecipe.kt
class NavigateToCreateRecipe : Interaction {
    
    override fun <T : Actor> performAs(actor: T): T {
        val browseApp = BrowseTheApp.`as`(actor)
        browseApp.tap(MainScreen.CREATE_RECIPE_BUTTON)
        return actor
    }
}

// screenplay/interactions/EnterRecipeTitle.kt
class EnterRecipeTitle private constructor(private val title: String) : Interaction {
    
    companion object {
        fun withValue(title: String): EnterRecipeTitle = EnterRecipeTitle(title)
    }
    
    override fun <T : Actor> performAs(actor: T): T {
        val browseApp = BrowseTheApp.`as`(actor)
        browseApp.enterText(CreateRecipeScreen.TITLE_FIELD, title)
        return actor
    }
}

// screenplay/interactions/AddIngredient.kt
class AddIngredient private constructor(
    private val name: String,
    private val quantity: String,
    private val unit: String
) : Interaction {
    
    companion object {
        fun withDetails(name: String, quantity: String, unit: String = "g"): AddIngredient {
            return AddIngredient(name, quantity, unit)
        }
    }
    
    override fun <T : Actor> performAs(actor: T): T {
        val browseApp = BrowseTheApp.`as`(actor)
        return actor.attemptsTo(
            Tap.on(CreateRecipeScreen.ADD_INGREDIENT_BUTTON),
            EnterText.into(CreateRecipeScreen.INGREDIENT_NAME_FIELD).withValue(name),
            EnterText.into(CreateRecipeScreen.INGREDIENT_QUANTITY_FIELD).withValue(quantity),
            SelectOption.from(CreateRecipeScreen.INGREDIENT_UNIT_DROPDOWN).withValue(unit),
            Tap.on(CreateRecipeScreen.CONFIRM_INGREDIENT_BUTTON)
        )
    }
}
```

#### Questions (Assertions and verifications)
```kotlin
// screenplay/questions/RecipeList.kt
class RecipeList : Question<List<String>> {
    
    companion object {
        fun displayed(): RecipeList = RecipeList()
    }
    
    override fun answeredBy(actor: Actor): List<String> {
        val browseApp = BrowseTheApp.`as`(actor)
        return browseApp.findElements(RecipeListScreen.RECIPE_ITEMS)
            .map { it.text }
    }
}

// screenplay/questions/SearchResults.kt
class SearchResults : Question<List<Recipe>> {
    
    companion object {
        fun displayed(): SearchResults = SearchResults()
    }
    
    override fun answeredBy(actor: Actor): List<Recipe> {
        val browseApp = BrowseTheApp.`as`(actor)
        return browseApp.findElements(SearchScreen.RESULT_ITEMS)
            .map { element ->
                Recipe(
                    id = element.getAttribute("data-recipe-id"),
                    title = element.findElement(By.className("recipe-title")).text,
                    // ... map other fields
                )
            }
    }
}

// screenplay/questions/ValidationMessage.kt
class ValidationMessage : Question<String> {
    
    companion object {
        fun displayed(): ValidationMessage = ValidationMessage()
    }
    
    override fun answeredBy(actor: Actor): String {
        val browseApp = BrowseTheApp.`as`(actor)
        return browseApp.findElement(CommonElements.ERROR_MESSAGE).text
    }
}
```

#### UI Element Locators
```kotlin
// screenplay/ui/MainScreen.kt
object MainScreen {
    val CREATE_RECIPE_BUTTON = By.id("create_recipe_button")
    val RECIPE_LIST = By.id("recipe_list")
    val SEARCH_BUTTON = By.id("search_button")
}

// screenplay/ui/CreateRecipeScreen.kt
object CreateRecipeScreen {
    val TITLE_FIELD = By.id("recipe_title_input")
    val DESCRIPTION_FIELD = By.id("recipe_description_input")
    val ADD_INGREDIENT_BUTTON = By.id("add_ingredient_button")
    val INGREDIENT_NAME_FIELD = By.id("ingredient_name_input")
    val INGREDIENT_QUANTITY_FIELD = By.id("ingredient_quantity_input")
    val INGREDIENT_UNIT_DROPDOWN = By.id("ingredient_unit_dropdown")
    val CONFIRM_INGREDIENT_BUTTON = By.id("confirm_ingredient_button")
    val ADD_STEP_BUTTON = By.id("add_step_button")
    val STEP_INSTRUCTION_FIELD = By.id("step_instruction_input")
    val SAVE_RECIPE_BUTTON = By.id("save_recipe_button")
}
```

### Step Definitions (Glue Code)
```kotlin
// steps/RecipeManagementSteps.kt
class RecipeManagementSteps {
    
    private val user = RecipeManagerUser.named("Test User")
    
    @Given("the Recipe Manager app is launched")
    fun theAppIsLaunched() {
        user.attemptsTo(LaunchApp())
    }
    
    @Given("I am on the main screen")
    fun iAmOnTheMainScreen() {
        user.should(seeThat(CurrentScreen.displayed(), equalTo("MainScreen")))
    }
    
    @When("I choose to create a new recipe")
    fun iChooseToCreateNewRecipe() {
        user.attemptsTo(NavigateToCreateRecipe())
    }
    
    @When("I enter the recipe title {string}")
    fun iEnterRecipeTitle(title: String) {
        user.attemptsTo(EnterRecipeTitle.withValue(title))
    }
    
    @When("I add ingredient {string} with quantity {string}")
    fun iAddIngredient(name: String, quantity: String) {
        user.attemptsTo(AddIngredient.withDetails(name, quantity))
    }
    
    @When("I add cooking step {string}")
    fun iAddCookingStep(instruction: String) {
        user.attemptsTo(AddCookingStep.withInstruction(instruction))
    }
    
    @When("I save the recipe")
    fun iSaveTheRecipe() {
        user.attemptsTo(SaveRecipe())
    }
    
    @Then("the recipe should be created successfully")
    fun theRecipeShouldBeCreatedSuccessfully() {
        user.should(seeThat(ValidationMessage.displayed(), containsString("successfully")))
    }
    
    @Then("I should see {string} in my recipe list")
    fun iShouldSeeInRecipeList(recipeName: String) {
        user.should(seeThat(RecipeList.displayed(), hasItem(recipeName)))
    }
    
    @Given("I have recipes in my collection:")
    fun iHaveRecipesInCollection(recipes: DataTable) {
        recipes.asMaps().forEach { row ->
            val recipe = Recipe(
                id = UUID.randomUUID().toString(),
                title = row["title"]!!,
                tags = row["tags"]!!.split(", "),
                // ... other fields with defaults
            )
            user.attemptsTo(CreateRecipeViaAPI.withDetails(recipe))
        }
    }
    
    @When("I search for {string}")
    fun iSearchFor(query: String) {
        user.attemptsTo(SearchForRecipes.withQuery(query))
    }
    
    @Then("I should see {int} recipe(s) in the results")
    fun iShouldSeeRecipesInResults(count: Int) {
        user.should(seeThat(SearchResults.displayed(), hasSize(count)))
    }
}
```

### Test Runner Configuration
```kotlin
// runners/CucumberTestRunner.kt
@RunWith(CucumberWithSerenity::class)
@CucumberOptions(
    features = ["src/test/features"],
    glue = ["steps"],
    plugin = ["pretty", "html:target/cucumber-reports"],
    tags = "not @ignore"
)
class CucumberTestRunner

// Configuration for Serenity properties
// serenity.properties
serenity.project.name=Recipe Manager
serenity.test.root=com.recipemanager.test
serenity.outputDirectory=target/site/serenity
serenity.reports.show.step.details=true
serenity.take.screenshots=FOR_FAILURES
serenity.restart.browser.for.each=scenario
```

### Gradle Configuration for Latest Kotlin
```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "1.9.21" // Latest stable version
    kotlin("plugin.serialization") version "1.9.21"
    id("com.android.library") version "8.2.0"
    id("org.jetbrains.compose") version "1.5.11"
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    jvmToolchain(17) // Use Java 17 for better performance
    
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                implementation("co.touchlab:kermit:2.0.2")
                
                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
            }
        }
        
        val commonTest by getting {
            dependencies {
                // Kotest for unit and property testing
                implementation("io.kotest:kotest-framework-engine:5.8.0")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("io.kotest:kotest-property:5.8.0")
                implementation("io.kotest:kotest-framework-datatest:5.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val jvmTest by getting {
            dependencies {
                // Cucumber Serenity for BDD testing
                implementation("net.serenity-bdd:serenity-core:4.0.19")
                implementation("net.serenity-bdd:serenity-cucumber:4.0.19")
                implementation("net.serenity-bdd:serenity-screenplay:4.0.19")
                implementation("net.serenity-bdd:serenity-screenplay-webdriver:4.0.19")
                implementation("io.cucumber:cucumber-java:7.15.0")
                implementation("io.cucumber:cucumber-junit:7.15.0")
                implementation("org.seleniumhq.selenium:selenium-java:4.16.1")
                implementation("io.appium:java-client:9.0.0")
            }
        }
    }
}
```

## Common Patterns

### Repository Pattern
```kotlin
interface RecipeRepository {
    suspend fun createRecipe(recipe: Recipe): Result<Recipe>
    suspend fun getRecipe(id: String): Result<Recipe?>
    fun observeRecipes(): Flow<List<Recipe>>
}
```

### Use Case Pattern
```kotlin
class CreateRecipeUseCase(
    private val repository: RecipeRepository,
    private val validator: RecipeValidator
) {
    suspend operator fun invoke(recipe: Recipe): Result<Recipe> {
        return try {
            validator.validateRecipe(recipe)
            repository.createRecipe(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

This steering file should guide all development decisions and ensure consistency across the Kotlin Multiplatform project with proper Kotest usage.