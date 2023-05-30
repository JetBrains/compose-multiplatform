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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.atomicfu.atomic
import org.junit.Rule


/**
 * Basic tests of the testing framework itself.
 */
@OptIn(ExperimentalTestApi::class)
class BasicTestTest {

    @get:Rule
    val rule = createComposeRule()

    // See https://github.com/JetBrains/compose-multiplatform/issues/3117
    @Test
    fun recompositionCompletesBeforeSetContentReturns() = repeat(1000) {
        runSkikoComposeUiTest {
            var globalValue by atomic(0)
            setContent {
                var localValue by remember{ mutableStateOf(0) }

                remember(localValue) {
                    globalValue = localValue
                }

                Layout(
                    {},
                    Modifier,
                    measurePolicy = { _, constraints ->
                        localValue = 100
                        layout(constraints.maxWidth, constraints.maxHeight) {}
                    }
                )
            }

            assertEquals(100, globalValue)
        }
    }

    @Test
    fun inputEventAdvancesClock() {
        rule.setContent {
            Box(Modifier.testTag("box"))
        }

        val clockBefore = rule.mainClock.currentTime
        rule.onNodeWithTag("box").performClick()
        val clockAfter = rule.mainClock.currentTime
        assertTrue(clockAfter > clockBefore, "performClick did not advance the test clock")
    }
}