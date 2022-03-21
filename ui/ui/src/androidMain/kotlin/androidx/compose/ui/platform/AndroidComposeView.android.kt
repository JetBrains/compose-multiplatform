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

package androidx.compose.ui.platform

import android.view.KeyEvent as AndroidKeyEvent
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.SparseArray
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AndroidAutofill
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillCallback
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.autofill.performAutofill
import androidx.compose.ui.autofill.populateViewStructure
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusManagerImpl
import androidx.compose.ui.focus.focusRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.PlatformHapticFeedback
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.InputModeManagerImpl
import androidx.compose.ui.input.key.Key.Companion.Back
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.DirectionDown
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.Key.Companion.DirectionUp
import androidx.compose.ui.input.key.Key.Companion.Enter
import androidx.compose.ui.input.key.Key.Companion.Escape
import androidx.compose.ui.input.key.Key.Companion.NumPadEnter
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AndroidPointerIcon
import androidx.compose.ui.input.pointer.AndroidPointerIconType
import androidx.compose.ui.input.pointer.MotionEventAdapter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.UsageByParent
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsModifierCore
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.InputDeviceCompat.SOURCE_ROTARY_ENCODER
import androidx.core.view.MotionEventCompat.AXIS_SCROLL
import androidx.core.view.ViewCompat
import androidx.core.view.ViewConfigurationCompat.getScaledHorizontalScrollFactor
import androidx.core.view.ViewConfigurationCompat.getScaledVerticalScrollFactor
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import java.lang.reflect.Method
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor", "VisibleForTests")
@OptIn(ExperimentalComposeUiApi::class)
internal class AndroidComposeView(context: Context) :
    ViewGroup(context), Owner, ViewRootForTest, PositionCalculator, DefaultLifecycleObserver {

    /**
     * Remembers the position of the last pointer input event that was down. This position will be
     * used to calculate whether this view is considered scrollable via [canScrollHorizontally]/
     * [canScrollVertically].
     */
    private var lastDownPointerPosition: Offset = Offset.Unspecified

    /**
     * Signal that AndroidComposeView's superclass constructors have finished running.
     * If this is false, it's because the runtime's default uninitialized value is currently
     * visible and AndroidComposeView's constructor hasn't started running yet. In this state
     * other expected invariants do not hold, e.g. property delegates may not be initialized.
     * View/ViewGroup have a history of calling non-final methods in their constructors that
     * can lead to this case, e.g. [onRtlPropertiesChanged].
     */
    private var superclassInitComplete = true

    override val sharedDrawScope = LayoutNodeDrawScope()

    override val view: View get() = this

    override var density = Density(context)
        private set

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsModifierCore.generateSemanticsId(),
        mergeDescendants = false,
        clearAndSetSemantics = false,
        properties = {}
    )

    private val _focusManager: FocusManagerImpl = FocusManagerImpl()
    override val focusManager: FocusManager
        get() = _focusManager

    private val _windowInfo: WindowInfoImpl = WindowInfoImpl()
    override val windowInfo: WindowInfo
        get() = _windowInfo

    // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
    //  that this common logic can be used by all owners.
    private val keyInputModifier: KeyInputModifier = KeyInputModifier(
        onKeyEvent = {
            val focusDirection = getFocusDirection(it)
            if (focusDirection == null || it.type != KeyDown) return@KeyInputModifier false

            // Consume the key event if we moved focus.
            focusManager.moveFocus(focusDirection)
        },
        onPreviewKeyEvent = null
    )

    private val rotaryInputModifier = Modifier.onRotaryScrollEvent {
        // TODO(b/210748692): call focusManager.moveFocus() in response to rotary events.
        false
    }

    private val canvasHolder = CanvasHolder()

    override val root = LayoutNode().also {
        it.measurePolicy = RootMeasurePolicy
        // Composed modifiers cannot be added here directly
        it.modifier = Modifier
            .then(semanticsModifier)
            .then(rotaryInputModifier)
            .then(_focusManager.modifier)
            .then(keyInputModifier)
        it.density = density
    }

    override val rootForTest: RootForTest = this

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)
    private val accessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(this)

    // Used by components that want to provide autofill semantic information.
    // TODO: Replace with SemanticsTree: Temporary hack until we have a semantics tree implemented.
    // TODO: Replace with SemanticsTree.
    //  This is a temporary hack until we have a semantics tree implemented.
    override val autofillTree = AutofillTree()

    // OwnedLayers that are dirty and should be redrawn.
    private val dirtyLayers = mutableListOf<OwnedLayer>()

    // OwnerLayers that invalidated themselves during their last draw. They will be redrawn
    // during the next AndroidComposeView dispatchDraw pass.
    private var postponedDirtyLayers: MutableList<OwnedLayer>? = null

    private var isDrawingContent = false

    private val motionEventAdapter = MotionEventAdapter()
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    // TODO(mount): reinstate when coroutines are supported by IR compiler
    // private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    /**
     * Used for updating LocalConfiguration when configuration changes - consume LocalConfiguration
     * instead of changing this observer if you are writing a component that adapts to
     * configuration changes.
     */
    var configurationChangeObserver: (Configuration) -> Unit = {}

    private val _autofill = if (autofillSupported()) AndroidAutofill(this, autofillTree) else null

    // Used as a CompositionLocal for performing autofill.
    override val autofill: Autofill? get() = _autofill

    private var observationClearRequested = false

    /**
     * Provide clipboard manager to the user. Use the Android version of clipboard manager.
     */
    override val clipboardManager = AndroidClipboardManager(context)

    /**
     * Provide accessibility manager to the user. Use the Android version of accessibility manager.
     */
    override val accessibilityManager = AndroidAccessibilityManager(context)

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        if (handler?.looper === Looper.myLooper()) {
            command()
        } else {
            handler?.post(command)
        }
    }

    @OptIn(InternalCoreApi::class)
    override var showLayoutBounds = false

    private var _androidViewsHandler: AndroidViewsHandler? = null
    internal val androidViewsHandler: AndroidViewsHandler
        get() {
            if (_androidViewsHandler == null) {
                _androidViewsHandler = AndroidViewsHandler(context)
                addView(_androidViewsHandler)
            }
            return _androidViewsHandler!!
        }
    private var viewLayersContainer: DrawChildContainer? = null

    // The constraints being used by the last onMeasure. It is set to null in onLayout. It allows
    // us to detect the case when the View was measured twice with different constraints within
    // the same measure pass.
    private var onMeasureConstraints: Constraints? = null

    // Will be set to true when we were measured twice with different constraints during the last
    // measure pass.
    private var wasMeasuredWithMultipleConstraints = false

    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration
    override val viewConfiguration: ViewConfiguration =
        AndroidViewConfiguration(android.view.ViewConfiguration.get(context))

    override val hasPendingMeasureOrLayout
        get() = measureAndLayoutDelegate.hasPendingMeasureOrLayout

    private var globalPosition: IntOffset = IntOffset.Zero

    private val tmpPositionArray = intArrayOf(0, 0)
    private val viewToWindowMatrix = Matrix()
    private val windowToViewMatrix = Matrix()
    private val tmpCalculationMatrix = Matrix()

    @VisibleForTesting
    internal var lastMatrixRecalculationAnimationTime = -1L
    private var forceUseMatrixCache = false

    /**
     * On some devices, the `getLocationOnScreen()` returns `(0, 0)` even when the Window
     * is offset in special circumstances. This contains the screen coordinates of the containing
     * Window the last time the [viewToWindowMatrix] and [windowToViewMatrix] were recalculated.
     */
    private var windowPosition = Offset.Infinite

    // Used to track whether or not there was an exception while creating an MRenderNode
    // so that we don't have to continue using try/catch after fails once.
    private var isRenderNodeCompatible = true

    /**
     * Current [ViewTreeOwners]. Use [setOnViewTreeOwnersAvailable] if you want to
     * execute your code when the object will be created.
     */
    var viewTreeOwners: ViewTreeOwners? by mutableStateOf(null)
        private set

    private var onViewTreeOwnersAvailable: ((ViewTreeOwners) -> Unit)? = null

    // executed when the layout pass has been finished. as a result of it our view could be moved
    // inside the window (we are interested not only in the event when our parent positioned us
    // on a different position, but also in the position of each of the grandparents as all these
    // positions add up to final global position)
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        updatePositionCacheAndDispatch()
    }

    // executed when a scrolling container like ScrollView of RecyclerView performed the scroll,
    // this could affect our global position
    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        updatePositionCacheAndDispatch()
    }

    // executed whenever the touch mode changes.
    private val touchModeChangeListener = ViewTreeObserver.OnTouchModeChangeListener { touchMode ->
        _inputModeManager.inputMode = if (touchMode) Touch else Keyboard
        _focusManager.fetchUpdatedFocusProperties()
    }

    private val textInputServiceAndroid = TextInputServiceAndroid(this)

    @OptIn(InternalComposeUiApi::class)
    override val textInputService = textInputServiceFactory(textInputServiceAndroid)

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver")
    )
    @Suppress("DEPRECATION")
    override val fontLoader: Font.ResourceLoader = AndroidFontResourceLoader(context)

    // Backed by mutableStateOf so that the local provider recomposes when it changes
    // FontFamily.Resolver is not guaranteed to be stable or immutable, hence referential check
    override var fontFamilyResolver: FontFamily.Resolver by mutableStateOf(
        createFontFamilyResolver(context),
        referentialEqualityPolicy()
    )
        private set

    // keeps track of changes in font weight adjustment to update fontFamilyResolver
    private var currentFontWeightAdjustment: Int =
        context.resources.configuration.fontWeightAdjustmentCompat

    private val Configuration.fontWeightAdjustmentCompat: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) fontWeightAdjustment else 0

    // Backed by mutableStateOf so that the ambient provider recomposes when it changes
    override var layoutDirection by mutableStateOf(
        context.resources.configuration.localeLayoutDirection
    )
        private set

    /**
     * Provide haptic feedback to the user. Use the Android version of haptic feedback.
     */
    override val hapticFeedBack: HapticFeedback =
        PlatformHapticFeedback(this)

    /**
     * Provide an instance of [InputModeManager] which is available as a CompositionLocal.
     */
    private val _inputModeManager = InputModeManagerImpl(
        initialInputMode = if (isInTouchMode) Touch else Keyboard,
        onRequestInputModeChange = {
            when (it) {
                // Android doesn't support programmatically switching to touch mode, so we
                // don't do anything, but just return true if we are already in touch mode.
                Touch -> isInTouchMode

                // If we are already in keyboard mode, we return true, otherwise, we call
                // requestFocusFromTouch, which puts the system in non-touch mode.
                Keyboard -> if (isInTouchMode) requestFocusFromTouch() else true

                else -> false
            }
        }
    )
    override val inputModeManager: InputModeManager get() = _inputModeManager

    /**
     * Provide textToolbar to the user, for text-related operation. Use the Android version of
     * floating toolbar(post-M) and primary toolbar(pre-M).
     */
    override val textToolbar: TextToolbar = AndroidTextToolbar(this)

    /**
     * When the first event for a mouse is ACTION_DOWN, an ACTION_HOVER_ENTER is never sent.
     * This means that we won't receive an `Enter` event for the first mouse. In order to prevent
     * this problem, we track whether or not the previous event was with the mouse inside and
     * if not, we can create a simulated mouse enter event to force an enter.
     */
    private var previousMotionEvent: MotionEvent? = null

    /**
     * The time of the last layout. This is used to send a synthetic MotionEvent.
     */
    private var relayoutTime = 0L

    /**
     * A cache for OwnedLayers. Recreating ViewLayers is expensive, so we avoid it as much
     * as possible. This also helps a little with RenderNodeLayers as well.
     */
    private val layerCache = WeakCache<OwnedLayer>()

    /**
     * List of lambdas to be called when [onEndApplyChanges] is called.
     */
    private val endApplyChangesListeners = mutableVectorOf<(() -> Unit)?>()

    /**
     * Runnable used to update the pointer position after layout. If
     * another pointer event comes in before this runs, this Runnable will be removed and
     * not executed.
     */
    private val resendMotionEventRunnable = object : Runnable {
        override fun run() {
            removeCallbacks(this)
            val lastMotionEvent = previousMotionEvent
            if (lastMotionEvent != null) {
                val wasMouseEvent = lastMotionEvent.getToolType(0) == TOOL_TYPE_MOUSE
                val action = lastMotionEvent.actionMasked
                val resend = if (wasMouseEvent) {
                    action != ACTION_HOVER_EXIT && action != ACTION_UP
                } else {
                    action != ACTION_UP
                }
                if (resend) {
                    val newAction =
                        if (action == ACTION_HOVER_MOVE || action == ACTION_HOVER_ENTER) {
                            ACTION_HOVER_MOVE
                        } else {
                            ACTION_MOVE
                        }
                    sendSimulatedEvent(lastMotionEvent, newAction, relayoutTime, forceHover = false)
                }
            }
        }
    }

    /**
     * If an [ACTION_HOVER_EXIT] event is received, it could be because an [ACTION_DOWN] is coming
     * from a mouse or stylus. We can't know for certain until the next event is sent. This message
     * is posted after receiving the [ACTION_HOVER_EXIT] to send the event if nothing else is
     * received before that.
     */
    private val sendHoverExitEvent = Runnable {
        hoverExitReceived = false
        val lastEvent = previousMotionEvent!!
        check(lastEvent.actionMasked == ACTION_HOVER_EXIT) {
            "The ACTION_HOVER_EXIT event was not cleared."
        }
        sendMotionEvent(lastEvent)
    }

    /**
     * Set to `true` when [sendHoverExitEvent] has been posted.
     */
    private var hoverExitReceived = false

    /**
     * Callback for [measureAndLayout] to update the pointer position 150ms after layout.
     */
    private val resendMotionEventOnLayout: () -> Unit = {
        val lastEvent = previousMotionEvent
        if (lastEvent != null) {
            when (lastEvent.actionMasked) {
                // We currently only care about hover events being updated when layout changes
                ACTION_HOVER_ENTER, ACTION_HOVER_MOVE -> {
                    relayoutTime = SystemClock.uptimeMillis()
                    post(resendMotionEventRunnable)
                }
            }
        }
    }

    init {
        setWillNotDraw(false)
        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidComposeViewVerificationHelperMethodsO.focusable(
                this,
                focusable = View.FOCUSABLE,
                defaultFocusHighlightEnabled = false
            )
        }
        isFocusableInTouchMode = true
        clipChildren = false
        isTransitionGroup = true
        ViewCompat.setAccessibilityDelegate(this, accessibilityDelegate)
        ViewRootForTest.onViewCreatedCallback?.invoke(this)
        root.attach(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Support for this feature in Compose is tracked here: b/207654434
            AndroidComposeViewForceDarkModeQ.disallowForceDark(this)
        }
    }

    /**
     * Since this view has its own concept of internal focus, it needs to report that to the view
     * system for accurate focus searching and so ViewRootImpl will scroll correctly.
     */
    override fun getFocusedRect(rect: Rect) {
        _focusManager.getActiveFocusModifier()?.focusRect()?.let {
            rect.left = it.left.roundToInt()
            rect.top = it.top.roundToInt()
            rect.right = it.right.roundToInt()
            rect.bottom = it.bottom.roundToInt()
        } ?: super.getFocusedRect(rect)
    }

    override fun onResume(owner: LifecycleOwner) {
        // Refresh in onResume in case the value has changed.
        showLayoutBounds = getIsShowingLayoutBounds()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        Log.d(FocusTag, "Owner FocusChanged($gainFocus)")
        with(_focusManager) {
            if (gainFocus) takeFocus() else releaseFocus()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        _windowInfo.isWindowFocused = hasWindowFocus
        super.onWindowFocusChanged(hasWindowFocus)

        if (hasWindowFocus) {
            // Refresh in onResume in case the value has changed from the quick settings tile, in
            // which case the activity won't be paused/resumed (b/225937688).
            getIsShowingLayoutBounds().also { newShowLayoutBounds ->
                if (showLayoutBounds != newShowLayoutBounds) {
                    showLayoutBounds = newShowLayoutBounds
                    // Unlike in onResume, getting window focus doesn't automatically trigger a new
                    // draw pass, so we have to do that manually.
                    invalidateDescendants()
                }
            }
        }
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        return keyInputModifier.processKeyInput(keyEvent)
    }

    override fun dispatchKeyEvent(event: AndroidKeyEvent) =
        if (isFocused) {
            // Focus lies within the Compose hierarchy, so we dispatch the key event to the
            // appropriate place.
            sendKeyEvent(KeyEvent(event))
        } else {
            // This Owner has a focused child view, which is a view interop use case,
            // so we use the default ViewGroup behavior which will route tke key event to the
            // focused view.
            super.dispatchKeyEvent(event)
        }

    override fun onAttach(node: LayoutNode) {
    }

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        requestClearInvalidObservations()
    }

    fun requestClearInvalidObservations() {
        observationClearRequested = true
    }

    override fun onEndApplyChanges() {
        if (observationClearRequested) {
            snapshotObserver.clearInvalidObservations()
            observationClearRequested = false
        }
        val childAndroidViews = _androidViewsHandler
        if (childAndroidViews != null) {
            clearChildInvalidObservations(childAndroidViews)
        }
        // Listeners can add more items to the list and we want to ensure that they
        // are executed after being added, so loop until the list is empty
        while (endApplyChangesListeners.isNotEmpty()) {
            val size = endApplyChangesListeners.size
            for (i in 0 until size) {
                val listener = endApplyChangesListeners[i]
                // null out the item so that if the listener is re-added then we execute it again.
                endApplyChangesListeners[i] = null
                listener?.invoke()
            }
            // Remove all the items that were visited. Removing items shifts all items after
            // to the front of the list, so removing in a chunk is cheaper than removing one-by-one
            endApplyChangesListeners.removeRange(0, size)
        }
    }

    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        if (listener !in endApplyChangesListeners) {
            endApplyChangesListeners += listener
        }
    }

    private fun clearChildInvalidObservations(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AndroidComposeView) {
                child.onEndApplyChanges()
            } else if (child is ViewGroup) {
                clearChildInvalidObservations(child)
            }
        }
    }

    /**
     * Called to inform the owner that a new Android [View] was [attached][Owner.onAttach]
     * to the hierarchy.
     */
    fun addAndroidView(view: AndroidViewHolder, layoutNode: LayoutNode) {
        androidViewsHandler.holderToLayoutNode[view] = layoutNode
        androidViewsHandler.addView(view)
        androidViewsHandler.layoutNodeToHolder[layoutNode] = view
        // Fetching AccessibilityNodeInfo from a View which is not set to
        // IMPORTANT_FOR_ACCESSIBILITY_YES will return null.
        ViewCompat.setImportantForAccessibility(
            view,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES
        )
        val thisView = this
        ViewCompat.setAccessibilityDelegate(
            view,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    var parentId = SemanticsNode(layoutNode.outerSemantics!!, false).parent!!.id
                    if (parentId == semanticsOwner.unmergedRootSemanticsNode.id) {
                        parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    }
                    info.setParent(thisView, parentId)
                }
            }
        )
    }

    /**
     * Called to inform the owner that an Android [View] was [detached][Owner.onDetach]
     * from the hierarchy.
     */
    fun removeAndroidView(view: AndroidViewHolder) {
        androidViewsHandler.removeView(view)
        androidViewsHandler.layoutNodeToHolder.remove(
            androidViewsHandler.holderToLayoutNode.remove(view)
        )
        ViewCompat.setImportantForAccessibility(
            view,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        )
    }

    /**
     * Called to ask the owner to draw a child Android [View] to [canvas].
     */
    fun drawAndroidView(view: AndroidViewHolder, canvas: android.graphics.Canvas) {
        androidViewsHandler.drawView(view, canvas)
    }

    private fun scheduleMeasureAndLayout(nodeToRemeasure: LayoutNode? = null) {
        if (!isLayoutRequested && isAttachedToWindow) {
            if (wasMeasuredWithMultipleConstraints && nodeToRemeasure != null) {
                // if nodeToRemeasure can potentially resize the root and the view was measured
                // twice with different constraints last time it means the constraints we have could
                // be not the final constraints and in fact our parent ViewGroup can remeasure us
                // with larger constraints if we call requestLayout()
                var node = nodeToRemeasure
                while (node != null && node.measuredByParent == UsageByParent.InMeasureBlock) {
                    node = node.parent
                }
                if (node === root) {
                    requestLayout()
                    return
                }
            }
            if (width == 0 || height == 0) {
                // if the view has no size calling invalidate() will be skipped
                requestLayout()
            } else {
                invalidate()
            }
        }
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        trace("AndroidOwner:measureAndLayout") {
            val resend = if (sendPointerUpdate) resendMotionEventOnLayout else null
            val rootNodeResized = measureAndLayoutDelegate.measureAndLayout(resend)
            if (rootNodeResized) {
                requestLayout()
            }
            measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
        }
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        trace("AndroidOwner:measureAndLayout") {
            measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
            measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
        }
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode) {
        measureAndLayoutDelegate.forceMeasureTheSubtree(layoutNode)
    }

    override fun onRequestMeasure(layoutNode: LayoutNode, forceRequest: Boolean) {
        if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest)) {
            scheduleMeasureAndLayout(layoutNode)
        }
    }

    override fun onRequestRelayout(layoutNode: LayoutNode, forceRequest: Boolean) {
        if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        trace("AndroidOwner:onMeasure") {
            if (!isAttachedToWindow) {
                invalidateLayoutNodeMeasurement(root)
            }
            val (minWidth, maxWidth) = convertMeasureSpec(widthMeasureSpec)
            val (minHeight, maxHeight) = convertMeasureSpec(heightMeasureSpec)

            val constraints = Constraints(minWidth, maxWidth, minHeight, maxHeight)
            if (onMeasureConstraints == null) {
                // first onMeasure after last onLayout
                onMeasureConstraints = constraints
                wasMeasuredWithMultipleConstraints = false
            } else if (onMeasureConstraints != constraints) {
                // we were remeasured twice with different constraints after last onLayout
                wasMeasuredWithMultipleConstraints = true
            }
            measureAndLayoutDelegate.updateRootConstraints(constraints)
            measureAndLayoutDelegate.measureAndLayout(resendMotionEventOnLayout)
            setMeasuredDimension(root.width, root.height)
            if (_androidViewsHandler != null) {
                androidViewsHandler.measure(
                    MeasureSpec.makeMeasureSpec(root.width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(root.height, MeasureSpec.EXACTLY)
                )
            }
        }
    }

    private fun convertMeasureSpec(measureSpec: Int): Pair<Int, Int> {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> size to size
            MeasureSpec.UNSPECIFIED -> 0 to Constraints.Infinity
            MeasureSpec.AT_MOST -> 0 to size
            else -> throw IllegalStateException()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        onMeasureConstraints = null
        // we postpone onPositioned callbacks until onLayout as LayoutCoordinates
        // are currently wrong if you try to get the global(activity) coordinates -
        // View is not yet laid out.
        updatePositionCacheAndDispatch()
        if (_androidViewsHandler != null) {
            // Even if we laid out during onMeasure, we want to set the bounds of the
            // AndroidViewsHandler for accessibility and for Views making assumptions based on
            // the size of their ancestors. Usually the Views in the hierarchy will not
            // be relaid out, as they have not requested layout in the meantime.
            // However, there is also chance for the AndroidViewsHandler and the children to be
            // isLayoutRequested at this point, in case the Views hierarchy receives forceLayout().
            // In case of a forceLayout(), calling layout here will traverse the entire subtree
            // and replace the Views at the same position, which is needed to clean up their
            // layout state, which otherwise might cause further requestLayout()s to be blocked.
            androidViewsHandler.layout(0, 0, r - l, b - t)
        }
    }

    private fun updatePositionCacheAndDispatch() {
        var positionChanged = false
        getLocationOnScreen(tmpPositionArray)
        if (globalPosition.x != tmpPositionArray[0] || globalPosition.y != tmpPositionArray[1]) {
            globalPosition = IntOffset(tmpPositionArray[0], tmpPositionArray[1])
            positionChanged = true
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(forceDispatch = positionChanged)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        // First try the layer cache
        val layer = layerCache.pop()
        if (layer !== null) {
            layer.reuseLayer(drawBlock, invalidateParentLayer)
            return layer
        }

        // RenderNode is supported on Q+ for certain, but may also be supported on M-O.
        // We can't be confident that RenderNode is supported, so we try and fail over to
        // the ViewLayer implementation. We'll try even on on P devices, but it will fail
        // until ART allows things on the unsupported list on P.
        if (isHardwareAccelerated &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            isRenderNodeCompatible
        ) {
            try {
                return RenderNodeLayer(
                    this,
                    drawBlock,
                    invalidateParentLayer
                )
            } catch (_: Throwable) {
                isRenderNodeCompatible = false
            }
        }
        if (viewLayersContainer == null) {
            if (!ViewLayer.hasRetrievedMethod) {
                // Test to see if updateDisplayList() can be called. If this fails then
                // ViewLayer.shouldUseDispatchDraw will be true.
                ViewLayer.updateDisplayList(View(context))
            }
            viewLayersContainer = if (ViewLayer.shouldUseDispatchDraw) {
                DrawChildContainer(context)
            } else {
                ViewLayerContainer(context)
            }
            addView(viewLayersContainer)
        }
        return ViewLayer(this, viewLayersContainer!!, drawBlock, invalidateParentLayer)
    }

    /**
     * Return [layer] to the layer cache. It can be reused in [createLayer] after this.
     * Returns `true` if it was recycled or `false` if it will be discarded.
     */
    internal fun recycle(layer: OwnedLayer): Boolean {
        // L throws during RenderThread when reusing the Views. The stack trace
        // wasn't easy to decode, so this work-around keeps up to 10 Views active
        // only for L. On other versions, it uses the WeakHashMap to retain as many
        // as are convenient.
        val cacheValue = viewLayersContainer == null || ViewLayer.shouldUseDispatchDraw ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ||
            layerCache.size < MaximumLayerCacheSize
        if (cacheValue) {
            layerCache.push(layer)
        }
        return cacheValue
    }

    override fun onSemanticsChange() {
        accessibilityDelegate.onSemanticsChange()
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        accessibilityDelegate.onLayoutChange(layoutNode)
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        measureAndLayoutDelegate.registerOnLayoutCompletedListener(listener)
        scheduleMeasureAndLayout()
    }

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? {
        return when (keyEvent.key) {
            Tab -> if (keyEvent.isShiftPressed) Previous else Next
            DirectionRight -> Right
            DirectionLeft -> Left
            DirectionUp -> Up
            DirectionDown -> Down
            DirectionCenter, Enter, NumPadEnter -> In
            Back, Escape -> Out
            else -> null
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        if (!isAttachedToWindow) {
            invalidateLayers(root)
        }
        measureAndLayout()

        isDrawingContent = true
        // we don't have to observe here because the root has a layer modifier
        // that will observe all children. The AndroidComposeView has only the
        // root, so it doesn't have to invalidate itself based on model changes.
        canvasHolder.drawInto(canvas) { root.draw(this) }

        if (dirtyLayers.isNotEmpty()) {
            for (i in 0 until dirtyLayers.size) {
                val layer = dirtyLayers[i]
                layer.updateDisplayList()
            }
        }

        if (ViewLayer.shouldUseDispatchDraw) {
            // We must update the display list of all children using dispatchDraw()
            // instead of updateDisplayList(). But since we don't want to actually draw
            // the contents, we will clip out everything from the canvas.
            val saveCount = canvas.save()
            canvas.clipRect(0f, 0f, 0f, 0f)

            super.dispatchDraw(canvas)
            canvas.restoreToCount(saveCount)
        }

        dirtyLayers.clear()
        isDrawingContent = false

        // updateDisplayList operations performed above (during root.draw and during the explicit
        // layer.updateDisplayList() calls) can result in the same layers being invalidated. These
        // layers have been added to postponedDirtyLayers and will be redrawn during the next
        // dispatchDraw.
        if (postponedDirtyLayers != null) {
            val postponed = postponedDirtyLayers!!
            dirtyLayers.addAll(postponed)
            postponed.clear()
        }
    }

    internal fun notifyLayerIsDirty(layer: OwnedLayer, isDirty: Boolean) {
        if (!isDirty) {
            // It is correct to remove the layer here regardless of this if, but for performance
            // we are hackily not doing the removal here in order to just do clear() a bit later.
            if (!isDrawingContent) require(dirtyLayers.remove(layer))
        } else if (!isDrawingContent) {
            dirtyLayers += layer
        } else {
            val postponed = postponedDirtyLayers
                ?: mutableListOf<OwnedLayer>().also { postponedDirtyLayers = it }
            postponed += layer
        }
    }

    /**
     * The callback to be executed when [viewTreeOwners] is created and not-null anymore.
     * Note that this callback will be fired inline when it is already available
     */
    fun setOnViewTreeOwnersAvailable(callback: (ViewTreeOwners) -> Unit) {
        val viewTreeOwners = viewTreeOwners
        if (viewTreeOwners != null) {
            callback(viewTreeOwners)
        }
        if (!isAttachedToWindow) {
            onViewTreeOwnersAvailable = callback
        }
    }

    suspend fun boundsUpdatesEventLoop() {
        accessibilityDelegate.boundsUpdatesEventLoop()
    }

    /**
     * Android has an issue where calling showSoftwareKeyboard after calling
     * hideSoftwareKeyboard, it results in keyboard flickering and sometimes the keyboard ends up
     * being hidden even though the most recent call was to showKeyboard.
     *
     * This function starts a suspended function that listens for show/hide commands and only
     * runs the latest command.
     */
    suspend fun keyboardVisibilityEventLoop() {
        textInputServiceAndroid.textInputCommandEventLoop()
    }

    /**
     * Walks the entire LayoutNode sub-hierarchy and marks all nodes as needing measurement.
     */
    private fun invalidateLayoutNodeMeasurement(node: LayoutNode) {
        measureAndLayoutDelegate.requestRemeasure(node)
        node._children.forEach { invalidateLayoutNodeMeasurement(it) }
    }

    /**
     * Walks the entire LayoutNode sub-hierarchy and marks all layers as needing to be redrawn.
     */
    private fun invalidateLayers(node: LayoutNode) {
        node.invalidateLayers()
        node._children.forEach { invalidateLayers(it) }
    }

    override fun invalidateDescendants() {
        invalidateLayers(root)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidateLayoutNodeMeasurement(root)
        invalidateLayers(root)
        snapshotObserver.startObserving()
        ifDebug {
            if (autofillSupported()) {
                _autofill?.let { AutofillCallback.register(it) }
            }
        }

        val lifecycleOwner = ViewTreeLifecycleOwner.get(this)
        val savedStateRegistryOwner = findViewTreeSavedStateRegistryOwner()

        val oldViewTreeOwners = viewTreeOwners
        // We need to change the ViewTreeOwner if there isn't one yet (null)
        // or if either the lifecycleOwner or savedStateRegistryOwner has changed.
        val resetViewTreeOwner = oldViewTreeOwners == null ||
            (
                (lifecycleOwner != null && savedStateRegistryOwner != null) &&
                    (
                        lifecycleOwner !== oldViewTreeOwners.lifecycleOwner ||
                            savedStateRegistryOwner !== oldViewTreeOwners.lifecycleOwner
                        )
                )
        if (resetViewTreeOwner) {
            if (lifecycleOwner == null) {
                throw IllegalStateException(
                    "Composed into the View which doesn't propagate ViewTreeLifecycleOwner!"
                )
            }
            if (savedStateRegistryOwner == null) {
                throw IllegalStateException(
                    "Composed into the View which doesn't propagate" +
                        "ViewTreeSavedStateRegistryOwner!"
                )
            }
            oldViewTreeOwners?.lifecycleOwner?.lifecycle?.removeObserver(this)
            lifecycleOwner.lifecycle.addObserver(this)
            val viewTreeOwners = ViewTreeOwners(
                lifecycleOwner = lifecycleOwner,
                savedStateRegistryOwner = savedStateRegistryOwner
            )
            this.viewTreeOwners = viewTreeOwners
            onViewTreeOwnersAvailable?.invoke(viewTreeOwners)
            onViewTreeOwnersAvailable = null
        }
        viewTreeOwners!!.lifecycleOwner.lifecycle.addObserver(this)
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
        viewTreeObserver.addOnTouchModeChangeListener(touchModeChangeListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapshotObserver.stopObserving()
        viewTreeOwners?.lifecycleOwner?.lifecycle?.removeObserver(this)
        ifDebug {
            if (autofillSupported()) {
                _autofill?.let { AutofillCallback.unregister(it) }
            }
        }
        viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
        viewTreeObserver.removeOnTouchModeChangeListener(touchModeChangeListener)
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure?, flags: Int) {
        if (autofillSupported() && structure != null) _autofill?.populateViewStructure(structure)
    }

    override fun autofill(values: SparseArray<AutofillValue>) {
        if (autofillSupported()) _autofill?.performAutofill(values)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent) = when (event.actionMasked) {
        ACTION_SCROLL -> when {
            event.isFromSource(SOURCE_ROTARY_ENCODER) -> handleRotaryEvent(event)
            else -> handleMotionEvent(event).dispatchedToAPointerInputModifier
        }
        else -> super.dispatchGenericMotionEvent(event)
    }

    // TODO(shepshapard): Test this method.
    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (hoverExitReceived) {
            // Go ahead and send ACTION_HOVER_EXIT if this isn't an ACTION_DOWN for the same
            // pointer
            removeCallbacks(sendHoverExitEvent)
            val lastEvent = previousMotionEvent!!
            if (motionEvent.actionMasked != ACTION_DOWN ||
                hasChangedDevices(motionEvent, lastEvent)
            ) {
                sendHoverExitEvent.run()
            } else {
                hoverExitReceived = false
            }
        }
        if (isBadMotionEvent(motionEvent)) {
            return false // Bad MotionEvent. Don't handle it.
        }

        if (motionEvent.actionMasked == ACTION_MOVE && !isPositionChanged(motionEvent)) {
            // There was no movement from previous MotionEvent, so we don't need to dispatch this.
            // This could be a scroll event or some other non-touch event that results in an
            // ACTION_MOVE without any movement.
            return false
        }

        val processResult = handleMotionEvent(motionEvent)

        if (processResult.anyMovementConsumed) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return processResult.dispatchedToAPointerInputModifier
    }

    private fun handleRotaryEvent(event: MotionEvent): Boolean {
        val config = android.view.ViewConfiguration.get(context)
        val axisValue = -event.getAxisValue(AXIS_SCROLL)
        val rotaryEvent = RotaryScrollEvent(
            verticalScrollPixels = axisValue * getScaledVerticalScrollFactor(config, context),
            horizontalScrollPixels = axisValue * getScaledHorizontalScrollFactor(config, context),
            uptimeMillis = event.eventTime
        )
        return _focusManager.getActiveFocusModifier()?.propagateRotaryEvent(rotaryEvent) ?: false
    }

    private fun handleMotionEvent(motionEvent: MotionEvent): ProcessResult {
        removeCallbacks(resendMotionEventRunnable)
        try {
            recalculateWindowPosition(motionEvent)
            forceUseMatrixCache = true
            measureAndLayout(sendPointerUpdate = false)
            desiredPointerIcon = null
            val result = trace("AndroidOwner:onTouch") {
                val action = motionEvent.actionMasked
                val lastEvent = previousMotionEvent

                val wasMouseEvent = lastEvent?.getToolType(0) == TOOL_TYPE_MOUSE
                if (lastEvent != null &&
                    hasChangedDevices(motionEvent, lastEvent)
                ) {
                    if (isDevicePressEvent(lastEvent)) {
                        // Send a cancel event
                        pointerInputEventProcessor.processCancel()
                    } else if (lastEvent.actionMasked != ACTION_HOVER_EXIT && wasMouseEvent) {
                        // The mouse cursor disappeared without sending an ACTION_HOVER_EXIT, so
                        // we have to send that event.
                        sendSimulatedEvent(lastEvent, ACTION_HOVER_EXIT, lastEvent.eventTime)
                    }
                }

                val isMouseEvent = motionEvent.getToolType(0) == TOOL_TYPE_MOUSE

                if (!wasMouseEvent &&
                    isMouseEvent &&
                    action != ACTION_CANCEL &&
                    action != ACTION_HOVER_ENTER &&
                    isInBounds(motionEvent)
                ) {
                    // We didn't previously have an enter event and we're getting our first
                    // mouse event. Send a simulated enter event so that we have a consistent
                    // enter/exit.
                    sendSimulatedEvent(motionEvent, ACTION_HOVER_ENTER, motionEvent.eventTime)
                }
                lastEvent?.recycle()
                previousMotionEvent = MotionEvent.obtainNoHistory(motionEvent)

                sendMotionEvent(motionEvent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                AndroidComposeViewVerificationHelperMethodsN.setPointerIcon(
                    this,
                    desiredPointerIcon
                )
            }
            return result
        } finally {
            forceUseMatrixCache = false
        }
    }

    private fun hasChangedDevices(event: MotionEvent, lastEvent: MotionEvent): Boolean {
        return lastEvent.source != event.source ||
            lastEvent.getToolType(0) != event.getToolType(0)
    }

    private fun isDevicePressEvent(event: MotionEvent): Boolean {
        if (event.buttonState != 0) {
            return true
        }
        return when (event.actionMasked) {
            ACTION_POINTER_UP, // means that there is at least one remaining pointer
            ACTION_DOWN,
            ACTION_MOVE -> true
//            ACTION_SCROLL, // We've already checked for buttonState, so it must not be down
//            ACTION_HOVER_ENTER,
//            ACTION_HOVER_MOVE,
//            ACTION_HOVER_EXIT,
//            ACTION_UP,
//            ACTION_CANCEL,
            else -> false
        }
    }

    private fun sendMotionEvent(motionEvent: MotionEvent): ProcessResult {
        val pointerInputEvent =
            motionEventAdapter.convertToPointerInputEvent(motionEvent, this)
        return if (pointerInputEvent != null) {
            // Cache the last position of the last pointer to go down so we can check if
            // it's in a scrollable region in canScroll{Vertically|Horizontally}. Those
            // methods use semantics data, and because semantics coordinates are local to
            // this view, the pointer _position_, not _positionOnScreen_, is the offset that
            // needs to be cached.
            pointerInputEvent.pointers.lastOrNull { it.down }?.position?.let {
                lastDownPointerPosition = it
            }

            val result = pointerInputEventProcessor.process(
                pointerInputEvent,
                this,
                isInBounds(motionEvent)
            )
            val action = motionEvent.actionMasked
            if ((action == ACTION_DOWN || action == ACTION_POINTER_DOWN) &&
                !result.dispatchedToAPointerInputModifier
            ) {
                // We aren't handling the pointer, so the event stream has ended for us.
                // The next time we receive a pointer event, it should be considered a new
                // pointer.
                motionEventAdapter.endStream(motionEvent.getPointerId(motionEvent.actionIndex))
            }
            result
        } else {
            pointerInputEventProcessor.processCancel()
            ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false
            )
        }
    }

    private fun sendSimulatedEvent(
        motionEvent: MotionEvent,
        action: Int,
        eventTime: Long,
        forceHover: Boolean = true
    ) {
        val oldAction = motionEvent.actionMasked
        // don't send any events for pointers that are "up" unless they support hover
        val upIndex = when (oldAction) {
            ACTION_UP -> if (action == ACTION_HOVER_ENTER || action == ACTION_HOVER_EXIT) -1 else 0
            ACTION_POINTER_UP -> motionEvent.actionIndex
            else -> -1
        }
        val pointerCount = motionEvent.pointerCount - if (upIndex >= 0) 1 else 0
        if (pointerCount == 0) {
            return
        }
        val pointerProperties = Array(pointerCount) { MotionEvent.PointerProperties() }
        val pointerCoords = Array(pointerCount) { MotionEvent.PointerCoords() }
        for (i in 0 until pointerCount) {
            val sourceIndex = i + if (upIndex < 0 || i < upIndex) 0 else 1
            motionEvent.getPointerProperties(sourceIndex, pointerProperties[i])
            val coords = pointerCoords[i]
            motionEvent.getPointerCoords(sourceIndex, coords)
            val localPosition = Offset(coords.x, coords.y)
            val screenPosition = localToScreen(localPosition)
            coords.x = screenPosition.x
            coords.y = screenPosition.y
        }
        val buttonState = if (forceHover) 0 else motionEvent.buttonState

        val downTime = if (motionEvent.downTime == motionEvent.eventTime) {
            eventTime
        } else {
            motionEvent.downTime
        }
        val event = MotionEvent.obtain(
            /* downTime */ downTime,
            /* eventTime */ eventTime,
            /* action */ action,
            /* pointerCount */ pointerCount,
            /* pointerProperties */ pointerProperties,
            /* pointerCoords */ pointerCoords,
            /* metaState */ motionEvent.metaState,
            /* buttonState */ buttonState,
            /* xPrecision */ motionEvent.xPrecision,
            /* yPrecision */ motionEvent.yPrecision,
            /* deviceId */ motionEvent.deviceId,
            /* edgeFlags */ motionEvent.edgeFlags,
            /* source */ motionEvent.source,
            /* flags */ motionEvent.flags
        )
        val pointerInputEvent =
            motionEventAdapter.convertToPointerInputEvent(event, this)!!

        pointerInputEventProcessor.process(
            pointerInputEvent,
            this,
            true
        )
        event.recycle()
    }

    /**
     * This method is required to correctly support swipe-to-dismiss layouts on WearOS, which search
     * their children for scrollable views to determine whether or not to intercept touch events –
     * a sort of simplified nested scrolling mechanism.
     *
     * Because a composition may contain many scrollable and non-scrollable areas, and this method
     * doesn't know which part of the view the caller cares about, it uses the
     * [lastDownPointerPosition] as the location to check.
     */
    override fun canScrollHorizontally(direction: Int): Boolean =
        accessibilityDelegate.canScroll(vertical = false, direction, lastDownPointerPosition)

    /** See [canScrollHorizontally]. */
    override fun canScrollVertically(direction: Int): Boolean =
        accessibilityDelegate.canScroll(vertical = true, direction, lastDownPointerPosition)

    private fun isInBounds(motionEvent: MotionEvent): Boolean {
        val x = motionEvent.x
        val y = motionEvent.y
        return (x in 0f..width.toFloat() && y in 0f..height.toFloat())
    }

    override fun localToScreen(localPosition: Offset): Offset {
        recalculateWindowPosition()
        val local = viewToWindowMatrix.map(localPosition)
        return Offset(
            local.x + windowPosition.x,
            local.y + windowPosition.y
        )
    }

    override fun screenToLocal(positionOnScreen: Offset): Offset {
        recalculateWindowPosition()
        val x = positionOnScreen.x - windowPosition.x
        val y = positionOnScreen.y - windowPosition.y
        return windowToViewMatrix.map(Offset(x, y))
    }

    private fun recalculateWindowPosition() {
        if (!forceUseMatrixCache) {
            val animationTime = AnimationUtils.currentAnimationTimeMillis()
            if (animationTime != lastMatrixRecalculationAnimationTime) {
                lastMatrixRecalculationAnimationTime = animationTime
                recalculateWindowViewTransforms()
                var viewParent = parent
                var view: View = this
                while (viewParent is ViewGroup) {
                    view = viewParent
                    viewParent = view.parent
                }
                view.getLocationOnScreen(tmpPositionArray)
                val screenX = tmpPositionArray[0].toFloat()
                val screenY = tmpPositionArray[1].toFloat()
                view.getLocationInWindow(tmpPositionArray)
                val windowX = tmpPositionArray[0].toFloat()
                val windowY = tmpPositionArray[1].toFloat()
                windowPosition = Offset(screenX - windowX, screenY - windowY)
            }
        }
    }

    /**
     * Recalculates the window position based on the [motionEvent]'s coordinates and
     * screen coordinates. Some devices give false positions for [getLocationOnScreen] in
     * some unusual circumstances, so a different mechanism must be used to determine the
     * actual position.
     */
    private fun recalculateWindowPosition(motionEvent: MotionEvent) {
        lastMatrixRecalculationAnimationTime = AnimationUtils.currentAnimationTimeMillis()
        recalculateWindowViewTransforms()
        val positionInWindow = viewToWindowMatrix.map(Offset(motionEvent.x, motionEvent.y))

        windowPosition = Offset(
            motionEvent.rawX - positionInWindow.x,
            motionEvent.rawY - positionInWindow.y
        )
    }

    private fun recalculateWindowViewTransforms() {
        viewToWindowMatrix.reset()
        transformMatrixToWindow(this, viewToWindowMatrix)
        viewToWindowMatrix.invertTo(windowToViewMatrix)
    }

    override fun onCheckIsTextEditor(): Boolean = textInputServiceAndroid.isEditorFocused()

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        textInputServiceAndroid.createInputConnection(outAttrs)

    override fun calculateLocalPosition(positionInWindow: Offset): Offset {
        recalculateWindowPosition()
        return windowToViewMatrix.map(positionInWindow)
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset {
        recalculateWindowPosition()
        return viewToWindowMatrix.map(localPosition)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        density = Density(context)
        if (newConfig.fontWeightAdjustmentCompat != currentFontWeightAdjustment) {
            currentFontWeightAdjustment = newConfig.fontWeightAdjustmentCompat
            fontFamilyResolver = createFontFamilyResolver(context)
        }
        configurationChangeObserver(newConfig)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        // This method can be called while View's constructor is running
        // by way of resolving padding in response to initScrollbars.
        // If we get such a call, don't try to write to a property delegate
        // that hasn't been initialized yet.
        if (superclassInitComplete) {
            layoutDirectionFromInt(layoutDirection).let {
                this.layoutDirection = it
                _focusManager.layoutDirection = it
            }
        }
    }

    private fun autofillSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (hoverExitReceived) {
            // Go ahead and send it now
            removeCallbacks(sendHoverExitEvent)
            sendHoverExitEvent.run()
        }
        if (isBadMotionEvent(event)) {
            return false // Bad MotionEvent. Don't handle it.
        }
        if (event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN) &&
            event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
        ) {
            // Accessibility touch exploration
            return accessibilityDelegate.dispatchHoverEvent(event)
        }
        when (event.actionMasked) {
            ACTION_HOVER_EXIT -> {
                if (isInBounds(event)) {
                    if (event.getToolType(0) != TOOL_TYPE_MOUSE) {
                        // This may be caused by a press (e.g. stylus pressed on the screen), but
                        // we can't be sure until the ACTION_DOWN is received. Let's delay this
                        // message and see if the ACTION_DOWN comes.
                        previousMotionEvent?.recycle()
                        previousMotionEvent = MotionEvent.obtainNoHistory(event)
                        hoverExitReceived = true
                        post(sendHoverExitEvent)
                        return false
                    } else if (event.buttonState != 0) {
                        // We know that this is caused by a button press, so we can ignore it
                        return false
                    }
                }
            }
            ACTION_HOVER_MOVE ->
                // Check if we're receiving this when we've already handled it elsewhere
                if (!isPositionChanged(event)) {
                    return false
                }
        }
        val result = handleMotionEvent(event)
        return result.dispatchedToAPointerInputModifier
    }

    private fun isBadMotionEvent(event: MotionEvent): Boolean {
        return event.x.isNaN() ||
            event.y.isNaN() ||
            event.rawX.isNaN() ||
            event.rawY.isNaN()
    }

    private fun isPositionChanged(event: MotionEvent): Boolean {
        if (event.pointerCount != 1) {
            return true
        }
        val lastEvent = previousMotionEvent
        return lastEvent == null || event.rawX != lastEvent.rawX || event.rawY != lastEvent.rawY
    }

    private fun findViewByAccessibilityIdRootedAtCurrentView(
        accessibilityId: Int,
        currentView: View
    ): View? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val getAccessibilityViewIdMethod = View::class.java
                .getDeclaredMethod("getAccessibilityViewId")
            getAccessibilityViewIdMethod.isAccessible = true
            if (getAccessibilityViewIdMethod.invoke(currentView) == accessibilityId) {
                return currentView
            }
            if (currentView is ViewGroup) {
                for (i in 0 until currentView.childCount) {
                    val foundView = findViewByAccessibilityIdRootedAtCurrentView(
                        accessibilityId,
                        currentView.getChildAt(i)
                    )
                    if (foundView != null) {
                        return foundView
                    }
                }
            }
        }
        return null
    }

    private var desiredPointerIcon: PointerIcon? = null

    override val pointerIconService: PointerIconService =
        object : PointerIconService {
            override var current: PointerIcon
                get() = desiredPointerIcon ?: PointerIconDefaults.Default
                set(value) {
                    desiredPointerIcon = value
                }
        }

    /**
     * This overrides an @hide method in ViewGroup. Because of the @hide, the override keyword
     * cannot be used, but the override works anyway because the ViewGroup method is not final.
     * In Android P and earlier, the call path is
     * AccessibilityInteractionController#findViewByAccessibilityId ->
     * View#findViewByAccessibilityId -> ViewGroup#findViewByAccessibilityIdTraversal. In Android
     * Q and later, AccessibilityInteractionController#findViewByAccessibilityId uses
     * AccessibilityNodeIdManager and findViewByAccessibilityIdTraversal is only used by autofill.
     */
    @Suppress("unused")
    fun findViewByAccessibilityIdTraversal(accessibilityId: Int): View? {
        try {
            // AccessibilityInteractionController#findViewByAccessibilityId doesn't call this
            // method in Android Q and later. Ideally, we should only define this method in
            // Android P and earlier, but since we don't have a way to do so, we can simply
            // invoke the hidden parent method after Android P. If in new android, the hidden method
            // ViewGroup#findViewByAccessibilityIdTraversal signature is changed or removed, we can
            // simply return null here because there will be no call to this method.
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val findViewByAccessibilityIdTraversalMethod = View::class.java
                    .getDeclaredMethod("findViewByAccessibilityIdTraversal", Int::class.java)
                findViewByAccessibilityIdTraversalMethod.isAccessible = true
                findViewByAccessibilityIdTraversalMethod.invoke(this, accessibilityId) as? View
            } else {
                findViewByAccessibilityIdRootedAtCurrentView(accessibilityId, this)
            }
        } catch (e: NoSuchMethodException) {
            return null
        }
    }

    override val isLifecycleInResumedState: Boolean
        get() = viewTreeOwners?.lifecycleOwner
            ?.lifecycle?.currentState == Lifecycle.State.RESUMED

    private fun transformMatrixToWindow(view: View, matrix: Matrix) {
        val parentView = view.parent
        if (parentView is View) {
            transformMatrixToWindow(parentView, matrix)
            matrix.preTranslate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            matrix.preTranslate(view.left.toFloat(), view.top.toFloat())
        } else {
            view.getLocationInWindow(tmpPositionArray)
            matrix.preTranslate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            matrix.preTranslate(tmpPositionArray[0].toFloat(), tmpPositionArray[1].toFloat())
        }

        val viewMatrix = view.matrix
        if (!viewMatrix.isIdentity) {
            matrix.preConcat(viewMatrix)
        }
    }

    /**
     * Like [android.graphics.Matrix.preConcat], for a Compose [Matrix] that accepts an [other]
     * [android.graphics.Matrix].
     */
    private fun Matrix.preConcat(other: android.graphics.Matrix) {
        tmpCalculationMatrix.setFrom(other)
        preTransform(tmpCalculationMatrix)
    }

    /**
     * Like [android.graphics.Matrix.preTranslate], for a Compose [Matrix]
     */
    private fun Matrix.preTranslate(x: Float, y: Float) {
        tmpCalculationMatrix.reset()
        tmpCalculationMatrix.translate(x, y)
        preTransform(tmpCalculationMatrix)
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    companion object {
        private const val FocusTag = "Compose Focus"
        private const val MaximumLayerCacheSize = 10
        private var systemPropertiesClass: Class<*>? = null
        private var getBooleanMethod: Method? = null

        // TODO(mount): replace with ViewCompat.isShowingLayoutBounds() when it becomes available.
        @SuppressLint("PrivateApi", "BanUncheckedReflection")
        private fun getIsShowingLayoutBounds(): Boolean = try {
            if (systemPropertiesClass == null) {
                systemPropertiesClass = Class.forName("android.os.SystemProperties")
                getBooleanMethod = systemPropertiesClass?.getDeclaredMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.java
                )
            }
            getBooleanMethod?.invoke(null, "debug.layout", false) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Combines objects populated via ViewTree*Owner
     */
    class ViewTreeOwners(
        /**
         * The [LifecycleOwner] associated with this owner.
         */
        val lifecycleOwner: LifecycleOwner,
        /**
         * The [SavedStateRegistryOwner] associated with this owner.
         */
        val savedStateRegistryOwner: SavedStateRegistryOwner
    )
}

/**
 * Return the layout direction set by the [Locale][java.util.Locale].
 *
 * A convenience getter that translates [Configuration.getLayoutDirection] result into
 * [LayoutDirection] instance.
 */
internal val Configuration.localeLayoutDirection: LayoutDirection
    // We don't use the attached View's layout direction here since that layout direction may not
    // be resolved since the composables may be composed without attaching to the RootViewImpl.
    // In Jetpack Compose, use the locale layout direction (i.e. layoutDirection came from
    // configuration) as a default layout direction.
    get() = layoutDirectionFromInt(layoutDirection)

private fun layoutDirectionFromInt(layoutDirection: Int): LayoutDirection = when (layoutDirection) {
    android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
    android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
    else -> LayoutDirection.Ltr
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalComposeUiApi // used by testing infra
var textInputServiceFactory: (PlatformTextInputService) -> TextInputService =
    { TextInputService(it) }

/**
 * These classes are here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.O)
private object AndroidComposeViewVerificationHelperMethodsO {
    @RequiresApi(Build.VERSION_CODES.O)
    @DoNotInline
    fun focusable(view: View, focusable: Int, defaultFocusHighlightEnabled: Boolean) {
        view.focusable = focusable
        // not to add the default focus highlight to the whole compose view
        view.defaultFocusHighlightEnabled = defaultFocusHighlightEnabled
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private object AndroidComposeViewVerificationHelperMethodsN {
    @DoNotInline
    @RequiresApi(Build.VERSION_CODES.N)
    fun setPointerIcon(view: View, icon: PointerIcon?) {
        val iconToSet = when (icon) {
            is AndroidPointerIcon ->
                icon.pointerIcon
            is AndroidPointerIconType ->
                android.view.PointerIcon.getSystemIcon(view.context, icon.type)
            else ->
                android.view.PointerIcon.getSystemIcon(
                    view.context,
                    android.view.PointerIcon.TYPE_DEFAULT
                )
        }

        if (view.pointerIcon != iconToSet) {
            view.pointerIcon = iconToSet
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private object AndroidComposeViewForceDarkModeQ {
    @DoNotInline
    @RequiresApi(Build.VERSION_CODES.Q)
    fun disallowForceDark(view: View) {
        view.isForceDarkAllowed = false
    }
}

/**
 * Sets this [Matrix] to be the result of this * [other]
 */
private fun Matrix.preTransform(other: Matrix) {
    val v00 = dot(other, 0, this, 0)
    val v01 = dot(other, 0, this, 1)
    val v02 = dot(other, 0, this, 2)
    val v03 = dot(other, 0, this, 3)
    val v10 = dot(other, 1, this, 0)
    val v11 = dot(other, 1, this, 1)
    val v12 = dot(other, 1, this, 2)
    val v13 = dot(other, 1, this, 3)
    val v20 = dot(other, 2, this, 0)
    val v21 = dot(other, 2, this, 1)
    val v22 = dot(other, 2, this, 2)
    val v23 = dot(other, 2, this, 3)
    val v30 = dot(other, 3, this, 0)
    val v31 = dot(other, 3, this, 1)
    val v32 = dot(other, 3, this, 2)
    val v33 = dot(other, 3, this, 3)
    this[0, 0] = v00
    this[0, 1] = v01
    this[0, 2] = v02
    this[0, 3] = v03
    this[1, 0] = v10
    this[1, 1] = v11
    this[1, 2] = v12
    this[1, 3] = v13
    this[2, 0] = v20
    this[2, 1] = v21
    this[2, 2] = v22
    this[2, 3] = v23
    this[3, 0] = v30
    this[3, 1] = v31
    this[3, 2] = v32
    this[3, 3] = v33
}

// Taken from Matrix.kt
private fun dot(m1: Matrix, row: Int, m2: Matrix, column: Int): Float {
    return m1[row, 0] * m2[0, column] +
        m1[row, 1] * m2[1, column] +
        m1[row, 2] * m2[2, column] +
        m1[row, 3] * m2[3, column]
}
