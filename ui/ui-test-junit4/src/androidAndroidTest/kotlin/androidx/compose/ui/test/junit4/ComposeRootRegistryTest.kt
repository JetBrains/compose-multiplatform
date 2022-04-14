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

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.ViewRootForTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runners.model.Statement

@LargeTest
class ComposeRootRegistryTest {

    private val activityRule = ActivityScenarioRule(ComponentActivity::class.java)
    private val composeRootRegistry = ComposeRootRegistry()

    @get:Rule
    val testRule: RuleChain = RuleChain
        .outerRule { base, _ ->
            object : Statement() {
                override fun evaluate() {
                    composeRootRegistry.withRegistry {
                        base.evaluate()
                    }
                }
            }
        }
        .around(activityRule)

    private val onRegistrationChangedListener =
        object : ComposeRootRegistry.OnRegistrationChangedListener {
            val recordedChanges = mutableListOf<Pair<ViewRootForTest, Boolean>>()
            override fun onRegistrationChanged(composeRoot: ViewRootForTest, registered: Boolean) {
                recordedChanges.add(Pair(composeRoot, registered))
            }
        }

    @Before
    fun setUp() {
        assertThat(composeRootRegistry.getCreatedComposeRoots()).isEmpty()
        composeRootRegistry.addOnRegistrationChangedListener(onRegistrationChangedListener)
    }

    @Test
    fun registryIsSetUpAndEmpty() {
        assertThat(composeRootRegistry.isSetUp).isTrue()
        assertThat(composeRootRegistry.getCreatedComposeRoots()).isEmpty()
    }

    @Test
    fun registerComposeRoot() {
        activityRule.scenario.onActivity { activity ->
            // set the composable content and find a compose root
            activity.setContent { }
            val composeRoot = activity.findRootForTest()

            // Then it is registered
            assertThat(composeRootRegistry.getCreatedComposeRoots())
                .isEqualTo(setOf(composeRoot))
            assertThat(composeRootRegistry.getRegisteredComposeRoots())
                .isEqualTo(setOf(composeRoot))
            // And our listener was notified
            assertThat(onRegistrationChangedListener.recordedChanges).isEqualTo(
                listOf(Pair(composeRoot, true))
            )
        }
    }

    @Test
    fun unregisterViewRoot() {
        activityRule.scenario.onActivity { activity ->
            // set the composable content and find a compose root
            activity.setContent { }
            val composeRoot = activity.findRootForTest()

            // And remove it from the hierarchy
            activity.setContentView(View(activity))

            // Then it is not registered now
            assertThat(composeRootRegistry.getRegisteredComposeRoots()).isEmpty()
            // But our listener was notified of addition and removal
            assertThat(onRegistrationChangedListener.recordedChanges).isEqualTo(
                listOf(
                    Pair(composeRoot, true),
                    Pair(composeRoot, false)
                )
            )
        }
    }

    @Test
    fun tearDownRegistry() {
        activityRule.scenario.onActivity { activity ->
            // set the composable content and find a compose root
            activity.setContent { }
            val composeRoot = activity.findRootForTest()

            // When we tear down the registry
            composeRootRegistry.tearDownRegistry()

            // Then the registry is empty
            assertThat(composeRootRegistry.getCreatedComposeRoots()).isEmpty()
            assertThat(composeRootRegistry.getRegisteredComposeRoots()).isEmpty()
            // And our listener was notified of addition and removal
            assertThat(onRegistrationChangedListener.recordedChanges).isEqualTo(
                listOf(
                    Pair(composeRoot, true),
                    Pair(composeRoot, false)
                )
            )
        }
    }
}

private fun Activity.findRootForTest(): ViewRootForTest {
    val viewGroup = findViewById<ViewGroup>(android.R.id.content)
    return requireNotNull(viewGroup.findRootForTest())
}

private fun View.findRootForTest(): ViewRootForTest? {
    if (this is ViewRootForTest) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val composeRoot = getChildAt(i).findRootForTest()
            if (composeRoot != null) {
                return composeRoot
            }
        }
    }
    return null
}
