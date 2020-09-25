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

import android.os.Build
import androidx.compose.foundation.AmbientContentColor
import androidx.compose.foundation.Text
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertContainsColor
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.isDialog
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // Should be O: b/163023027
class AlertDialogTest {

    @get:Rule
    val rule = createComposeRule()

    @FlakyTest(bugId = 170333139)
    @Test
    fun customStyleProperties_shouldApply() {
        var contentColor = Color.Unspecified
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                modifier = Modifier.border(10.dp, Color.Blue),
                text = {
                    contentColor = AmbientContentColor.current
                    Text("Text")
                },
                confirmButton = {},
                backgroundColor = Color.Yellow,
                contentColor = Color.Red
            )
        }

        // Assert background
        rule.onNode(isDialog())
            .captureToBitmap()
            .assertContainsColor(Color.Yellow) // Background
            .assertContainsColor(Color.Blue) // Modifier border

        // Assert content color
        rule.runOnIdle {
            // Reset opacity as that is changed by the emphasis
            assertThat(contentColor.copy(alpha = 1f)).isEqualTo(Color.Red)
        }
    }
}
