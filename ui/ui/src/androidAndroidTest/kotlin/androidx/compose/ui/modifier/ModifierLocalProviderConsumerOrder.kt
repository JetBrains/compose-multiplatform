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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierLocalProviderConsumerOrder {

    @get:Rule
    val rule = createComposeRule()

    private val defaultValue = "Default Value"
    private val key = modifierLocalOf { defaultValue }

    @Test
    fun modifierDoesNotConsumeTheValueItProduced() {
        // Arrange.
        val providedValue = "Provided Value"
        lateinit var consumedValue: String
        rule.setContent {
            Box(
                ProviderConsumerModifier(key, { providedValue }) {
                    consumedValue = key.current
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
            Box(ProviderConsumerModifier(key, { parentProvidedValue }) {}) {
                Box(
                    ProviderConsumerModifier(key, { providedValue }) {
                        consumedValue = key.current
                    }
                )
            }
        }

        // Assert.
        assertThat(consumedValue).isEqualTo(parentProvidedValue)
    }
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
