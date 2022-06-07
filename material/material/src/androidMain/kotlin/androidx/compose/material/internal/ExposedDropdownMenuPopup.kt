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

package androidx.compose.material.internal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Popup specific for exposed dropdown menus. b/202810604. Should not be used in other components.
 */
@Composable
internal fun ExposedDropdownMenuPopup(
    onDismissRequest: (() -> Unit)? = null,
    popupPositionProvider: PopupPositionProvider,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val testTag = LocalPopupTestTag.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val popupId = rememberSaveable { UUID.randomUUID() }
    val popupLayout = remember {
        PopupLayout(
            onDismissRequest = onDismissRequest,
            testTag = testTag,
            composeView = view,
            density = density,
            initialPositionProvider = popupPositionProvider,
            popupId = popupId
        ).apply {
            setContent(parentComposition) {
                SimpleStack(
                    Modifier
                        .semantics { this.popup() }
                        // Get the size of the content
                        .onSizeChanged {
                            popupContentSize = it
                            updatePosition()
                        }
                        // Hide the popup while we can't position it correctly
                        .alpha(if (canCalculatePosition) 1f else 0f)
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(popupLayout) {
        popupLayout.show()
        popupLayout.updateParameters(
            onDismissRequest = onDismissRequest,
            testTag = testTag,
            layoutDirection = layoutDirection
        )
        onDispose {
            popupLayout.disposeComposition()
            // Remove the window
            popupLayout.dismiss()
        }
    }

    SideEffect {
        popupLayout.updateParameters(
            onDismissRequest = onDismissRequest,
            testTag = testTag,
            layoutDirection = layoutDirection
        )
    }

    DisposableEffect(popupPositionProvider) {
        popupLayout.positionProvider = popupPositionProvider
        popupLayout.updatePosition()
        onDispose {}
    }

    // TODO(soboleva): Look at module arrangement so that Box can be
    //  used instead of this custom Layout
    // Get the parent's position, size and layout direction
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentLayoutCoordinates!!
            val layoutSize = coordinates.size

            val position = coordinates.positionInWindow()
            val layoutPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())

            popupLayout.parentBounds = IntRect(layoutPosition, layoutSize)
            // Update the popup's position
            popupLayout.updatePosition()
        }
    ) { _, _ ->
        popupLayout.parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }
}

// TODO(b/139861182): This is a hack to work around Popups not using Semantics for test tags
//  We should either remove it, or come up with an abstracted general solution that isn't specific
//  to Popup
internal val LocalPopupTestTag = compositionLocalOf { "DEFAULT_TEST_TAG" }

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
                val placeables = measurables.fastMap { it.measure(constraints) }
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
    private var onDismissRequest: (() -> Unit)?,
    var testTag: String,
    private val composeView: View,
    density: Density,
    initialPositionProvider: PopupPositionProvider,
    popupId: UUID
) : AbstractComposeView(composeView.context),
    ViewRootForInspector,
    ViewTreeObserver.OnGlobalLayoutListener {
    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params = createLayoutParams()

    /** The logic of positioning the popup relative to its parent. */
    var positionProvider = initialPositionProvider

    // Position params
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
    var parentBounds: IntRect? by mutableStateOf(null)
    var popupContentSize: IntSize? by mutableStateOf(null)

    // Track parent bounds and content size; only show popup once we have both
    val canCalculatePosition by derivedStateOf { parentBounds != null && popupContentSize != null }

    private val maxSupportedElevation = 30.dp

    // The window visible frame used for the last popup position calculation.
    private val previousWindowVisibleFrame = Rect()
    private val tmpWindowVisibleFrame = Rect()

    override val subCompositionView: AbstractComposeView get() = this

    // Specific to exposed dropdown menus.
    private val dismissOnOutsideClick = { offset: Offset?, bounds: IntRect ->
        if (offset == null) false
        else {
            offset.x < bounds.left || offset.x > bounds.right ||
                offset.y < bounds.top || offset.y > bounds.bottom
        }
    }

    init {
        id = android.R.id.content
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        composeView.viewTreeObserver.addOnGlobalLayoutListener(this)
        // Set unique id for AbstractComposeView. This allows state restoration for the state
        // defined inside the Popup via rememberSaveable()
        setTag(R.id.compose_view_saveable_id_tag, "Popup:$popupId")

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

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun show() {
        windowManager.addView(this, params)
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    @Composable
    override fun Content() {
        content()
    }

    /**
     * Taken from PopupWindow
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (keyDispatcherState == null) {
                return super.dispatchKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val state = keyDispatcherState
                state?.startTracking(event, this)
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val state = keyDispatcherState
                if (state != null && state.isTracking(event) && !event.isCanceled) {
                    onDismissRequest?.invoke()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun updateParameters(
        onDismissRequest: (() -> Unit)?,
        testTag: String,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        this.testTag = testTag
        superSetLayoutDirection(layoutDirection)
    }

    /**
     * Updates the position of the popup based on current position properties.
     */
    fun updatePosition() {
        val parentBounds = parentBounds ?: return
        val popupContentSize = popupContentSize ?: return

        val windowSize = previousWindowVisibleFrame.let {
            composeView.getWindowVisibleDisplayFrame(it)
            val bounds = it.toIntBounds()
            IntSize(width = bounds.width, height = bounds.height)
        }

        val popupPosition = positionProvider.calculatePosition(
            parentBounds,
            windowSize,
            parentLayoutDirection,
            popupContentSize
        )

        params.x = popupPosition.x
        params.y = popupPosition.y

        windowManager.updateViewLayout(this, params)
    }

    /**
     * Remove the view from the [WindowManager].
     */
    fun dismiss() {
        ViewTreeLifecycleOwner.set(this, null)
        composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        windowManager.removeViewImmediate(this)
    }

    /**
     * Handles touch screen motion events and calls [onDismissRequest] when the
     * users clicks outside the popup.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        // Note that this implementation is taken from PopupWindow. It actually does not seem to
        // matter whether we return true or false as some upper layer decides on whether the
        // event is propagated to other windows or not. So for focusable the event is consumed but
        // for not focusable it is propagated to other windows.
        if (
            (
                (event.action == MotionEvent.ACTION_DOWN) &&
                    (
                        (event.x < 0) ||
                            (event.x >= width) ||
                            (event.y < 0) ||
                            (event.y >= height)
                        )
                ) ||
            event.action == MotionEvent.ACTION_OUTSIDE
        ) {
            val parentBounds = parentBounds
            val shouldDismiss = parentBounds == null || dismissOnOutsideClick(
                if (event.x != 0f || event.y != 0f) {
                    Offset(
                        params.x + event.x,
                        params.y + event.y
                    )
                } else null,
                parentBounds
            )
            if (shouldDismiss) {
                onDismissRequest?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        // Do nothing. ViewRootImpl will call this method attempting to set the layout direction
        // from the context's locale, but we have one already from the parent composition.
    }

    // Sets the "real" layout direction for our content that we obtain from the parent composition.
    private fun superSetLayoutDirection(layoutDirection: LayoutDirection) {
        val direction = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
        super.setLayoutDirection(direction)
    }

    /**
     * Initialize the LayoutParams specific to [android.widget.PopupWindow].
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            // Start to position the popup in the top left corner, a new position will be calculated
            gravity = Gravity.START or Gravity.TOP

            // Flags specific to exposed dropdown menu.
            flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED

            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Wrap the frame layout which contains composable content
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            format = PixelFormat.TRANSLUCENT

            // accessibilityTitle is not exposed as a public API therefore we set popup window
            // title which is used as a fallback by a11y services
            title = composeView.context.resources.getString(R.string.default_popup_window_title)
        }
    }

    private fun Rect.toIntBounds() = IntRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )

    override fun onGlobalLayout() {
        // Update the position of the popup, in case getWindowVisibleDisplayFrame has changed.
        composeView.getWindowVisibleDisplayFrame(tmpWindowVisibleFrame)
        if (tmpWindowVisibleFrame != previousWindowVisibleFrame) {
            updatePosition()
        }
    }
}
