/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ObserverNodeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun simplyObservingValue_doesNotTriggerCallback() {
        // Arrange.
        val value by mutableStateOf(1)
        var callbackInvoked = false
        val observerNode = TestObserverNode { callbackInvoked = true }
        rule.setContent {
            Box(Modifier.modifierElementOf(observerNode))
        }

        // Act.
        rule.runOnIdle {
            // Read value to observe changes.
            observerNode.observeReads { value.toString() }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(callbackInvoked).isFalse()
        }
    }

    @Test
    fun changeInObservedValue_triggersCallback() {
        // Arrange.
        var value by mutableStateOf(1)
        var callbackInvoked = false
        val observerNode = TestObserverNode { callbackInvoked = true }
        rule.setContent {
            Box(Modifier.modifierElementOf(observerNode))
        }

        // Act.
        rule.runOnIdle {
            // Read value to observe changes.
            observerNode.observeReads { value.toString() }

            // Write to the read value to trigger onObservedReadsChanged.
            value = 3
        }

        // Assert.
        rule.runOnIdle {
            assertThat(callbackInvoked).isTrue()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun unusedNodeDoesNotObserve() {
        // Arrange.
        var value by mutableStateOf(1)
        var callbackInvoked = false
        val observerNode = TestObserverNode { callbackInvoked = true }

        // Act.
        rule.runOnIdle {
            // Read value to observe changes.
            observerNode.observeReads { value.toString() }

            // Write to the read value to trigger onObservedReadsChanged.
            value = 3
        }

        // Assert.
        rule.runOnIdle {
            assertThat(callbackInvoked).isFalse()
        }
    }

    @Test
    fun detachedNodeCanObserveReads() {
        // Arrange.
        var value by mutableStateOf(1)
        var callbackInvoked = false
        val observerNode = TestObserverNode { callbackInvoked = true }
        var attached by mutableStateOf(true)
        rule.setContent {
            Box(if (attached) Modifier.modifierElementOf(observerNode) else Modifier)
        }

        // Act.
        // Read value while not attached.
        rule.runOnIdle { attached = false }
        rule.runOnIdle { observerNode.observeReads { value.toString() } }
        rule.runOnIdle { attached = true }
        // Write to the read value to trigger onObservedReadsChanged.
        rule.runOnIdle { value = 3 }

        // Assert.
        rule.runOnIdle {
            assertThat(callbackInvoked).isTrue()
        }
    }

    @Test
    fun detachedNodeDoesNotCallOnObservedReadsChanged() {
        // Arrange.
        var value by mutableStateOf(1)
        var callbackInvoked = false
        val observerNode = TestObserverNode { callbackInvoked = true }
        var attached by mutableStateOf(true)
        rule.setContent {
            Box(if (attached) Modifier.modifierElementOf(observerNode) else Modifier)
        }

        // Act.
        rule.runOnIdle {
            // Read value to observe changes.
            observerNode.observeReads { value.toString() }
        }

        rule.runOnIdle {
            attached = false
        }
        // Write to the read value to trigger onObservedReadsChanged.
        rule.runOnIdle { value = 3 }

        // Assert.
        rule.runOnIdle {
            assertThat(callbackInvoked).isFalse()
        }
    }

    @ExperimentalComposeUiApi
    private inline fun <reified T : Modifier.Node> Modifier.modifierElementOf(node: T): Modifier {
        return this then ModifierElementOf(node)
    }

    private data class ModifierElementOf<T : Modifier.Node>(
        val node: T
    ) : ModifierNodeElement<T>() {
        override fun create(): T = node
        override fun update(node: T) = this.node
        override fun InspectorInfo.inspectableProperties() {
            name = "testNode"
        }
    }

    class TestObserverNode(
        private val onObserveReadsChanged: () -> Unit,
    ) : ObserverNode, Modifier.Node() {
        override fun onObservedReadsChanged() {
            this.onObserveReadsChanged()
        }
    }
}
