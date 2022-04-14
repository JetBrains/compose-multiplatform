/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierLocalProviderConsumerOrderTest {

    @get:Rule
    val rule = createComposeRule()

    private val defaultValue = "Default Value"
    private val modifierLocal = modifierLocalOf { defaultValue }

    @Test
    fun modifierDoesNotConsumeTheValueItProduced() {
        // Arrange.
        val providedValue = "Provided Value"
        lateinit var consumedValue: String
        rule.setContent {
            Box(
                ProviderConsumerModifier(modifierLocal, { providedValue }) {
                    consumedValue = modifierLocal.current
                }
            )
        }

        // Assert.
        assertThat(consumedValue).isEqualTo(defaultValue)
    }

    @Test
    fun modifierConsumesTheValueProducedByParent() {
        // Arrange.
        val parentProvidedValue = "Parent Provided Value"
        val providedValue = "Provided Value"
        lateinit var consumedValue: String
        rule.setContent {
            Box(ProviderConsumerModifier(modifierLocal, { parentProvidedValue }) {}) {
                Box(
                    ProviderConsumerModifier(modifierLocal, { providedValue }) {
                        consumedValue = modifierLocal.current
                    }
                )
            }
        }

        // Assert.
        assertThat(consumedValue).isEqualTo(parentProvidedValue)
    }

    @Test
    fun detachedProducerStopsProducing() {
        var useInnerModifier by mutableStateOf(true)
        var consumerValue by mutableStateOf("")
        rule.setContent {
            Box(ProviderModifier(modifierLocal) { "Outer" }) {
                val modifier = if (useInnerModifier) {
                    ProviderModifier(modifierLocal) { "Inner" }
                } else {
                    Modifier
                }
                Box(modifier) {
                    Box(ProviderConsumerModifier(modifierLocal, { "" }) {
                        consumerValue = modifierLocal.current
                    })
                }
            }
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo("Inner")

            // remove the inner modifier
            useInnerModifier = false
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo("Outer")
        }
    }

    @Test
    fun detachedProducerChangesToDefault() {
        var useProvider by mutableStateOf(true)
        var consumerValue by mutableStateOf("")
        rule.setContent {
            val modifier = if (useProvider) {
                ProviderModifier(modifierLocal) { "Provided Value" }
            } else {
                Modifier
            }
            Box(modifier) {
                Box(ProviderConsumerModifier(modifierLocal, { "" }) {
                    consumerValue = modifierLocal.current
                })
            }
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo("Provided Value")

            // remove the inner modifier
            useProvider = false
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo(defaultValue)
        }
    }

    @Test
    fun attachedProducerStartsProducing() {
        var useInnerModifier by mutableStateOf(false)
        var consumerValue by mutableStateOf("")
        rule.setContent {
            Box(ProviderModifier(modifierLocal) { "Outer" }) {
                val modifier = if (useInnerModifier) {
                    ProviderModifier(modifierLocal) { "Inner" }
                } else {
                    Modifier
                }
                Box(modifier) {
                    Box(ProviderConsumerModifier(modifierLocal, { "" }) {
                        consumerValue = modifierLocal.current
                    })
                }
            }
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo("Outer")

            // add the inner modifier
            useInnerModifier = true
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo("Inner")
        }
    }

    @Test
    fun attachedProducerStartsProducingFromDefault() {
        var useProvider by mutableStateOf(false)
        var consumerValue by mutableStateOf("")
        rule.setContent {
            val modifier = if (useProvider) {
                ProviderModifier(modifierLocal) { "Provided Value" }
            } else {
                Modifier
            }
            Box(modifier) {
                Box(ProviderConsumerModifier(modifierLocal, { "" }) {
                    consumerValue = modifierLocal.current
                })
            }
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo(defaultValue)

            // add the inner modifier
            useProvider = true
        }

        rule.runOnIdle {
            assertThat(consumerValue).isEqualTo("Provided Value")
        }
    }

    @Test
    fun changeModifierLocals() {
        var providedValue by mutableStateOf("Value 1")
        val consumerValue = mutableStateOf("")

        rule.setContent {
            val value = providedValue
            Box(ProviderModifier(modifierLocal) { value }) {
                ConsumeLocal(modifierLocal, consumerValue)
            }
        }

        rule.runOnIdle {
            assertThat(consumerValue.value).isEqualTo("Value 1")
            providedValue = "Value 2"
        }

        rule.runOnIdle {
            assertThat(consumerValue.value).isEqualTo("Value 2")
        }
    }
}

/**
 * This is extracted out so that it can be skipped.
 */
@Composable
fun ConsumeLocal(
    modifierLocal: ProvidableModifierLocal<String>,
    consumerValue: MutableState<String>
) {
    Box(
        ProviderConsumerModifier(modifierLocal, { "" }) {
            consumerValue.value = modifierLocal.current
        }
    )
}

class ProviderConsumerModifier<T>(
    override val key: ProvidableModifierLocal<T>,
    value: () -> T,
    private val consumer: ModifierLocalReadScope.() -> Unit
) : ModifierLocalConsumer, ModifierLocalProvider<T> {
    override val value by derivedStateOf(value)
    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        consumer(scope)
    }
}

class ProviderModifier<T>(
    override val key: ProvidableModifierLocal<T>,
    value: () -> T,
) : ModifierLocalProvider<T> {
    override val value by derivedStateOf(value)
}
