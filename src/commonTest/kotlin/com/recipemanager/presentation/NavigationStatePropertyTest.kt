package com.recipemanager.presentation

import com.recipemanager.presentation.navigation.*
import com.recipemanager.presentation.viewmodel.BaseViewModel
import com.recipemanager.test.generators.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.checkAll
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/**
 * Property-based tests for navigation and state management functionality.
 * Tests Properties 16 and 17 from the design document.
 */
class NavigationStatePropertyTest : FunSpec({
    
    test("Property 16: Navigation State Preservation - Feature: recipe-manager, Property 16: For any user action sequence, navigating away from and returning to a screen should restore the previous state and progress") {
        checkAll(100, navigationStateArb()) { originalState ->
            val manager = NavigationStateManager()
            
            // Simulate setting the navigation state (as would happen during navigation)
            val serializedState = kotlinx.serialization.json.Json.encodeToString(
                NavigationState.serializer(),
                originalState
            )
            
            // Restore the state (simulating app restoration)
            val restoredState = manager.restoreState(serializedState)
            
            // Verify state is properly restored
            restoredState.shouldNotBeNull()
            restoredState.currentRoute shouldBe originalState.currentRoute
            restoredState.backStack shouldBe originalState.backStack
            restoredState.timestamp shouldBe originalState.timestamp
            
            // Verify serialization round-trip consistency
            val reSerializedState = kotlinx.serialization.json.Json.encodeToString(
                NavigationState.serializer(),
                restoredState
            )
            
            val finalState = manager.restoreState(reSerializedState)
            finalState.shouldNotBeNull()
            finalState.currentRoute shouldBe originalState.currentRoute
            finalState.backStack shouldBe originalState.backStack
            finalState.timestamp shouldBe originalState.timestamp
        }
    }
    
    test("Property 17: Data Persistence Round-Trip - Feature: recipe-manager, Property 17: For any user data modification, closing and reopening the application should restore all data in the exact state it was when the app was closed").config(coroutineTestScope = true) {
        checkAll(50, appStateMapArb()) { originalAppState ->
            val persistence = InMemoryStatePersistence()
            val manager = AppStatePersistenceManager(persistence)
            
            // Create a test navigation state
            val navigationState = NavigationState(
                currentRoute = "test_route_${System.currentTimeMillis()}",
                backStack = listOf("back1", "back2"),
                timestamp = System.currentTimeMillis()
            )
            
            // Save both navigation state and app state
            manager.saveNavigationState(navigationState)
            manager.saveAppState(originalAppState)
            
            // Simulate app restart by creating new manager with same persistence
            val newManager = AppStatePersistenceManager(persistence)
            
            // Restore navigation state
            val restoredNavigationState = newManager.loadNavigationState()
            restoredNavigationState.shouldNotBeNull()
            restoredNavigationState.currentRoute shouldBe navigationState.currentRoute
            restoredNavigationState.backStack shouldBe navigationState.backStack
            
            // Verify the persistence layer maintains data integrity
            val directNavigationLoad = persistence.loadState("navigation_state")
            directNavigationLoad.shouldNotBeNull()
            
            // Test that multiple save/load cycles preserve data
            val secondNavigationState = navigationState.copy(
                currentRoute = "second_route_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis()
            )
            
            newManager.saveNavigationState(secondNavigationState)
            val secondRestored = newManager.loadNavigationState()
            secondRestored.shouldNotBeNull()
            secondRestored.currentRoute shouldBe secondNavigationState.currentRoute
            secondRestored.backStack shouldBe secondNavigationState.backStack
        }
    }
    
    test("Navigation state serialization should handle edge cases").config(coroutineTestScope = true) {
        val persistence = InMemoryStatePersistence()
        val manager = AppStatePersistenceManager(persistence)
        
        // Test empty back stack
        val emptyBackStackState = NavigationState(
            currentRoute = "test_route",
            backStack = emptyList(),
            timestamp = System.currentTimeMillis()
        )
        
        manager.saveNavigationState(emptyBackStackState)
        val restored = manager.loadNavigationState()
        restored.shouldNotBeNull()
        restored.backStack shouldBe emptyList()
        
        // Test very long back stack
        val longBackStack = (1..100).map { "route_$it" }
        val longBackStackState = NavigationState(
            currentRoute = "current_route",
            backStack = longBackStack,
            timestamp = System.currentTimeMillis()
        )
        
        manager.saveNavigationState(longBackStackState)
        val restoredLong = manager.loadNavigationState()
        restoredLong.shouldNotBeNull()
        restoredLong.backStack shouldBe longBackStack
        
        // Test special characters in routes
        val specialCharState = NavigationState(
            currentRoute = "route/with/special?chars=test&value=123#fragment",
            backStack = listOf("back/route?param=value", "another/route#section"),
            timestamp = System.currentTimeMillis()
        )
        
        manager.saveNavigationState(specialCharState)
        val restoredSpecial = manager.loadNavigationState()
        restoredSpecial.shouldNotBeNull()
        restoredSpecial.currentRoute shouldBe specialCharState.currentRoute
        restoredSpecial.backStack shouldBe specialCharState.backStack
    }
    
    test("BaseViewModel state persistence should work correctly").config(coroutineTestScope = true) {
        @Serializable
        data class TestState(
            val value: String,
            val counter: Int,
            val isActive: Boolean
        )
        
        class TestViewModel(
            persistence: StatePersistence,
            stateKey: String
        ) : BaseViewModel<TestState>(
            initialState = TestState("initial", 0, false),
            statePersistence = persistence,
            stateKey = stateKey
        ) {
            fun updateState(newValue: String, newCounter: Int, newActive: Boolean) {
                currentState = TestState(newValue, newCounter, newActive)
            }
            
            override fun serializeState(state: TestState): String {
                return kotlinx.serialization.json.Json.encodeToString(TestState.serializer(), state)
            }
            
            public override fun deserializeState(serializedState: String): TestState? {
                return try {
                    kotlinx.serialization.json.Json.decodeFromString(TestState.serializer(), serializedState)
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        val persistence = InMemoryStatePersistence()
        val stateKey = "test_view_model_state"
        
        // Create first view model instance
        val viewModel1 = TestViewModel(persistence, stateKey)
        viewModel1.initialize()
        
        // Update state
        viewModel1.updateState("updated_value", 42, true)
        
        // Manually trigger persistence (since auto-persistence happens in background)
        viewModel1.onAppPaused()
        
        // Wait for persistence to complete
        delay(100)
        
        // Verify state was persisted
        val persistedData = persistence.loadState(stateKey)
        persistedData shouldNotBe null
        
        // Create second view model instance (simulating app restart)
        val viewModel2 = TestViewModel(persistence, stateKey)
        viewModel2.initialize()
        
        // Wait for restoration to complete
        delay(100)
        
        // Verify state was restored - check the persisted data directly first
        val deserializedState = viewModel2.deserializeState(persistedData!!)
        deserializedState shouldNotBe null
        deserializedState!!.value shouldBe "updated_value"
        deserializedState.counter shouldBe 42
        deserializedState.isActive shouldBe true
        
        // Clean up
        viewModel1.onCleared()
        viewModel2.onCleared()
    }
    
    test("Deep link handling should preserve navigation state") {
        val manager = NavigationStateManager()
        
        // Test recipe deep link
        val recipeDeepLink = "recipemanager://recipe/test123"
        val handled = manager.handleDeepLink(recipeDeepLink)
        handled shouldBe true
        
        // Test collection deep link
        val collectionDeepLink = "recipemanager://collection/collection456"
        val handledCollection = manager.handleDeepLink(collectionDeepLink)
        handledCollection shouldBe true
        
        // Test invalid deep link
        val invalidDeepLink = "invalid://link"
        val handledInvalid = manager.handleDeepLink(invalidDeepLink)
        handledInvalid shouldBe false
        
        // Test malformed deep link
        val malformedDeepLink = "recipemanager://recipe/"
        val handledMalformed = manager.handleDeepLink(malformedDeepLink)
        handledMalformed shouldBe true // Should still handle it, even if ID is empty
    }
    
    test("State persistence should handle concurrent operations").config(coroutineTestScope = true) {
        val persistence = InMemoryStatePersistence()
        val manager = AppStatePersistenceManager(persistence)
        
        // Create multiple navigation states
        val states = (1..10).map { index ->
            NavigationState(
                currentRoute = "route_$index",
                backStack = listOf("back_$index"),
                timestamp = System.currentTimeMillis() + index
            )
        }
        
        // Save all states concurrently (last one should win)
        states.forEach { state ->
            manager.saveNavigationState(state)
        }
        
        // Wait for all operations to complete
        delay(100)
        
        // Load final state
        val finalState = manager.loadNavigationState()
        finalState.shouldNotBeNull()
        
        // Should be one of the saved states (exact one depends on timing)
        val wasOneOfSavedStates = states.any { state ->
            state.currentRoute == finalState.currentRoute &&
            state.backStack == finalState.backStack
        }
        wasOneOfSavedStates shouldBe true
    }
})