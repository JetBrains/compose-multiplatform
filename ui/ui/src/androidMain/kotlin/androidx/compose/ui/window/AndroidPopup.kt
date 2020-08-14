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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.compositionReference
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.ViewAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntBounds
import androidx.compose.ui.unit.round
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import org.jetbrains.annotations.TestOnly

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the popup tries to dismiss itself. This happens when
 * the popup is focusable and the user clicks outside.
 * @param children The content to be displayed inside the popup.
 */
@Composable
internal actual fun ActualPopup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    children: @Composable () -> Unit
) {
    val view = ViewAmbient.current
    val providedTestTag = PopupTestTagAmbient.current

    val popupPositionProperties = remember { PopupPositionProperties() }
    val popupLayout = remember {
        PopupLayout(
            composeView = view,
            onDismissRequest = onDismissRequest,
            testTag = providedTestTag
        )
    }

    // Refresh anything that might have changed
    popupLayout.testTag = providedTestTag
    remember(isFocusable) { popupLayout.updateLayoutParams(isFocusable) }

    var composition: Composition? = null

    // TODO(soboleva): Look at module arrangement so that Box can be
    // used instead of this custom Layout
    // Get the parent's global position, size and layout direction
    Layout(children = emptyContent(), modifier = Modifier.onPositioned { childCoordinates ->
        val coordinates = childCoordinates.parentCoordinates!!
        // Get the global position of the parent
        val layoutPosition = coordinates.localToGlobal(Offset.Zero).round()
        val layoutSize = coordinates.size

        popupPositionProperties.parentGlobalBounds = IntBounds(layoutPosition, layoutSize)

        // Update the popup's position
        popupLayout.updatePosition(popupPositionProvider, popupPositionProperties)
    }) { _, _ ->
        popupPositionProperties.parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }

    // TODO(lmr): refactor these APIs so that recomposer isn't necessary
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    val parentComposition = compositionReference()
    onCommit {
        composition = popupLayout.setContent(recomposer, parentComposition) {
            SimpleStack(Modifier.semantics { this.popup() }.onPositioned {
                // Get the size of the content
                popupPositionProperties.popupContentSize = it.size

                // Update the popup's position
                popupLayout.updatePosition(popupPositionProvider, popupPositionProperties)
            }, children = children)
        }
    }

    onDispose {
        composition?.dispose()
        // Remove the window
        popupLayout.dismiss()
    }
}

// TODO(soboleva): Look at module dependencies so that we can get code reuse between
// Popup's SimpleStack and Stack.
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun SimpleStack(modifier: Modifier, noinline children: @Composable () -> Unit) {
    Layout(children = children, modifier = modifier) { measurables, constraints ->
        when (measurables.size) {
            0 -> layout(0, 0) {}
            1 -> {
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.placeRelative(0, 0)
                }
            }
            else -> {
                val placeables = measurables.map { it.measure(constraints) }
                var width = 0
                var height = 0
                for (i in 0..placeables.lastIndex) {
                    val p = placeables[i]
                    width = maxOf(width, p.width)
                    height = maxOf(height, p.height)
                }
                layout(width, height) {
                    for (i in 0..placeables.lastIndex) {
                        val p = placeables[i]
                        p.placeRelative(0, 0)
                    }
                }
            }
        }
    }
}

/**
 * The layout the popup uses to display its content.
 *
 * @param composeView The parent view of the popup which is the AndroidComposeView.
 * @param onDismissRequest Executed when the popup tries to dismiss itself.
 * @param testTag The test tag used to match the popup in tests.
 */
@SuppressLint("ViewConstructor")
private class PopupLayout(
    private val composeView: View,
    private val onDismissRequest: (() -> Unit)? = null,
    var testTag: String
) : FrameLayout(composeView.context) {
    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params = createLayoutParams()
    private var viewAdded: Boolean = false

    init {
        id = android.R.id.content
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        ViewTreeSavedStateRegistryOwner.set(this, ViewTreeSavedStateRegistryOwner.get(composeView))
    }

    private fun Rect.toIntBounds() = IntBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )

    /**
     * Shows the popup at a position given by the method which calculates the coordinates
     * relative to its parent.
     *
     * @param positionProvider The logic of positioning the popup relative to its parent.
     * @param positionProperties Properties to use to position the popup.
     */
    fun updatePosition(
        positionProvider: PopupPositionProvider,
        positionProperties: PopupPositionProperties
    ) {
        val windowGlobalBounds = Rect().let {
            composeView.rootView.getWindowVisibleDisplayFrame(it)
            it.toIntBounds()
        }

        val popupGlobalPosition = positionProvider.calculatePosition(
            positionProperties.parentGlobalBounds,
            windowGlobalBounds,
            positionProperties.parentLayoutDirection,
            positionProperties.popupContentSize
        )

        // WindowManager treats the given coordinates as relative to our window, not relative to the
        // screen. Which means that we need to translate them. Other option would be to only work
        // with window relative coordinates but our layout APIs don't provide this value so it
        // could be confusing for the implementors of position provider.
        val rootViewLocation = IntArray(2)
        composeView.rootView.getLocationOnScreen(rootViewLocation)
        params.x = popupGlobalPosition.x - rootViewLocation[0]
        params.y = popupGlobalPosition.y - rootViewLocation[1]

        if (!viewAdded) {
            windowManager.addView(this, params)
            viewAdded = true
        } else {
            windowManager.updateViewLayout(this, params)
        }
    }

    /**
     * Update the LayoutParams.
     *
     * @param popupIsFocusable Indicates if the popup can grab the focus.
     */
    fun updateLayoutParams(popupIsFocusable: Boolean) {
        params.flags = if (!popupIsFocusable) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            params.flags and (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
        }

        if (viewAdded) {
            windowManager.updateViewLayout(this, params)
        }
    }

    /**
     * Remove the view from the [WindowManager].
     */
    fun dismiss() {
        ViewTreeLifecycleOwner.set(this, null)
        windowManager.removeViewImmediate(this)
    }

    /**
     * Handles touch screen motion events and calls [onDismissRequest] when the
     * users clicks outside the popup.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if ((event?.action == MotionEvent.ACTION_DOWN) &&
            ((event.x < 0) || (event.x >= width) || (event.y < 0) || (event.y >= height))
        ) {
            onDismissRequest?.invoke()
            return true
        } else if (event?.action == MotionEvent.ACTION_OUTSIDE) {
            onDismissRequest?.invoke()
            return true
        }

        return super.onTouchEvent(event)
    }

    /**
     * Initialize the LayoutParams specific to [android.widget.PopupWindow].
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            // Start to position the popup in the top left corner, a new position will be calculated
            gravity = Gravity.START or Gravity.TOP

            // Flags specific to android.widget.PopupWindow
            flags = flags and (WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH).inv()

            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Wrap the frame layout which contains composable content
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            format = PixelFormat.TRANSLUCENT
        }
    }
}

/**
 * Returns whether the given view is an underlying decor view of a popup. If the given testTag is
 * supplied it also verifies that the popup has such tag assigned.
 *
 * @param view View to verify.
 * @param testTag If provided, tests that the given tag in defined on the popup.
 */
// TODO(b/139861182): Move this functionality to ComposeTestRule
@TestOnly
fun isPopupLayout(view: View, testTag: String? = null): Boolean =
    view is PopupLayout && (testTag == null || testTag == view.testTag)