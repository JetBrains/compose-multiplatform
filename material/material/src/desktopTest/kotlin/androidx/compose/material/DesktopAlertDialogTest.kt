/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.runners.JUnit4
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Test

@RunWith(JUnit4::class)
class DesktopAlertDialogTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun alignedToCenter_inPureWindow() {
        var rootSize = IntSize(1024, 768) // default value
        var dialogSize = IntSize(150, 150)
        var location = Offset.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                @OptIn(ExperimentalMaterialApi::class)
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("AlerDialog") },
                    text = { Text("Apply?") },
                    confirmButton = { Button(onClick = {}) { Text("Apply") } },
                    modifier = Modifier.size(dialogSize.width.dp, dialogSize.height.dp)
                        .onGloballyPositioned { location = it.positionInRoot() }
                )
            }
        }
        rule.runOnIdle {
           assertThat(location).isEqualTo(calculateCenterPosition(rootSize, dialogSize))
        }
    }

    private fun calculateCenterPosition(rootSize: IntSize, childSize: IntSize): Offset {
        val x = (rootSize.width - childSize.width) / 2f
        val y = (rootSize.height - childSize.height) / 2f
        return Offset(x, y)
    }
}