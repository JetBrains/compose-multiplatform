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

package androidx.compose.ui.test.gesturescope

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.center
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class LocalToRootTest {
    @get:Rule
    val rule = createComposeRule()

    private val recorder = SinglePointerInputRecorder()

    @Test
    fun test() {
        rule.setContent {
            with(LocalDensity.current) {
                Column(
                    Modifier.requiredSize(100.toDp())
                        .testTag("viewport")
                        .verticalScroll(rememberScrollState())
                        .padding(top = 20.toDp())
                ) {
                    ClickableTestBox(recorder, width = 100f, height = 200f)
                }
            }
        }

        rule.onNodeWithTag("viewport").performGesture { click(center) }

        val expectedClickLocation = Offset(50f, 30f)
        recorder.events.forEach {
            assertThat(it.position).isEqualTo(expectedClickLocation)
        }
    }
}
