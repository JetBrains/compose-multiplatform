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
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.platform.withInfiniteAnimationFrameNanos
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import org.jetbrains.annotations.TestOnly

/**
 * Properties used to customize the behavior of a [Popup].
 *
 * @property focusable Whether the popup is focusable. When true, the popup will receive IME
 * events and key presses, such as when the back button is pressed.
 * @property dismissOnBackPress Whether the popup can be dismissed by pressing the back button.
 * If true, pressing the back button will call onDismissRequest. Note that [focusable] must be
 * set to true in order to receive key events such as the back button - if the popup is not
 * focusable then this property does nothing.
 * @property dismissOnClickOutside Whether the popup can be dismissed by clicking outside the
 * popup's bounds. If true, clicking outside the popup will call onDismissRequest.
 * @property securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the popup's
 * window.
 * @property excludeFromSystemGesture A flag to check whether to set the systemGestureExclusionRects.
 * The default is true.
 * @property clippingEnabled Whether to allow the popup window to extend beyond the bounds of the
 * screen. By default the window is clipped to the screen boundaries. Setting this to false will
 * allow windows to be accurately positioned.
 * The default value is true.
 * @property usePlatformDefaultWidth Whether the width of the popup's content should be limited to
 * the platform default, which is smaller than the screen width.
 */
@Immutable
class PopupProperties @ExperimentalComposeUiApi constructor(
    val focusable: Boolean = false,
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
    val securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    val excludeFromSystemGesture: Boolean = true,
    val clippingEnabled: Boolean = true,
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalComposeUiApi
    val usePlatformDefaultWidth: Boolean = false
) {
    @OptIn(ExperimentalComposeUiApi::class)
    constructor(
        focusable: Boolean = false,
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        excludeFromSystemGesture: Boolean = true,
        clippingEnabled: Boolean = true,
    ) : this (
        focusable = focusable,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        securePolicy = securePolicy,
        excludeFromSystemGesture = excludeFromSystemGesture,
        clippingEnabled = clippingEnabled,
        usePlatformDefaultWidth = false
    )

    @OptIn(ExperimentalComposeUiApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PopupProperties) return false

        if (focusable != other.focusable) return false
        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (securePolicy != other.securePolicy) return false
        if (excludeFromSystemGesture != other.excludeFromSystemGesture) return false
        if (clippingEnabled != other.clippingEnabled) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false

        return true
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + focusable.hashCode()
        result = 31 * result + dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + securePolicy.hashCode()
        result = 31 * result + excludeFromSystemGesture.hashCode()
        result = 31 * result + clippingEnabled.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        return result
    }
}

/**
 * Opens a popup with the given content.
 *
 * A popup is a floating container that appears on top of the current activity.
 * It is especially useful for non-modal UI surfaces that remain hidden until they
 * are needed, for example floating menus like Cut/Copy/Paste.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit
) {
    val popupPositioner = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(
            alignment,
            offset
        )
    }

    Popup(
        popupPositionProvider = popupPositioner,
        onDismissRequest = onDismissRequest,
        properties = properties,
        content = content
    )
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
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
            properties = properties,
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
            properties = properties,
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
            properties = properties,
            testTag = testTag,
            layoutDirection = layoutDirection
        )
    }

    DisposableEffect(popupPositionProvider) {
        popupLayout.positionProvider = popupPositionProvider
        popupLayout.updatePosition()
        onDispose {}
    }

    // The parent's bounds can change on any frame without onGloballyPositioned being called, if
    // e.g. the soft keyboard changes visibility. For that reason, we need to check if we've moved
    // on every frame. However, we don't need to handle all moves – most position changes will be
    // handled by onGloballyPositioned. This polling loop only needs to handle the case where the
    // view's absolute position on the screen has changed, so we do a quick check to see if it has,
    // and only do the other position calculations in that case.
    LaunchedEffect(popupLayout) {
        while (isActive) {
            withInfiniteAnimationFrameNanos {}
            popupLayout.pollForLocationOnScreenChange()
        }
    }

    // TODO(soboleva): Look at module arrangement so that Box can be
    //  used instead of this custom Layout
    // Get the parent's position, size and layout direction
    Layout(
        content = {},
        modifier = Modifier
            .onGloballyPositioned { childCoordinates ->
                // This callback is best-effort – the screen coordinates of this layout node can
                // change at any time without this callback being fired (e.g. during IME visibility
                // change). For that reason, updating the position in this callback is not
                // sufficient, and the coordinates are also re-calculated on every frame.
                val parentCoordinates = childCoordinates.parentLayoutCoordinates!!
                popupLayout.updateParentLayoutCoordinates(parentCoordinates)
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

@Composable
internal fun PopupTestTag(tag: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPopupTestTag provides tag, content = content)
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
internal class PopupLayout(
    private var onDismissRequest: (() -> Unit)?,
    private var properties: PopupProperties,
    var testTag: String,
    private val composeView: View,
    density: Density,
    initialPositionProvider: PopupPositionProvider,
    popupId: UUID,
    private val popupLayoutHelper: PopupLayoutHelper = if (Build.VERSION.SDK_INT >= 29) {
        PopupLayoutHelperImpl29()
    } else {
        PopupLayoutHelperImpl()
    }
) : AbstractComposeView(composeView.context),
    ViewRootForInspector {
    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val params = createLayoutParams()

    /** The logic of positioning the popup relative to its parent. */
    var positionProvider = initialPositionProvider

    // Position params
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
    var popupContentSize: IntSize? by mutableStateOf(null)
    private var parentLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null)
    private var parentBounds: IntRect? = null

    /** Track parent coordinates and content size; only show popup once we have both. */
    val canCalculatePosition by derivedStateOf {
        parentLayoutCoordinates != null && popupContentSize != null
    }

    private val maxSupportedElevation = 30.dp

    // The window visible frame used for the last popup position calculation.
    private val previousWindowVisibleFrame = Rect()

    override val subCompositionView: AbstractComposeView get() = this

    init {
        id = android.R.id.content
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
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
    @UiComposable
    override fun Content() {
        content()
    }

    override fun internalOnMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (properties.usePlatformDefaultWidth) {
            super.internalOnMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            // usePlatformDefaultWidth false, so don't want to limit the popup width to the Android
            // platform default. Therefore, we create a new measure spec for width, which
            // corresponds to the full screen width. We do the same for height, even if
            // ViewRootImpl gives it to us from the first measure.
            val displayWidthMeasureSpec = makeMeasureSpec(displayWidth, MeasureSpec.AT_MOST)
            val displayHeightMeasureSpec = makeMeasureSpec(displayHeight, MeasureSpec.AT_MOST)
            super.internalOnMeasure(displayWidthMeasureSpec, displayHeightMeasureSpec)
        }
    }

    override fun internalOnLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.internalOnLayout(changed, left, top, right, bottom)
        // Now set the content size as fixed layout params, such that ViewRootImpl knows
        // the exact window size.
        val child = getChildAt(0) ?: return
        params.width = child.measuredWidth
        params.height = child.measuredHeight
        popupLayoutHelper.updateViewLayout(windowManager, this, params)
    }

    private val displayWidth: Int
        get() {
            val density = context.resources.displayMetrics.density
            return (context.resources.configuration.screenWidthDp * density).roundToInt()
        }

    private val displayHeight: Int
        get() {
            val density = context.resources.displayMetrics.density
            return (context.resources.configuration.screenHeightDp * density).roundToInt()
        }

    /**
     * Taken from PopupWindow
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && properties.dismissOnBackPress) {
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

    /**
     * Set whether the popup can grab a focus and support dismissal.
     */
    private fun setIsFocusable(isFocusable: Boolean) = applyNewFlags(
        if (!isFocusable) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            params.flags and (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
        }
    )

    private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
        val secureFlagEnabled =
            securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
        applyNewFlags(
            if (secureFlagEnabled) {
                params.flags or WindowManager.LayoutParams.FLAG_SECURE
            } else {
                params.flags and (WindowManager.LayoutParams.FLAG_SECURE.inv())
            }
        )
    }

    private fun setClippingEnabled(clippingEnabled: Boolean) = applyNewFlags(
        if (clippingEnabled) {
            params.flags and (WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv())
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
    )

    fun updateParameters(
        onDismissRequest: (() -> Unit)?,
        properties: PopupProperties,
        testTag: String,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        this.testTag = testTag
        setIsFocusable(properties.focusable)
        setSecurePolicy(properties.securePolicy)
        setClippingEnabled(properties.clippingEnabled)
        superSetLayoutDirection(layoutDirection)
    }

    private fun applyNewFlags(flags: Int) {
        params.flags = flags
        popupLayoutHelper.updateViewLayout(windowManager, this, params)
    }

    /**
     * Updates the [LayoutCoordinates] object that is used by [updateParentBounds] to calculate
     * the position of the popup. If the new [LayoutCoordinates] reports new parent bounds, calls
     * [updatePosition].
     */
    fun updateParentLayoutCoordinates(parentLayoutCoordinates: LayoutCoordinates) {
        this.parentLayoutCoordinates = parentLayoutCoordinates
        updateParentBounds()
    }

    /**
     * Used by [pollForLocationOnScreenChange] to read the [composeView]'s absolute position
     * on screen. The array is stored as a field instead of allocated in the method because it's
     * called on every frame.
     */
    private val locationOnScreen = IntArray(2)

    /**
     * Returns true if the absolute location of the [composeView] on the screen has changed since
     * the last call. This method asks the view for its location instead of using Compose APIs like
     * [LayoutCoordinates] because it does less work, and this method is intended to be called on
     * every frame.
     *
     * The location can change without any callbacks being fired if, for example, the soft keyboard
     * is shown or hidden when the window is in `adjustPan` mode. In that case, the window's root
     * view (`ViewRootImpl`) will "scroll" the view hierarchy in a special way that doesn't fire any
     * callbacks.
     */
    fun pollForLocationOnScreenChange() {
        val (oldX, oldY) = locationOnScreen
        composeView.getLocationOnScreen(locationOnScreen)
        if (oldX != locationOnScreen[0] || oldY != locationOnScreen[1]) {
            updateParentBounds()
        }
    }

    /**
     * Re-calculates the bounds of the parent layout node that this popup is anchored to. If they've
     * changed since the last call, calls [updatePosition] to actually calculate the popup's new
     * position and update the window.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun updateParentBounds() {
        val coordinates = parentLayoutCoordinates ?: return
        val layoutSize = coordinates.size

        val position = coordinates.positionInWindow()
        val layoutPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())

        val newParentBounds = IntRect(layoutPosition, layoutSize)
        if (newParentBounds != parentBounds) {
            this.parentBounds = newParentBounds
            updatePosition()
        }
    }

    /**
     * Updates the position of the popup based on current position properties.
     */
    fun updatePosition() {
        val parentBounds = parentBounds ?: return
        val popupContentSize = popupContentSize ?: return

        val windowSize = previousWindowVisibleFrame.let {
            popupLayoutHelper.getWindowVisibleDisplayFrame(composeView, it)
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

        if (properties.excludeFromSystemGesture) {
            // Resolve conflict with gesture navigation back when dragging this handle view on the
            // edge of the screen.
            popupLayoutHelper.setGestureExclusionRects(this, windowSize.width, windowSize.height)
        }

        popupLayoutHelper.updateViewLayout(windowManager, this, params)
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
        if (!properties.dismissOnClickOutside) {
            return super.onTouchEvent(event)
        }
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

            // Flags specific to android.widget.PopupWindow
            flags = flags and (
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                ).inv()

            // Enables us to intercept outside clicks even when popup is not focusable
            flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL

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
}

/**
 * Collection of methods delegated to platform methods to support APIs only available on newer
 * platforms and testing.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal interface PopupLayoutHelper {
    fun getWindowVisibleDisplayFrame(composeView: View, outRect: Rect)
    fun setGestureExclusionRects(composeView: View, width: Int, height: Int)
    fun updateViewLayout(
        windowManager: WindowManager,
        popupView: View,
        params: ViewGroup.LayoutParams
    )
}

private open class PopupLayoutHelperImpl : PopupLayoutHelper {
    override fun getWindowVisibleDisplayFrame(composeView: View, outRect: Rect) {
        composeView.getWindowVisibleDisplayFrame(outRect)
    }

    override fun setGestureExclusionRects(composeView: View, width: Int, height: Int) {
        // do nothing
    }

    override fun updateViewLayout(
        windowManager: WindowManager,
        popupView: View,
        params: ViewGroup.LayoutParams
    ) {
        windowManager.updateViewLayout(popupView, params)
    }
}

@RequiresApi(29)
private class PopupLayoutHelperImpl29 : PopupLayoutHelperImpl() {
    override fun setGestureExclusionRects(composeView: View, width: Int, height: Int) {
        composeView.systemGestureExclusionRects = mutableListOf(
            Rect(
                0,
                0,
                width,
                height
            )
        )
    }
}

internal fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
}

private fun Rect.toIntBounds() = IntRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom
)

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
