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

package androidx.compose.material.studies.rally

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
fun RallyAlertDialog(
    onDismiss: () -> Unit,
    bodyText: String,
    buttonText: String
) {
    RallyDialogThemeOverlay {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(bodyText) },
            buttons = {
                Column {
                    Divider(
                        Modifier.padding(12.dp, 0.dp, 12.dp, 0.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                    )
                    TextButton(
                        onClick = onDismiss,
                        shape = RectangleShape,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(buttonText)
                    }
                }
            }
        )
    }
}
