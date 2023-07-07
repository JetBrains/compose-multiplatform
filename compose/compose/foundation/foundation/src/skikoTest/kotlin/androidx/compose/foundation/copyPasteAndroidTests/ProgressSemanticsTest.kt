/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ProgressSemanticsTest {

    @Test
    fun determinateProgress_testSemantics() = runSkikoComposeUiTest {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        setContent {
            Box(
                Modifier
                    .testTag(tag)
                    .progressSemantics(progress.value)
                    .size(50.dp)
                    .background(color = Color.Cyan)
            )
        }

        onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        runOnIdle {
            progress.value = 0.005f
        }

        onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.005f, 0f..1f))

        runOnIdle {
            progress.value = 0.5f
        }

        onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun indeterminateProgress_testSemantics() = runSkikoComposeUiTest {
        val tag = "linear"

        setContent {
            Box(
                Modifier
                    .testTag(tag)
                    .progressSemantics()
                    .size(50.dp)
                    .background(color = Color.Cyan)
            )
        }

        onNodeWithTag(tag)
            .assert(
                SemanticsMatcher("progress is ProgressBarRangeInfo.Indeterminate") {
                    val progress = it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
                    progress === ProgressBarRangeInfo.Indeterminate
                }
            )
    }
}
