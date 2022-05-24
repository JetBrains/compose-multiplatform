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

package androidx.compose.foundation.demos

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun WindowInsetsDemo() {
    val insets = WindowInsets.safeDrawing
    val density = LocalDensity.current

    Column {
        Text(
            "The numbers around the text field below show the respective WindowInsets values for" +
                " the safeDrawing insets. To use this demo, go the demo app settings (⚙️ icon), " +
                "set the soft input mode to AdjustResize, and disable Decor Fits System Windows." +
                " If you don't configure the settings this way, the insets will not be updated." +
                " Note that IME insets are only supported on API 23 and above."
        )

        BasicTextField(
            value = "Click to show keyboard",
            onValueChange = {},
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(),
            textStyle = TextStyle(color = Color.Black.copy(alpha = 0.5f))
        ) { field ->
            with(density) {
                Column(horizontalAlignment = CenterHorizontally) {
                    Text(
                        insets.getTop(density).toDp().toString(),
                        style = MaterialTheme.typography.caption
                    )
                    Row(verticalAlignment = CenterVertically) {
                        val layoutDirection = LocalLayoutDirection.current
                        Text(
                            insets.getLeft(density, layoutDirection).toDp().toString(),
                            style = MaterialTheme.typography.caption
                        )
                        Box(
                            Modifier
                                .padding(2.dp)
                                .border(1.dp, Color.Black)
                        ) {
                            field()
                        }
                        Text(
                            insets.getRight(density, layoutDirection).toDp().toString(),
                            style = MaterialTheme.typography.caption
                        )
                    }
                    Text(
                        insets.getBottom(density).toDp().toString(),
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}