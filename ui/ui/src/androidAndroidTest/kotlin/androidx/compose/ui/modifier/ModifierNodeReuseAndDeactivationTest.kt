/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeReuseAndDeactivationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun reusingCallsResetOnModifier() {
        var reuseKey by mutableStateOf(0)

        var resetCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(onReset = { resetCalls++ })
            }
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(0)
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun nodeIsNotRecreatedWhenReused() {
        var reuseKey by mutableStateOf(0)

        var createCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(onCreate = { createCalls++ })
            }
        }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
        }
    }

    @Test
    fun resetIsCalledWhenContentIsDeactivated() {
        var active by mutableStateOf(true)
        var resetCalls = 0

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    TestLayout(onReset = { resetCalls++ })
                }
            }
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(0)
            active = false
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun resetIsCalledAgainWhenContentIsReactivated() {
        var active by mutableStateOf(true)
        var resetCalls = 0

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    TestLayout(onReset = { resetCalls++ })
                }
            }
        }

        rule.runOnIdle {
            active = false
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun updateIsNotCalledWhenReusedWithTheSameParams() {
        var reuseKey by mutableStateOf(0)
        var updateCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(
                    key = 1,
                    onUpdate = { updateCalls++ }
                )
            }
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(0)
            reuseKey++
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(0)
        }
    }

    @Test
    fun updateIsCalledWhenReusedWithDifferentParam() {
        var reuseKey by mutableStateOf(0)
        var updateCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(
                    key = reuseKey,
                    onUpdate = { updateCalls++ }
                )
            }
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(0)
            reuseKey++
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(1)
        }
    }

    @Test
    fun nodesAreDetachedWhenReused() {
        var reuseKey by mutableStateOf(0)

        var onResetCalls = 0
        var onAttachCalls = 0
        var onResetCallsWhenDetached: Int? = null

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(
                    onAttach = { onAttachCalls++ },
                    onReset = { onResetCalls++ },
                    onDetach = { onResetCallsWhenDetached = onResetCalls }
                )
            }
        }

        rule.runOnIdle {
            assertThat(onAttachCalls).isEqualTo(1)
            assertThat(onResetCallsWhenDetached).isNull()
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(onResetCalls).isEqualTo(1)
            // makes sure onReset is called before detach:
            assertThat(onResetCallsWhenDetached).isEqualTo(1)
            assertThat(onAttachCalls).isEqualTo(2)
        }
    }

    @Test
    fun nodesAreDetachedAndAttachedWhenDeactivatedAndReactivated() {
        var active by mutableStateOf(true)

        var onResetCalls = 0
        var onAttachCalls = 0
        var onResetCallsWhenDetached: Int? = null

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    TestLayout(
                        onAttach = { onAttachCalls++ },
                        onReset = { onResetCalls++ },
                        onDetach = { onResetCallsWhenDetached = onResetCalls }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(onAttachCalls).isEqualTo(1)
            assertThat(onResetCallsWhenDetached).isNull()
            active = false
        }

        rule.runOnIdle {
            assertThat(onResetCalls).isEqualTo(1)
            // makes sure onReset is called before detach:
            assertThat(onResetCallsWhenDetached).isEqualTo(1)
            assertThat(onAttachCalls).isEqualTo(1)
            active = true
        }

        rule.runOnIdle {
            assertThat(onAttachCalls).isEqualTo(2)
        }
    }

    @Test
    fun reusingStatelessModifierNotCausingInvalidation() {
        var active by mutableStateOf(true)
        var reuseKey by mutableStateOf(0)

        var invalidations = 0
        val onInvalidate: () -> Unit = {
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(reuseKey) {
                    Layout(
                        modifier = StatelessModifierElement(onInvalidate),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            active = true
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
        }
    }

    @Test
    fun reusingStatelessModifierWithUpdatedInputCausingInvalidation() {
        var active by mutableStateOf(true)
        var reuseKey by mutableStateOf(0)
        var size by mutableStateOf(10)

        var invalidations = 0
        val onInvalidate: () -> Unit = {
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(reuseKey) {
                    Layout(
                        modifier = StatelessModifierElement(onInvalidate, size),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            active = true
            reuseKey = 1
            size = 20
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
        }
    }

    @Test
    fun reusingModifierCausingInvalidationOnDelegatedInnerNode() {
        var reuseKey by mutableStateOf(0)

        var resetCalls = 0
        val onReset: () -> Unit = {
            resetCalls++
        }

        rule.setContent {
            ReusableContent(reuseKey) {
                Layout(
                    modifier = DelegatingModifierElement(onReset),
                    measurePolicy = MeasurePolicy
                )
            }
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(0)
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun reusingModifierReadingStateInLayerBlock() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val layerBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = LayerModifierElement(layerBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierObservingState() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val observedBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = ObserverModifierElement(observedBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierLocalProviderAndConsumer() {
        val key = modifierLocalOf { -1 }
        var active by mutableStateOf(true)
        var providedValue by mutableStateOf(0)

        var receivedValue: Int? = null

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = Modifier
                            .modifierLocalProvider(key) { providedValue }
                            .modifierLocalConsumer { receivedValue = key.current },
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(receivedValue).isEqualTo(0)
            active = false
        }

        rule.runOnIdle {
            providedValue = 1
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(receivedValue).isEqualTo(1)
        }
    }
}

@Composable
private fun TestLayout(
    key: Any? = null,
    onReset: () -> Unit = {},
    onCreate: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onDetach: () -> Unit = {},
    onAttach: () -> Unit = {}
) {
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnCreate by rememberUpdatedState(onCreate)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    val currentOnDetach by rememberUpdatedState(onDetach)
    val currentOnAttach by rememberUpdatedState(onAttach)
    Layout(
        modifier = createModifier(
            key = key,
            onCreate = { currentOnCreate.invoke() },
            onUpdate = { currentOnUpdate.invoke() },
            onReset = { currentOnReset.invoke() },
            onDetach = { currentOnDetach.invoke() },
            onAttach = { currentOnAttach.invoke() },
        ),
        measurePolicy = MeasurePolicy
    )
}

private fun createModifier(
    key: Any? = null,
    onCreate: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onReset: () -> Unit = {},
    onDetach: () -> Unit = {},
    onAttach: () -> Unit = {},
): Modifier {
    class GenericModifierWithLifecycle(
        val key: Any?
    ) : ModifierNodeElement<Modifier.Node>() {
        override fun create(): Modifier.Node {
            onCreate()
            return object : Modifier.Node() {
                override fun onReset() = onReset()
                override fun onAttach() = onAttach()
                override fun onDetach() = onDetach()
            }
        }

        override fun update(node: Modifier.Node) = node.apply { onUpdate() }

        override fun hashCode(): Int = "ModifierNodeReuseAndDeactivationTest".hashCode()

        override fun equals(other: Any?) = (other === this) ||
            (other is GenericModifierWithLifecycle && other.key == this.key)
    }
    return GenericModifierWithLifecycle(key)
}

private val MeasurePolicy = MeasurePolicy { _, _ ->
    layout(100, 100) { }
}

private data class StatelessModifierElement(
    private val onInvalidate: () -> Unit,
    private val size: Int = 10
) : ModifierNodeElement<StatelessModifierElement.Node>() {
    override fun create() = Node(size, onInvalidate)

    override fun update(node: Node) = node.also {
        it.size = size
        it.onMeasure = onInvalidate
    }

    class Node(var size: Int, var onMeasure: () -> Unit) : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(Constraints.fixed(size, size))
            onMeasure()
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}

private data class DelegatingModifierElement(
    private val onDelegatedNodeReset: () -> Unit,
) : ModifierNodeElement<DelegatingModifierElement.Node>() {
    override fun create() = Node(onDelegatedNodeReset)

    override fun update(node: Node) = node.also {
        it.onReset = onDelegatedNodeReset
    }

    class Node(var onReset: () -> Unit) : DelegatingNode() {
        private val inner = delegated {
            object : Modifier.Node() {
                override fun onReset() {
                    this@Node.onReset.invoke()
                }
            }
        }
    }
}

private data class LayerModifierElement(
    private val layerBlock: () -> Unit,
) : ModifierNodeElement<LayerModifierElement.Node>() {
    override fun create() = Node(layerBlock)

    override fun update(node: Node) = node.also {
        it.layerBlock = layerBlock
    }

    class Node(var layerBlock: () -> Unit) : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    layerBlock.invoke()
                }
            }
        }
    }
}

private data class ObserverModifierElement(
    private val observedBlock: () -> Unit,
) : ModifierNodeElement<ObserverModifierElement.Node>() {
    override fun create() = Node(observedBlock)

    override fun update(node: Node) = node.also {
        it.observedBlock = observedBlock
    }

    class Node(var observedBlock: () -> Unit) : Modifier.Node(), ObserverNode {

        override fun onAttach() {
            observe()
        }

        private fun observe() {
            observeReads {
                observedBlock()
            }
        }

        override fun onObservedReadsChanged() {
            observe()
        }
    }
}
