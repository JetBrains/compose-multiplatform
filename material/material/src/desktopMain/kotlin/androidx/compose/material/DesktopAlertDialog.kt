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

package androidx.compose.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Alert dialog is a [Dialog] which interrupts the user with urgent information, details or actions.
 *
 * The dialog will position its buttons based on the available space. By default it will try to
 * place them horizontally next to each other and fallback to horizontal placement if not enough
 * space is available. There is also another version of this composable that has a slot for buttons
 * to provide custom buttons layout.
 *
 * Sample of dialog:
 * @sample androidx.compose.material.samples.AlertDialogSample
 *
 * @param onDismissRequest Executes when the user tries to dismiss the Dialog by clicking outside
 * or pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton A button which is meant to confirm a proposed action, thus resolving
 * what triggered the dialog. The dialog does not set up any events for this button so they need
 * to be set up by the caller.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param dismissButton A button which is meant to dismiss the dialog. The dialog does not set up
 * any events for this button so they need to be set up by the caller.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.h6].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body1].
 * @param shape Defines the Dialog's shape
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param properties Typically platform specific properties to further configure the dialog.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        buttons = {
            // TODO: move the modifiers to FlowRow when it supports a modifier parameter
            Box(Modifier.fillMaxWidth().padding(all = 8.dp)) {
                AlertDialogFlowRow(
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 12.dp
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        },
        modifier = modifier,
        title = title,
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    )
}

/**
 * Alert dialog is a [Dialog] which interrupts the user with urgent information, details or actions.
 *
 * This function can be used to fully customize the button area, e.g. with:
 *
 * @sample androidx.compose.material.samples.CustomAlertDialogSample
 *
 * @param onDismissRequest Executes when the user tries to dismiss the Dialog by clicking outside
 * or pressing the back button. This is not called when the dismiss button is clicked.
 * @param buttons Function that emits the layout with the buttons.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.h6].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body1].
 * @param shape Defines the Dialog's shape.
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param properties Typically platform specific properties to further configure the dialog.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        AlertDialogContent(
            buttons = buttons,
            modifier = modifier,
            title = title,
            text = text,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }
}
