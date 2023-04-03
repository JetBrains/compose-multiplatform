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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.rememberDialogState
import java.awt.event.KeyEvent
import androidx.compose.ui.window.Dialog as CoreDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

/**
 * The default padding for an [AlertDialog].
 */
private val AlertDialogPadding = PaddingValues(24.dp)

/**
 * Alert dialog is a Dialog which interrupts the user with urgent information, details or actions.
 *
 * The dialog will position its buttons based on the available space. By default it will try to
 * place them horizontally next to each other and fallback to horizontal placement if not enough
 * space is available. There is also another version of this composable that has a slot for buttons
 * to provide custom buttons layout.
 *
 * Sample of dialog:
 * @sample androidx.compose.material.samples.AlertDialogSample
 *
 * @param onDismissRequest Callback that will be called when the user closes the dialog.
 * @param confirmButton A button which is meant to confirm a proposed action, thus resolving
 * what triggered the dialog. The dialog does not set up any events for this button so they need
 * to be set up by the caller.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param dismissButton A button which is meant to dismiss the dialog. The dialog does not set up
 * any events for this button so they need to be set up by the caller.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.subtitle1].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body2].
 * @param shape Defines the Dialog's shape
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param dialogProvider Defines how to create dialog in which will be placed AlertDialog's content.
 */
@Composable
@ExperimentalMaterialApi
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
    dialogProvider: AlertDialogProvider = PopupAlertDialogProvider
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        dialogPadding = AlertDialogPadding,
        dialogProvider = dialogProvider
    )
}

/**
 * Alert dialog is a Dialog which interrupts the user with urgent information, details or actions.
 *
 * The dialog will position its buttons based on the available space. By default it will try to
 * place them horizontally next to each other and fallback to horizontal placement if not enough
 * space is available. There is also another version of this composable that has a slot for buttons
 * to provide custom buttons layout.
 *
 * Sample of dialog:
 * @sample androidx.compose.material.samples.AlertDialogSample
 *
 * @param onDismissRequest Callback that will be called when the user closes the dialog.
 * @param confirmButton A button which is meant to confirm a proposed action, thus resolving
 * what triggered the dialog. The dialog does not set up any events for this button so they need
 * to be set up by the caller.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param dismissButton A button which is meant to dismiss the dialog. The dialog does not set up
 * any events for this button so they need to be set up by the caller.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.subtitle1].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body2].
 * @param shape Defines the Dialog's shape
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param dialogPadding The outer padding of the dialog.
 * @param dialogProvider Defines how to create dialog in which will be placed AlertDialog's content.
 */
@Composable
@ExperimentalMaterialApi
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
    dialogPadding: PaddingValues = AlertDialogPadding,
    dialogProvider: AlertDialogProvider = PopupAlertDialogProvider,
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
        dialogPadding = dialogPadding,
        dialogProvider = dialogProvider
    )
}

/**
 * Alert dialog is a Dialog which interrupts the user with urgent information, details or actions.
 *
 * This function can be used to fully customize the button area, e.g. with:
 *
 * @sample androidx.compose.material.samples.CustomAlertDialogSample
 *
 * @param onDismissRequest Callback that will be called when the user closes the dialog.
 * @param buttons Function that emits the layout with the buttons.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.subtitle1].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body2].
 * @param shape Defines the Dialog's shape.
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param dialogProvider Defines how to create dialog in which will be placed AlertDialog's content.
 */
@Composable
@ExperimentalMaterialApi
fun AlertDialog(
    onDismissRequest: () -> Unit,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    dialogProvider: AlertDialogProvider = PopupAlertDialogProvider
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        buttons = buttons,
        modifier = modifier,
        title = title,
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        dialogPadding = AlertDialogPadding,
        dialogProvider = dialogProvider
    )
}

/**
 * Alert dialog is a Dialog which interrupts the user with urgent information, details or actions.
 *
 * This function can be used to fully customize the button area, e.g. with:
 *
 * @sample androidx.compose.material.samples.CustomAlertDialogSample
 *
 * @param onDismissRequest Callback that will be called when the user closes the dialog.
 * @param buttons Function that emits the layout with the buttons.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.subtitle1].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body2].
 * @param shape Defines the Dialog's shape.
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param dialogPadding The outer padding of the dialog.
 * @param dialogProvider Defines how to create dialog in which will be placed AlertDialog's content.
 */
@Composable
@ExperimentalMaterialApi
fun AlertDialog(
    onDismissRequest: () -> Unit,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    dialogPadding: PaddingValues = AlertDialogPadding,
    dialogProvider: AlertDialogProvider = PopupAlertDialogProvider
) {
    with(dialogProvider) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = shape,
            modifier = Modifier.padding(dialogPadding).then(modifier),
        ) { modifier ->
            AlertDialogContent(
                buttons = buttons,
                modifier = modifier.width(IntrinsicSize.Max),
                title = title,
                text = text,
                shape = shape,
                backgroundColor = backgroundColor,
                contentColor = contentColor
            )
        }
    }
}

/**
 * Defines how to create dialog in which will be placed AlertDialog's content.
 */
@ExperimentalMaterialApi
interface AlertDialogProvider {
    /**
     * Dialog which will be used to place AlertDialog's [content].
     *
     * @param onDismissRequest Callback that will be called when the user closes the dialog
     * @param content Content of the dialog
     */
    @Deprecated("Will be removed in 1.5; use the other overload")
    @Composable
    fun AlertDialog(
        onDismissRequest: () -> Unit,
        content: @Composable () -> Unit
    )

    /**
     * Dialog which will be used to place AlertDialog's [content].
     *
     * @param onDismissRequest Callback that will be called when the user closes the dialog
     * @param shape The Dialog's shape
     * @param content Content of the dialog
     */
    @Suppress("DEPRECATION")
    @Composable
    fun AlertDialog(
        onDismissRequest: () -> Unit,
        shape: Shape,
        modifier: Modifier,
        content: @Composable (Modifier) -> Unit
    ) = AlertDialog(
        onDismissRequest = onDismissRequest,
        content = { content(modifier) }
    )
}

// TODO(https://github.com/JetBrains/compose-jb/issues/933): is it right to use Popup to show a
//  dialog?
/**
 * Shows Alert dialog as popup in the middle of the window.
 */
@ExperimentalMaterialApi
object PopupAlertDialogProvider : AlertDialogProvider {

    @Deprecated("Will be removed in 1.5; use the other overload")
    @Composable
    override fun AlertDialog(
        onDismissRequest: () -> Unit,
        content: @Composable () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = RectangleShape,
            modifier = Modifier,
            content = { content() }
        )
    }

    @Composable
    override fun AlertDialog(
        onDismissRequest: () -> Unit,
        shape: Shape,
        modifier: Modifier,
        content: @Composable (Modifier) -> Unit
    ) {
        // Popups on the desktop are by default embedded in the component in which
        // they are defined and aligned within its bounds. But an [AlertDialog] needs
        // to be aligned within the window, not the parent component, so we cannot use
        // [alignment] property of [Popup] and have to use [Box] that fills all the
        // available space. Also [Box] provides a dismiss request feature when clicked
        // outside of the [AlertDialog] content.
        Popup(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = IntOffset.Zero
            },
            focusable = true,
            onDismissRequest = onDismissRequest,
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown && it.awtEventOrNull?.keyCode == KeyEvent.VK_ESCAPE) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            },
        ) {
            val scrimColor = Color.Black.copy(alpha = 0.32f) //todo configure scrim color in function arguments
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .pointerInput(onDismissRequest) {
                        detectTapGestures(onPress = { onDismissRequest() })
                    },
                contentAlignment = Alignment.Center
            ) {
                content(
                    modifier
                        .shadow(elevation = 24.dp, shape = shape)
                        .pointerInput(onDismissRequest) {
                            detectTapGestures(onPress = {
                                // Workaround to disable clicks on Surface background
                                // https://github.com/JetBrains/compose-jb/issues/2581
                            })
                        }
                )
            }
        }
    }
}

/**
 * Shows Alert dialog as undecorated draggable window.
 */
@ExperimentalMaterialApi
object UndecoratedWindowAlertDialogProvider : AlertDialogProvider {
    @Deprecated("Will be removed in 1.5; use the overload that takes the dialog shape")
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun AlertDialog(
        onDismissRequest: () -> Unit,
        content: @Composable () -> Unit
    ) {
        CoreDialog(
            onCloseRequest = onDismissRequest,
            state = rememberDialogState(width = Dp.Unspecified, height = Dp.Unspecified),
            undecorated = true,
            transparent = true,
            resizable = false,
            onKeyEvent = {
                if (it.key == Key.Escape) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            },
        ) {
            WindowDraggableArea {
                content()
            }
        }
    }
}
