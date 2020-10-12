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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Opens a dialog with the given content.
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the Dialog, the implementation of [onDismissRequest] should
 * contain a way to remove to remove the dialog from the composition hierarchy.
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.DialogSample
 *
 * @param onDismissRequest Executes when the user tries to dismiss the Dialog.
 * @param properties Typically platform specific properties to further configure the dialog.
 * @param content The content to be displayed inside the dialog.
 */
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties? = null,
    content: @Composable () -> Unit
) = ActualDialog(onDismissRequest, properties, content)

/**
 * Common interface for dialog properties. These are typically platform specific options to further
 * configure a dialog. For android ones use AndroidDialogProperties.
 */
@Immutable
interface DialogProperties

@Composable
internal expect fun ActualDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties? = null,
    content: @Composable () -> Unit
)
