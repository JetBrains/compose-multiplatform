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

import android.app.Dialog
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionReference
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionReference
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner

/**
 * Android specific properties to configure a dialog.
 *
 * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the dialog's
 * window.
 */
@Immutable
data class AndroidDialogProperties(
    val securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit
) : DialogProperties

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
internal actual fun ActualDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties?,
    content: @Composable () -> Unit
) {
    val view = ViewAmbient.current

    val dialog = remember(view) { DialogWrapper(view) }
    dialog.onCloseRequest = onDismissRequest
    remember(properties) { dialog.setProperties(properties) }

    onActive {
        dialog.show()

        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    val composition = compositionReference()
    onCommit {
        dialog.setContent(composition) {
            // TODO(b/159900354): draw a scrim and add margins around the Compose Dialog, and
            //  consume clicks so they can't pass through to the underlying UI
            DialogLayout(
                Modifier.semantics { dialog() },
                content
            )
        }
    }
}

/**
 * Provides the underlying window of a dialog.
 *
 * Implemented by dialog's root layout.
 */
interface DialogWindowProvider {
    val window: Window
}

private class DialogLayout(
    context: Context,
    override val window: Window
) : FrameLayout(context), DialogWindowProvider

private class DialogWrapper(
    private val composeView: View
) : Dialog(composeView.context) {
    lateinit var onCloseRequest: () -> Unit

    private val dialogLayout: DialogLayout
    private var composition: Composition? = null

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        dialogLayout = DialogLayout(context, window)
        setContentView(dialogLayout)
        ViewTreeLifecycleOwner.set(dialogLayout, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(dialogLayout, ViewTreeViewModelStoreOwner.get(composeView))
        ViewTreeSavedStateRegistryOwner.set(
            dialogLayout,
            ViewTreeSavedStateRegistryOwner.get(composeView)
        )
    }

    // TODO(b/159900354): Make the Android Dialog full screen and the scrim fully transparent

    fun setContent(parentComposition: CompositionReference, children: @Composable () -> Unit) {
        composition = dialogLayout.setContent(parentComposition, children)
    }

    private fun setSecureFlagEnabled(secureFlagEnabled: Boolean) {
        window!!.setFlags(
            if (secureFlagEnabled) {
                WindowManager.LayoutParams.FLAG_SECURE
            } else {
                WindowManager.LayoutParams.FLAG_SECURE.inv()
            },
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun setProperties(properties: DialogProperties?) {
        if (properties != null && properties is AndroidDialogProperties) {
            setSecureFlagEnabled(
                properties.securePolicy
                    .shouldApplySecureFlag(composeView.isFlagSecureEnabled())
            )
        } else {
            setSecureFlagEnabled(composeView.isFlagSecureEnabled())
        }
    }

    fun disposeComposition() {
        composition?.dispose()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result) {
            onCloseRequest()
        }

        return result
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }

    override fun onBackPressed() {
        onCloseRequest()
    }
}

@Composable
private fun DialogLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        children = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.fastMap { it.measure(constraints) }
        val width = placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth
        val height = placeables.fastMaxBy { it.height }?.height ?: constraints.minHeight
        layout(width, height) {
            placeables.fastForEach { it.placeRelative(0, 0) }
        }
    }
}