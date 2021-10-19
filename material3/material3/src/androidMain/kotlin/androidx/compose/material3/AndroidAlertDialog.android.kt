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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * ![Basic dialog image](https://developer.android.com/images/reference/androidx/compose/material3/basic-dialog.png)
 *
 * Material Design basic dialog.
 *
 * Dialogs interrupt users with urgent information, details, or actions.
 *
 * The dialog will position its buttons, typically [TextButton]s, based on the available space.
 * By default it will try to place them horizontally next to each other and fallback to horizontal
 * placement if not enough space is available.
 *
 * Simple usage:
 * @sample androidx.compose.material3.samples.AlertDialogSample
 *
 * Usage with a "Hero" icon:
 * @sample androidx.compose.material3.samples.AlertDialogWithIconSample
 *
 * @param onDismissRequest Executes when the user tries to dismiss the Dialog by clicking outside
 * or pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton A button which is meant to confirm a proposed action, thus resolving
 * what triggered the dialog. The dialog does not set up any events for this button so they need
 * to be set up by the caller.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param dismissButton A button which is meant to dismiss the dialog. The dialog does not set up
 * any events for this button so they need to be set up by the caller.
 * @param icon An optional icon that will appear above the [title] or above the [text], in case a
 * title was not provided.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text].
 * @param text The text which presents the details regarding the Dialog's purpose.
 * @param shape Defines the Dialog's shape
 * @param containerColor The container color of the dialog.
 * @param tonalElevation When [containerColor] is [ColorScheme.surface], a higher tonal elevation
 * value will result in a darker dialog color in light theme and lighter color in dark theme.
 * See also [Surface].
 * @param iconContentColor The content color used for the icon.
 * @param titleContentColor The content color used for the title.
 * @param textContentColor The content color used for the text.
 * @param properties Typically platform specific properties to further configure the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = androidx.compose.material3.tokens.Dialog.ContainerShape,
    containerColor: Color =
        MaterialTheme.colorScheme.fromToken(
            androidx.compose.material3.tokens.Dialog.ContainerColor
        ),
    tonalElevation: Dp = androidx.compose.material3.tokens.Dialog.ContainerElevation,
    iconContentColor: Color = MaterialTheme.colorScheme.fromToken(
        androidx.compose.material3.tokens.Dialog.WithIconIconColor
    ),
    titleContentColor: Color = MaterialTheme.colorScheme.fromToken(
        androidx.compose.material3.tokens.Dialog.SubheadColor
    ),
    textContentColor: Color = MaterialTheme.colorScheme.fromToken(
        androidx.compose.material3.tokens.Dialog.SupportingTextColor
    ),
    properties: DialogProperties = DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        AlertDialogContent(
            buttons = {
                AlertDialogFlowRow(
                    mainAxisSpacing = ButtonsMainAxisSpacing,
                    crossAxisSpacing = ButtonsCrossAxisSpacing
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            },
            modifier = modifier,
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            tonalElevation = tonalElevation,
            // Note that a button content color is provided here from the dialog's token, but in
            // most cases, TextButtons should be used for dismiss and confirm buttons.
            // TextButtons will not consume this provided content color value, and will used their
            // own defined or default colors.
            buttonContentColor = MaterialTheme.colorScheme.fromToken(
                androidx.compose.material3.tokens.Dialog.ActionLabelTextColor
            ),
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
        )
    }
}

private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 12.dp
