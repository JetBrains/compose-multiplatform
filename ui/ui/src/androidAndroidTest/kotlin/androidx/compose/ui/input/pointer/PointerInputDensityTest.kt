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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PointerInputDensityTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun compositionLocalDensityChangeRestartsPointerInputOverload1() {
        compositionLocalDensityChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, block = it)
        }
    }

    @Test
    fun compositionLocalDensityChangeRestartsPointerInputOverload2() {
        compositionLocalDensityChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, Unit, block = it)
        }
    }

    @Test
    fun compositionLocalDensityChangeRestartsPointerInputOverload3() {
        compositionLocalDensityChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, Unit, Unit, block = it)
        }
    }

    private fun compositionLocalDensityChangeRestartsPointerInput(
        pointerInput: (block: suspend PointerInputScope.() -> Unit) -> Modifier
    ) {
        var density by mutableStateOf(5f)

        val pointerInputDensities = mutableListOf<Float>()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                Box(pointerInput {
                    pointerInputDensities.add(density)
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                })
            }
        }

        rule.runOnIdle {
            assertThat(pointerInputDensities.size).isEqualTo(1)
            assertThat(pointerInputDensities.last()).isEqualTo(5f)
            density = 9f
        }

        rule.runOnIdle {
            assertThat(pointerInputDensities.size).isEqualTo(2)
            assertThat(pointerInputDensities.last()).isEqualTo(9f)
        }
    }
}
