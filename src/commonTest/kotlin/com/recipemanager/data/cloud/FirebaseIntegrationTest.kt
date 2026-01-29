package com.recipemanager.data.cloud

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FirebaseIntegrationTest : FunSpec({
    
    test("Firebase configuration should have required fields") {
        val config = FirebaseConfig.DEFAULT
        
        config.projectId shouldNotBe ""
        config.apiKey shouldNotBe ""
        config.authDomain shouldNotBe ""
        config.storageBucket shouldNotBe ""
        config.messagingSenderId shouldNotBe ""
        config.appId shouldNotBe ""
    }
    
    test("Firebase user should be created with correct properties") {
        val user = FirebaseUser(
            uid = "test-uid",
            email = "test@example.com",
            isAnonymous = false,
            displayName = "Test User"
        )
        
        user.uid shouldBe "test-uid"
        user.email shouldBe "test@example.com"
        user.isAnonymous shouldBe false
        user.displayName shouldBe "Test User"
    }
    
    test("Anonymous Firebase user should have correct properties") {
        val user = FirebaseUser(
            uid = "anonymous-uid",
            email = null,
            isAnonymous = true
        )
        
        user.uid shouldBe "anonymous-uid"
        user.email shouldBe null
        user.isAnonymous shouldBe true
        user.displayName shouldBe null
    }
})