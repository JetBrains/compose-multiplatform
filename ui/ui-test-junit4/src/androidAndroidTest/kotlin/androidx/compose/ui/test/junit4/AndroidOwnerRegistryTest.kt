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
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.test.junit4.android.AndroidOwnerRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class AndroidOwnerRegistryTest {

    private val activityRule = ActivityScenarioRule(ComponentActivity::class.java)
    private val androidOwnerRegistry = AndroidOwnerRegistry()

    @get:Rule
    val testRule: RuleChain = RuleChain
        .outerRule { base, _ -> androidOwnerRegistry.getStatementFor(base) }
        .around(activityRule)

    private val onRegistrationChangedListener =
        object : AndroidOwnerRegistry.OnRegistrationChangedListener {
            val recordedChanges = mutableListOf<Pair<AndroidOwner, Boolean>>()
            override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
                recordedChanges.add(Pair(owner, registered))
            }
        }

    @Before
    fun setUp() {
        assertThat(androidOwnerRegistry.getUnfilteredOwners()).isEmpty()
        androidOwnerRegistry.addOnRegistrationChangedListener(onRegistrationChangedListener)
    }

    @Test
    fun registryIsSetUpAndEmpty() {
        assertThat(androidOwnerRegistry.isSetUp).isTrue()
        assertThat(androidOwnerRegistry.getUnfilteredOwners()).isEmpty()
    }

    @Test
    fun registerOwner() {
        activityRule.scenario.onActivity { activity ->
            // set the composable content and find an owner
            activity.setContent { }
            val owner = activity.findOwner()

            // Then it is registered
            assertThat(androidOwnerRegistry.getUnfilteredOwners()).isEqualTo(setOf(owner))
            assertThat(androidOwnerRegistry.getOwners()).isEqualTo(setOf(owner))
            // And our listener was notified
            assertThat(onRegistrationChangedListener.recordedChanges).isEqualTo(
                listOf(Pair(owner, true))
            )
        }
    }

    @Test
    fun unregisterOwner() {
        activityRule.scenario.onActivity { activity ->
            // set the composable content and find an owner
            activity.setContent { }
            val owner = activity.findOwner()

            // And remove it from the hierarchy
            activity.setContentView(View(activity))

            // Then it is not registered now
            assertThat(androidOwnerRegistry.getUnfilteredOwners()).isEmpty()
            assertThat(androidOwnerRegistry.getOwners()).isEmpty()
            // But our listener was notified of addition and removal
            assertThat(onRegistrationChangedListener.recordedChanges).isEqualTo(
                listOf(
                    Pair(owner, true),
                    Pair(owner, false)
                )
            )
        }
    }

    @Test
    fun tearDownRegistry() {
        activityRule.scenario.onActivity { activity ->
            // set the composable content and find an owner
            activity.setContent { }
            val owner = activity.findOwner()

            // When we tear down the registry
            androidOwnerRegistry.tearDownRegistry()

            // Then the registry is empty
            assertThat(androidOwnerRegistry.getUnfilteredOwners()).isEmpty()
            assertThat(androidOwnerRegistry.getOwners()).isEmpty()
            // And our listener was notified of addition and removal
            assertThat(onRegistrationChangedListener.recordedChanges).isEqualTo(
                listOf(
                    Pair(owner, true),
                    Pair(owner, false)
                )
            )
        }
    }
}

private fun Activity.findOwner(): AndroidOwner {
    val viewGroup = findViewById<ViewGroup>(android.R.id.content)
    return requireNotNull(viewGroup.findOwner())
}

private fun View.findOwner(): AndroidOwner? {
    if (this is AndroidOwner) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val owner = getChildAt(i).findOwner()
            if (owner != null) {
                return owner
            }
        }
    }
    return null
}
