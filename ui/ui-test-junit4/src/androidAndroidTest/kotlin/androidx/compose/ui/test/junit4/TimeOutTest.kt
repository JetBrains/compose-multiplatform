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

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.expectError
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingPolicy
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TimeOutTest {

    private var idlingResourcePolicy: IdlingPolicy? = null
    private var masterPolicy: IdlingPolicy? = null

    private val idlingResourceTimeOut =
        "Idling resource timed out: possibly due to compose being busy\\..*" +
            "\\[busy\\] ComposeIdlingResource is busy due to .*pending recompositions.*\\..*"
    private val globalTimeOut =
        "Global time out: possibly due to compose being busy\\..*" +
            "\\[busy\\] ComposeIdlingResource is busy due to .*pending recompositions.*\\..*"

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
    private fun InfiniteRecompositionCase() {
        Box {
            val infiniteCounter = remember { mutableStateOf(0) }
            Text("Hello ${infiniteCounter.value}")
            SideEffect {
                infiniteCounter.value += 1
            }
        }
    }

    @Test(timeout = 10_000)
    fun infiniteRecompositions_resourceTimeout() = runComposeUiTest {
        IdlingPolicies.setIdlingResourceTimeout(300, TimeUnit.MILLISECONDS)

        expectError<ComposeNotIdleException>(expectedMessage = idlingResourceTimeOut) {
            setContent {
                InfiniteRecompositionCase()
            }
        }
    }

    @Test(timeout = 10_000)
    fun infiniteRecompositions_masterTimeout() = runComposeUiTest {
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)

        expectError<ComposeNotIdleException>(expectedMessage = globalTimeOut) {
            setContent {
                InfiniteRecompositionCase()
            }
        }
    }

    @Test(timeout = 10_000)
    fun delayInfiniteTrigger() = runComposeUiTest {
        // This test checks that we properly time out on infinite recompositions that happen later
        // down the road (not right during setContent).
        val count = mutableStateOf(0)
        setContent {
            Text("Hello ${count.value}")
            if (count.value > 0) {
                count.value++
            }
        }

        onNodeWithText("Hello 0").assertExists()

        count.value++ // Start infinite re-compositions

        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        expectError<ComposeNotIdleException>(expectedMessage = globalTimeOut) {
            onNodeWithText("Hello").assertExists()
        }
    }

    @Test(timeout = 10_000)
    fun emptyComposition_masterTimeout_fromIndependentIdlingResource() = runComposeUiTest {
        // This test checks that if we fail to sync on some unrelated idling resource we don't
        // override the vanilla errors from Espresso.

        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        IdlingRegistry.getInstance().register(InfiniteResource)

        expectError<AppNotIdleException> {
            setContent { }
        }
    }

    @Test(timeout = 10_000)
    fun checkIdlingResource_causesTimeout() = runComposeUiTest {
        // Block idleness with an IdlingResource
        registerIdlingResource(
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
            waitForIdle()
        }
    }

    /**
     * This test is here to guarantee that even if we crash on infinite recompositions during
     * setContent, the composition is disposed and won't keep running in the background.
     * This verifies that our tests run in isolation.
     */
    @Test(timeout = 10_000)
    fun timeout_testIsolation_check() {
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)

        // Test 1: set an infinite composition and expect it to crash
        expectError<ComposeNotIdleException> {
            runComposeUiTest {
                setContent {
                    InfiniteRecompositionCase()
                }
            }
        }

        // Test 2: normal composition, should not time out
        runComposeUiTest {
            setContent {
                Text("Hello")
            }
            // No timeout should happen this time
            onNodeWithText("Hello").assertExists()
        }
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