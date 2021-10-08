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

package androidx.compose.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest // Regression test for b/202398857
@RunWith(AndroidJUnit4::class)
class SnapshotFlowTest {

    @get:Rule
    val rule = createComposeRule()

    @OptIn(InternalCoroutinesApi::class)
    @Test
    fun changingValueInLaunchedEffectAndUsingSnapshotFlow() {
        val state = mutableStateOf(0)

        var lastComposedValue: Int? = null

        rule.setContent {
            LaunchedEffect(Unit) {
                state.value = 1
            }

            lastComposedValue = state.value

            LaunchedEffect(state) {
                snapshotFlow { state.value }.collect { }
            }
        }

        rule.runOnIdle {
            assertEquals(1, lastComposedValue)
        }
    }
}