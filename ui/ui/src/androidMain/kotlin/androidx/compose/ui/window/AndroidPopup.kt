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
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionReference
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionReference
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientView
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntBounds
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import org.jetbrains.annotations.TestOnly

/**
 * Android specific properties to configure a popup.
 *
 * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the popup's
 * window.
 */
@Immutable
data class AndroidPopupProperties(
    val securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit
) : PopupProperties

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties Typically a platform specific properties to further configure the popup.
 * @param content The content to be displayed inside the popup.
 */
@Composable
internal actual fun ActualPopup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties?,
    content: @Composable () -> Unit
) {
    val view = AmbientView.current
    val density = AmbientDensity.current
    val testTag = AmbientPopupTestTag.current
    val parentComposition = compositionReference()
    val currentContent by rememberUpdatedState(content)

    val popupLayout = remember {
        PopupLayout(view, density).apply {
            this.onDismissRequest = onDismissRequest
            this.testTag = testTag
            setPositionProvider(popupPositionProvider)
            setIsFocusable(isFocusable)
            setProperties(properties)
            setContent(parentComposition) {
                SimpleStack(
                    Modifier.semantics { this.popup() }.onGloballyPositioned {
                        // Get the size of the content
                        popupContentSize = it.size
                        updatePosition()
                    }
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(popupLayout) {
        onDispose {
            popupLayout.disposeComposition()
            // Remove the window
            popupLayout.dismiss()
        }
    }

    SideEffect {
        popupLayout.apply {
            this.onDismissRequest = onDismissRequest
            this.testTag = testTag
            setPositionProvider(popupPositionProvider)
            setIsFocusable(isFocusable)
            setProperties(properties)
        }
    }

    // TODO(soboleva): Look at module arrangement so that Box can be
    // used instead of this custom Layout
    // Get the parent's global position, size and layout direction
    Layout(
        content = emptyContent(),
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            // Get the global position of the parent
            @Suppress("DEPRECATION")
            val layoutPosition = coordinates.localToGlobal(Offset.Zero).round()
            val layoutSize = coordinates.size

            popupLayout.parentGlobalBounds = IntBounds(layoutPosition, layoutSize)
            // Update the popup's position
            popupLayout.updatePosition()
        }
    ) { _, _ ->
        popupLayout.parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }
}

// TODO(soboleva): Look at module dependencies so that we can get code reuse between
// Popup's SimpleStack and Box.
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun SimpleStack(modifier: Modifier, noinline content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
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
 */
@SuppressLint("ViewConstructor")
private class PopupLayout(
    private val composeView: View,
    density: Density
) : AbstractComposeView(composeView.context) {
    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params = createLayoutParams()
    private var viewAdded: Boolean = false

    /** Executed when the popup tries to dismiss itself. */
    var onDismissRequest: (() -> Unit)? = null
    /** The test tag used to match the popup in tests. */
    var testTag: String = ""

    /** The logic of positioning the popup relative to its parent. */
    private var positionProvider: PopupPositionProvider? = null

    // Position params
    var parentGlobalBounds = IntBounds(0, 0, 0, 0)
    var popupContentSize = IntSize.Zero
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr

    private val maxSupportedElevation = 30.dp

    init {
        id = android.R.id.content
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        ViewTreeSavedStateRegistryOwner.set(this, ViewTreeSavedStateRegistryOwner.get(composeView))

        // Enable children to draw their shadow by not clipping them
        clipChildren = false
        // Allocate space for elevation
        with(density) { elevation = maxSupportedElevation.toPx() }
        // Simple outline to force window manager to allocate space for shadow.
        // Note that the outline affects clickable area for the dismiss listener. In case of shapes
        // like circle the area for dismiss might be to small (rectangular outline consuming clicks
        // outside of the circle).
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, result: Outline) {
                result.setRect(0, 0, view.width, view.height)
                // We set alpha to 0 to hide the view's shadow and let the composable to draw its
                // own shadow. This still enables us to get the extra space needed in the surface.
                result.alpha = 0f
            }
        }
    }

    private var content: @Composable () -> Unit by mutableStateOf(emptyContent())

    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionReference, content: @Composable () -> Unit) {
        setParentCompositionReference(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }

    fun setPositionProvider(positionProvider: PopupPositionProvider) {
        val wasProviderSetBefore = this.positionProvider != null
        this.positionProvider = positionProvider
        // If we already had a provider before, update our position.
        // Otherwise, the position will be calculated during the first layout.
        if (wasProviderSetBefore) {
            updatePosition()
        }
    }

    /**
     * Set whether the popup can grab a focus and support dismissal.
     */
    fun setIsFocusable(isFocusable: Boolean) = applyNewFlags(
        if (!isFocusable) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            params.flags and (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
        }
    )

    fun setSecureFlagEnabled(secureFlagEnabled: Boolean) = applyNewFlags(
        if (secureFlagEnabled) {
            params.flags or WindowManager.LayoutParams.FLAG_SECURE
        } else {
            params.flags and (WindowManager.LayoutParams.FLAG_SECURE.inv())
        }
    )

    fun setProperties(properties: PopupProperties?) {
        if (properties != null && properties is AndroidPopupProperties) {
            setSecureFlagEnabled(
                properties.securePolicy
                    .shouldApplySecureFlag(composeView.isFlagSecureEnabled())
            )
        } else {
            setSecureFlagEnabled(composeView.isFlagSecureEnabled())
        }
    }

    private fun applyNewFlags(flags: Int) {
        params.flags = flags

        if (viewAdded) {
            windowManager.updateViewLayout(this, params)
        }
    }

    /**
     * Updates the position of the popup based on current position properties.
     */
    fun updatePosition() {
        val provider = positionProvider ?: return

        val windowGlobalBounds = Rect().let {
            composeView.rootView.getWindowVisibleDisplayFrame(it)
            it.toIntBounds()
        }

        val popupGlobalPosition = provider.calculatePosition(
            parentGlobalBounds,
            windowGlobalBounds,
            parentLayoutDirection,
            popupContentSize
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
        // Note that this implementation is taken from PopupWindow. It actually does not seem to
        // matter whether we return true or false as some upper layer decides on whether the
        // event is propagated to other windows or not. So for focusable the event is consumed but
        // for not focusable it is propagated to other windows.
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
            flags = flags and (
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                ).inv()

            // Enables us to intercept outside clicks even when popup is not focusable
            flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Wrap the frame layout which contains composable content
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            format = PixelFormat.TRANSLUCENT
        }
    }

    private fun Rect.toIntBounds() = IntBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}

internal fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
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