/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.test.junit4

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingPolicy
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class TimeOutTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var idlingResourcePolicy: IdlingPolicy? = null
    private var masterPolicy: IdlingPolicy? = null
    // TODO(pavlis): Improve the error messages
    private val expectedErrorDueToRecompositions =
        ".*Idling resource timed out: possibly due to compose being busy.*"
    private val expectedErrorGlobal =
        ".*Global time out: possibly due to compose being busy.*"

    @Before
    fun backupTimeOutPolicies() {
        idlingResourcePolicy = IdlingPolicies.getDynamicIdlingResourceErrorPolicy()
        masterPolicy = IdlingPolicies.getMasterIdlingPolicy()
    }

    @After
    fun restoreTimeOutPolicies() {
        IdlingRegistry.getInstance().unregister(InfiniteResource)
        IdlingPolicies.setIdlingResourceTimeout(
            idlingResourcePolicy!!.idleTimeout, idlingResourcePolicy!!.idleTimeoutUnit
        )
        IdlingPolicies.setMasterPolicyTimeout(
            masterPolicy!!.idleTimeout, masterPolicy!!.idleTimeoutUnit
        )
    }

    @Composable
    fun InfiniteCase() {
        Box {
            val infiniteCounter = remember { mutableStateOf(0) }
            Box(
                Modifier.onGloballyPositioned {
                    infiniteCounter.value += 1
                }
            ) {
                Text("Hello")
            }

            Text("Hello ${infiniteCounter.value}")
        }
    }

    @Test(timeout = 5000)
    fun infiniteRecompositions_resourceTimeout() {
        IdlingPolicies.setIdlingResourceTimeout(300, TimeUnit.MILLISECONDS)

        expectError<ComposeNotIdleException>(expectedMessage = expectedErrorDueToRecompositions) {
            rule.setContent {
                InfiniteCase()
            }
        }
    }

    @Test(timeout = 5000)
    fun infiniteRecompositions_masterTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)

        expectError<ComposeNotIdleException>(expectedMessage = expectedErrorGlobal) {
            rule.setContent {
                InfiniteCase()
            }
        }
    }

    @Test(timeout = 5000)
    fun delayInfiniteTrigger() {
        // This test checks that we properly time out on infinite recompositions that happen later
        // down the road (not right during setContent).
        val count = mutableStateOf(0)
        rule.setContent {
            Text("Hello ${count.value}")
            if (count.value > 0) {
                count.value++
            }
        }

        rule.onNodeWithText("Hello 0").assertExists()

        count.value++ // Start infinite re-compositions

        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        expectError<ComposeNotIdleException>(expectedMessage = expectedErrorGlobal) {
            rule.onNodeWithText("Hello").assertExists()
        }
    }

    @Test(timeout = 10_000)
    fun emptyComposition_masterTimeout_fromIndependentIdlingResource() {
        // This test checks that if we fail to sync on some unrelated idling resource we don't
        // override the vanilla errors from Espresso.

        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        IdlingRegistry.getInstance().register(InfiniteResource)

        expectError<AppNotIdleException> {
            rule.setContent { }
        }
    }

    @Test(timeout = 10_000)
    fun checkIdlingResource_causesTimeout() {
        // Block idleness with an IdlingResource
        rule.registerIdlingResource(
            object : androidx.compose.ui.test.IdlingResource {
                override val isIdleNow: Boolean = false
                override fun getDiagnosticMessageIfBusy(): String {
                    return "Never IDLE"
                }
            }
        )
        IdlingPolicies.setIdlingResourceTimeout(300, TimeUnit.MILLISECONDS)
        expectError<ComposeNotIdleException>(
            expectedMessage = ".*\\[busy\\] Never IDLE.*\\[idle\\] .*ComposeIdlingResource.*"
        ) {
            rule.waitForIdle()
        }
    }

    @Test(timeout = 5000)
    fun timeout_testIsolation_check() {
        // This test is here to guarantee that even if we crash on infinite recompositions after
        // we set a content. We still recover and the old composition is no longer running in the
        // background causing further delays. This verifies that our tests run in isolation.
        val androidTestRule = rule

        // Start infinite case and die on infinite recompositions
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        expectError<ComposeNotIdleException> {
            rule.setContent {
                InfiniteCase()
            }
        }

        // Act like we are tearing down the test
        rule.runOnUiThread {
            androidTestRule.disposeContentHook!!.invoke()
            androidTestRule.disposeContentHook = null
        }

        // Kick of simple composition again (like if we run new test)
        rule.setContent {
            Text("Hello")
        }

        // No timeout should happen this time
        rule.onNodeWithText("Hello").assertExists()
    }

    private object InfiniteResource : IdlingResource {
        override fun getName(): String {
            return "InfiniteResource"
        }

        override fun isIdleNow(): Boolean {
            return false
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {}
    }
}