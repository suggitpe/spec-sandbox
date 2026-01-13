plugins {
    kotlin("multiplatform") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
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
        
        val jvmMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
            }
        }
    }
}

sqldelight {
    databases {
        create("RecipeDatabase") {
            packageName.set("com.recipemanager.database")
        }
    }
}

// Configure default test task to clean, build and run all tests
tasks.register("testAll") {
    group = "verification"
    description = "Clean build and run all tests"
    dependsOn("clean", "jvmTest")
    
    // Ensure clean runs before jvmTest
    tasks.findByName("jvmTest")?.mustRunAfter("clean")
}

// Create a default test task that runs testAll
tasks.register("test") {
    group = "verification"
    description = "Run all tests (clean build)"
    dependsOn("testAll")
}

// Set the default task to be clean and test (which includes compilation)
defaultTasks("testAll")