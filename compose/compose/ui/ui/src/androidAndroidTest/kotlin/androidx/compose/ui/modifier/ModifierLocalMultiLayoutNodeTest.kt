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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierLocalMultiLayoutNodeTest {
    @get:Rule
    val rule = createComposeRule()

    private val defaultValue = "Default Value"

    @Test
    fun doesNotReadValuesProvidedByChildren() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedValue = "Provided Value"
        lateinit var readValue: String
        rule.setContent {
            Box(Modifier.modifierLocalConsumer { readValue = localString.current }) {
                Box(Modifier.modifierLocalProvider(localString) { providedValue })
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(readValue).isEqualTo(defaultValue) }
    }

    @Test
    fun readsValuesProvidedByParent() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedValue = "Provided Value"
        lateinit var readValue: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedValue }) {
                Box(Modifier.modifierLocalConsumer { readValue = localString.current })
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(readValue).isEqualTo(providedValue) }
    }

    @Test
    fun readsValuesProvidedByGrandParent() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedValue = "Provided Value"
        lateinit var readValue: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedValue }) {
                Box(Modifier.size(100.dp)) {
                    Box(Modifier.modifierLocalConsumer { readValue = localString.current })
                }
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(readValue).isEqualTo(providedValue) }
    }

    @Test
    fun parentInSameLayoutNodeTakesPrecedenceOverParentLayoutNode() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedValue1 = "Provided Value 1"
        val providedValue2 = "Provided Value 2"
        lateinit var readValue: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedValue1 }) {
                Box(
                    Modifier
                        .modifierLocalProvider(localString) { providedValue2 }
                        .modifierLocalConsumer { readValue = localString.current }
                )
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(readValue).isEqualTo(providedValue2) }
    }

    @Test
    fun readsValuesProvidedByNearestParent() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedValue1 = "Provided Value 1"
        val providedValue2 = "Provided Value 2"
        lateinit var readValue: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedValue1 }) {
                Box(Modifier.modifierLocalProvider(localString) { providedValue2 }) {
                    Box(Modifier.modifierLocalConsumer { readValue = localString.current })
                }
            }
        }

        // Assert.
        rule.runOnIdle { assertThat(readValue).isEqualTo(providedValue2) }
    }

    @Test
    fun multipleModifierLocalsOfSameDataType() {
        // Arrange.
        val localString1 = modifierLocalOf { defaultValue }
        val localString2 = modifierLocalOf { defaultValue }
        val providedValue1 = "Provided Value 1"
        val providedValue2 = "Provided Value 2"
        lateinit var readValue1: String
        lateinit var readValue2: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString1) { providedValue1 }) {
                Box(Modifier.modifierLocalProvider(localString2) { providedValue2 }) {
                    Box(
                        Modifier.modifierLocalConsumer {
                            readValue1 = localString1.current
                            readValue2 = localString2.current
                        }
                    )
                }
            }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(readValue1).isEqualTo(providedValue1)
            assertThat(readValue2).isEqualTo(providedValue2)
        }
    }

    @Test
    fun multipleModifierLocalsWithDifferentDataType() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val localInteger = modifierLocalOf { Int.MIN_VALUE }
        val providedString = "Provided Value"
        val providedInteger = 100
        lateinit var readString: String
        var readInteger = 0
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedString }) {
                Box(Modifier.modifierLocalProvider(localInteger) { providedInteger }) {
                    Box(
                        Modifier.modifierLocalConsumer {
                            readString = localString.current
                            readInteger = localInteger.current
                        }
                    )
                }
            }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(readString).isEqualTo(providedString)
            assertThat(readInteger).isEqualTo(providedInteger)
        }
    }

    @Test
    fun modifierLocalProviderChanged() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val provider1value = "Provider1"
        val provider2value = "Provider2"
        var useFirstProvider by mutableStateOf(true)
        lateinit var readString: String
        rule.setContent {
            Box(
                Modifier.then(
                    if (useFirstProvider) {
                        Modifier.modifierLocalProvider(localString) { provider1value }
                    } else {
                        Modifier.modifierLocalProvider(localString) { provider2value }
                    }
                )
            ) {
                Box(Modifier.modifierLocalConsumer { readString = localString.current })
            }
        }

        // Act.
        rule.runOnIdle { useFirstProvider = false }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(provider2value) }
    }

    @Test
    fun modifierLocalProviderValueChanged() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val value1 = "Value1"
        val value2 = "Value2"
        var useFirstValue by mutableStateOf(true)
        lateinit var readString: String
        rule.setContent {
            Box(
                Modifier.modifierLocalProvider(localString) {
                    if (useFirstValue) value1 else value2
                }
            ) {
                Box(
                    Modifier.modifierLocalConsumer {
                        readString = localString.current
                    }
                )
            }
        }

        // Act.
        rule.runOnIdle { useFirstValue = false }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(value2) }
    }

    @Test
    fun modifierLocalProviderAdded() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedByParent1 = "Parent1"
        var secondParentAdded by mutableStateOf(false)
        val providedByParent2 = "Parent2"
        lateinit var readString: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedByParent1 }) {
                Box(
                    Modifier.then(
                        if (secondParentAdded) {
                            Modifier.modifierLocalProvider(localString) { providedByParent2 }
                        } else {
                            Modifier
                        }
                    )
                ) {
                    Box(Modifier.modifierLocalConsumer { readString = localString.current })
                }
            }
        }

        // Act.
        rule.runOnIdle { secondParentAdded = true }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(providedByParent2) }
    }

    @Test
    fun modifierLocalProviderRemoved_readsDefaultValue() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedValue = "Parent"
        var providerRemoved by mutableStateOf(false)
        lateinit var readString: String
        rule.setContent {
            Box(
                Modifier.then(
                    if (providerRemoved) {
                        Modifier
                    } else {
                        Modifier.modifierLocalProvider(localString) { providedValue }
                    }
                )

            ) {
                Box(Modifier.modifierLocalConsumer { readString = localString.current })
            }
        }

        // Act.
        rule.runOnIdle { providerRemoved = true }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(defaultValue) }
    }

    @Test
    fun modifierLocalProviderRemoved_readsPreviousParent() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedByParent1 = "Parent1"
        var secondParentRemoved by mutableStateOf(false)
        val providedByParent2 = "Parent2"
        lateinit var readString: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedByParent1 }) {
                Box(
                    Modifier.then(
                        if (secondParentRemoved) {
                            Modifier
                        } else {
                            Modifier.modifierLocalProvider(localString) { providedByParent2 }
                        }
                    )
                ) {
                    Box(Modifier.modifierLocalConsumer { readString = localString.current })
                }
            }
        }

        // Act.
        rule.runOnIdle { secondParentRemoved = true }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(providedByParent1) }
    }

    @Test
    fun modifierLocalProviderMoved_readsDefaultValue() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        var providerMoved by mutableStateOf(false)
        val providedValue = "ProvidedValue"
        val providerModifier = Modifier.modifierLocalProvider(localString) { providedValue }
        lateinit var readString: String
        rule.setContent {
            Box(Modifier.then(if (providerMoved) Modifier else providerModifier)) {
                Box(Modifier.modifierLocalConsumer { readString = localString.current }) {
                    Box(Modifier.then(if (providerMoved) providerModifier else Modifier))
                }
            }
        }

        // Act.
        rule.runOnIdle { providerMoved = true }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(defaultValue) }
    }

    @Test
    fun modifierLocalProviderMoved_readsPreviousParent() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        val providedByParent1 = "Parent1"
        var secondParentMoved by mutableStateOf(false)
        val providedByParent2 = "Parent2"
        val parent2Modifier = Modifier.modifierLocalProvider(localString) { providedByParent2 }
        lateinit var readString: String
        rule.setContent {
            Box(Modifier.modifierLocalProvider(localString) { providedByParent1 }) {
                Box(Modifier.then(if (secondParentMoved) Modifier else parent2Modifier)) {
                    Box(Modifier.modifierLocalConsumer { readString = localString.current }) {
                        Box(Modifier.then(if (secondParentMoved) parent2Modifier else Modifier))
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle { secondParentMoved = true }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(providedByParent1) }
    }

    @Test
    fun modifierLocalProviderMoved_readsSameValue() {
        // Arrange.
        val localString = modifierLocalOf { defaultValue }
        var providerMoved by mutableStateOf(false)
        val providedValue = "ProvidedValue"
        val providerModifier = Modifier.modifierLocalProvider(localString) { providedValue }
        lateinit var readString: String
        rule.setContent {
            Box(Modifier.then(if (providerMoved) Modifier else providerModifier)) {
                Box(Modifier.size(100.dp)) {
                    Box(Modifier.then(if (providerMoved) providerModifier else Modifier)) {
                        Box(Modifier.modifierLocalConsumer { readString = localString.current })
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle { providerMoved = true }

        // Assert.
        rule.runOnIdle { assertThat(readString).isEqualTo(providedValue) }
    }
}