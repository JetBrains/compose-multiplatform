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
package androidx.compose.ui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AccessibilityController
import androidx.compose.ui.platform.Platform
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.FlushCoroutineDispatcher
import androidx.compose.ui.platform.GlobalSnapshotManager
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.synchronized
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.currentNanoTime
import org.jetbrains.skiko.SkiaLayer

internal val LocalComposeScene = staticCompositionLocalOf<ComposeScene> {
    error("CompositionLocal LocalComposeScene not provided")
}

/**
 * A virtual container that encapsulates Compose UI content. UI content can be constructed via
 * [setContent] method and with any Composable that manipulates [LayoutNode] tree.
 *
 * To draw content on [Canvas], you can use [render] method.
 *
 * To specify available size for the content, you should use [constraints].
 *
 * After [ComposeScene] will no longer needed, you should call [close] method, so all resources
 * and subscriptions will be properly closed. Otherwise there can be a memory leak.
 *
 * [ComposeScene] doesn't support concurrent read/write access from different threads. Except:
 * - [hasInvalidations] can be called from any thread
 * - [invalidate] callback can be called from any thread
 */
class ComposeScene internal constructor(
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    internal val platform: Platform,
    density: Density = Density(1f),
    private val invalidate: () -> Unit = {},
    @Deprecated("Will be removed in Compose 1.3")
    internal val createSyntheticNativeMoveEvent:
        (sourceEvent: Any?, positionSourceEvent: Any?) -> Any? = { _, _ -> null },
) {
    /**
     * Constructs [ComposeScene]
     *
     * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
     * [rememberCoroutineScope]) and run recompositions.
     * @param density Initial density of the content which will be used to convert [dp] units.
     * @param invalidate Callback which will be called when the content need to be recomposed or
     * rerendered. If you draw your content using [render] method, in this callback you should
     * schedule the next [render] in your rendering loop.
     */
    constructor(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        density: Density = Density(1f),
        invalidate: () -> Unit = {}
    ) : this(
        coroutineContext,
        Platform.Empty,
        density,
        invalidate
    )

    private var isInvalidationDisabled = false

    @Volatile
    private var hasPendingDraws = true
    private inline fun <T> postponeInvalidation(block: () -> T): T {
        check(!isClosed) { "ComposeScene is closed" }
        isInvalidationDisabled = true
        val result = try {
            // We must see the actual state before we will do [block]
            // TODO(https://github.com/JetBrains/compose-jb/issues/1854) get rid of synchronized.
            synchronized(GlobalSnapshotManager.sync) {
                Snapshot.sendApplyNotifications()
            }
            snapshotChanges.perform()
            block()
        } finally {
            isInvalidationDisabled = false
        }
        invalidateIfNeeded()
        return result
    }

    private fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters || needLayout || needDraw ||
           snapshotChanges.hasCommands || pointerPositionUpdater.needUpdate
        if (hasPendingDraws && !isInvalidationDisabled && !isClosed) {
            invalidate()
        }
    }

    private var needLayout = true
    private var needDraw = true

    private fun requestLayout() {
        needLayout = true
        invalidateIfNeeded()
    }

    private fun requestDraw() {
        needDraw = true
        invalidateIfNeeded()
    }

    private val list = LinkedHashSet<SkiaBasedOwner>()
    private val listCopy = mutableListOf<SkiaBasedOwner>()

    private inline fun forEachOwner(action: (SkiaBasedOwner) -> Unit) {
        listCopy.addAll(list)
        listCopy.forEach(action)
        listCopy.clear()
    }

    /**
     * All currently registered [RootForTest]s. After calling [setContent] the first root
     * will be added. If there is an any [Popup] is present in the content, it will be added as
     * another [RootForTest]
     */
    val roots: Set<RootForTest> get() = list

    private val defaultPointerStateTracker = DefaultPointerStateTracker()

    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)
    // We use FlushCoroutineDispatcher for effectDispatcher not because we need `flush` for
    // LaunchEffect tasks, but because we need to know if it is idle (hasn't scheduled tasks)
    private val effectDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposeDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)

    private val recomposer = Recomposer(coroutineContext + job + effectDispatcher)

    internal val pointerPositionUpdater = PointerPositionUpdater(::invalidateIfNeeded, ::sendAsMove)

    internal var mainOwner: SkiaBasedOwner? = null
    private var composition: Composition? = null

    /**
     * Density of the content which will be used to convert [dp] units.
     */
    var density: Density = density
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner?.density = value
        }

    private var isClosed = false

    init {
        GlobalSnapshotManager.ensureStarted()
        coroutineScope.launch(
            recomposeDispatcher + frameClock,
            start = CoroutineStart.UNDISPATCHED
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    /**
     * Close all resources and subscriptions. Not calling this method when [ComposeScene] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [ComposeScene].
     */
    fun close() {
        composition?.dispose()
        mainOwner?.dispose()
        recomposer.cancel()
        job.cancel()
        isClosed = true
    }

    private val snapshotChanges = CommandList(::invalidateIfNeeded)

    /**
     * Returns true if there are pending recompositions, renders or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations() = hasPendingDraws ||
        recomposer.hasPendingWork ||
        effectDispatcher.hasTasks() ||
        recomposeDispatcher.hasTasks()

    internal fun attach(owner: SkiaBasedOwner) {
        check(!isClosed) { "ComposeScene is closed" }
        list.add(owner)
        owner.requestLayout = ::requestLayout
        owner.requestDraw = ::requestDraw
        owner.dispatchSnapshotChanges = snapshotChanges::add
        owner.constraints = constraints
        invalidateIfNeeded()
        if (owner.isFocusable) {
            focusedOwner = owner
        }
        if (isFocused) {
            owner.focusManager.takeFocus()
        } else {
            owner.focusManager.releaseFocus()
        }
    }

    internal fun detach(owner: SkiaBasedOwner) {
        check(!isClosed) { "ComposeScene is closed" }
        list.remove(owner)
        owner.dispatchSnapshotChanges = null
        owner.requestDraw = null
        owner.requestLayout = null
        invalidateIfNeeded()
        if (owner == focusedOwner) {
            focusedOwner = list.lastOrNull { it.isFocusable }
        }
        if (owner == lastMoveOwner) {
            lastMoveOwner = null
        }
        if (owner == pressOwner) {
            pressOwner = null
        }
    }

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext? by mutableStateOf(null)

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(
        content: @Composable () -> Unit
    ) = setContent(
        parentComposition = null,
        content = content
    )

    // TODO(demin): We should configure routing of key events if there
    //  are any popups/root present:
    //   - ComposeScene.sendKeyEvent
    //   - ComposeScene.onPreviewKeyEvent (or Window.onPreviewKeyEvent)
    //   - Popup.onPreviewKeyEvent
    //   - NestedPopup.onPreviewKeyEvent
    //   - NestedPopup.onKeyEvent
    //   - Popup.onKeyEvent
    //   - ComposeScene.onKeyEvent
    //  Currently we have this routing:
    //   - [active Popup or the main content].onPreviewKeyEvent
    //   - [active Popup or the main content].onKeyEvent
    //   After we change routing, we can remove onPreviewKeyEvent/onKeyEvent from this method
    internal fun setContent(
        parentComposition: CompositionContext? = null,
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        check(!isClosed) { "ComposeScene is closed" }
        pointerPositionUpdater.reset()
        composition?.dispose()
        mainOwner?.dispose()
        val mainOwner = SkiaBasedOwner(
            platform,
            pointerPositionUpdater,
            density,
            IntSize(constraints.maxWidth, constraints.maxHeight).toIntRect(),
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        attach(mainOwner)
        composition = mainOwner.setContent(
            parentComposition ?: recomposer,
            { compositionLocalContext }
        ) {
            CompositionLocalProvider(
                LocalComposeScene provides this,
                content = content
            )
        }
        this.mainOwner = mainOwner

        // to perform all pending work synchronously
        recomposeDispatcher.flush()
    }

    /**
     * Constraints used to measure and layout content.
     */
    var constraints: Constraints = Constraints()
        set(value) {
            field = value
            forEachOwner {
                it.constraints = constraints
            }
            mainOwner?.bounds = IntSize(constraints.maxWidth, constraints.maxHeight).toIntRect()
        }

    /**
     * Returns the current content size
     */
    val contentSize: IntSize
        get() {
            check(!isClosed) { "ComposeScene is closed" }
            val mainOwner = mainOwner ?: return IntSize.Zero
            mainOwner.measureAndLayout()
            return mainOwner.contentSize
        }

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(canvas: Canvas, nanoTime: Long): Unit = postponeInvalidation {
        recomposeDispatcher.flush()
        frameClock.sendFrame(nanoTime)
        needLayout = false
        forEachOwner { it.measureAndLayout() }
        pointerPositionUpdater.update()
        needDraw = false
        forEachOwner { it.draw(canvas) }
        forEachOwner { it.clearInvalidObservations() }
    }

    private var focusedOwner: SkiaBasedOwner? = null
    private var pressOwner: SkiaBasedOwner? = null
    private var lastMoveOwner: SkiaBasedOwner? = null
    private fun hoveredOwner(event: PointerInputEvent): SkiaBasedOwner? =
        list.lastOrNull { it.isHovered(event.pointers.first().position) }

    private fun SkiaBasedOwner?.isAbove(
        targetOwner: SkiaBasedOwner?
    ) = this != null && targetOwner != null && list.indexOf(this) > list.indexOf(targetOwner)

    // TODO(demin): return Boolean (when it is consumed)
    /**
     * Send pointer event to the content.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param position The [Offset] of the current pointer event, relative to the content.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons).
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param nativeEvent The original native event.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = (currentNanoTime() / 1E6).toLong(),
        type: PointerType = PointerType.Mouse,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
    ): Unit = postponeInvalidation {
        defaultPointerStateTracker.onPointerEvent(eventType)

        val actualButtons = buttons ?: defaultPointerStateTracker.buttons
        val actualKeyboardModifiers =
            keyboardModifiers ?: defaultPointerStateTracker.keyboardModifiers

        val event = pointerInputEvent(
            eventType,
            position,
            timeMillis,
            nativeEvent,
            type,
            scrollDelta,
            actualButtons,
            actualKeyboardModifiers
        )
        needLayout = false
        forEachOwner { it.measureAndLayout() }
        pointerPositionUpdater.beforeEvent(event)
        processPointerInput(event)
    }

    @Suppress("DEPRECATION")
    private fun sendAsMove(sourceEvent: PointerInputEvent, positionSourceEvent: PointerInputEvent) {
        val nativeEvent = createSyntheticNativeMoveEvent(
            sourceEvent.nativeEvent,
            positionSourceEvent.nativeEvent
        )
        processPointerInput(createMoveEvent(nativeEvent, sourceEvent, positionSourceEvent))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun processPointerInput(event: PointerInputEvent) {
        when (event.eventType) {
            PointerEventType.Press -> processPress(event)
            PointerEventType.Release -> processRelease(event)
            PointerEventType.Move -> processMove(event)
            PointerEventType.Enter -> processMove(event)
            PointerEventType.Exit -> processMove(event)
            PointerEventType.Scroll -> processScroll(event)
        }

        if (!event.buttons.areAnyPressed) {
            pressOwner = null
        }
    }

    private fun processPress(event: PointerInputEvent) {
        val owner = hoveredOwner(event)
        if (focusedOwner.isAbove(owner)) {
            focusedOwner?.onDismissRequest?.invoke()
        } else {
            owner?.processPointerInput(event)
            pressOwner = owner
        }
    }

    private fun processRelease(event: PointerInputEvent) {
        val owner = pressOwner ?: hoveredOwner(event)
        owner?.processPointerInput(event)
    }

    private fun processMove(event: PointerInputEvent) {
        val owner = when {
            event.buttons.areAnyPressed -> pressOwner
            event.eventType == PointerEventType.Exit -> null
            else -> hoveredOwner(event)
        }

        // Cases:
        // - move from outside to the window (owner != null, lastMoveOwner == null): Enter
        // - move from the window to outside (owner == null, lastMoveOwner != null): Exit
        // - move from one point of the window to another (owner == lastMoveOwner): Move
        // - move from one popup to another (owner != lastMoveOwner): [Popup 1] Exit, [Popup 2] Enter

        if (owner != lastMoveOwner) {
            lastMoveOwner?.processPointerInput(
                event.copy(eventType = PointerEventType.Exit),
                isInBounds = false
            )
            owner?.processPointerInput(
                event.copy(eventType = PointerEventType.Enter)
            )
        } else {
            owner?.processPointerInput(
                event.copy(eventType = PointerEventType.Move)
            )
        }

        lastMoveOwner = owner
    }

    private fun processScroll(event: PointerInputEvent) {
        val owner = hoveredOwner(event)
        if (!focusedOwner.isAbove(owner)) {
            owner?.processPointerInput(event)
        }
    }

    /**
     * Send [KeyEvent] to the content.
     * @return true if the event was consumed by the content
     */
    fun sendKeyEvent(event: ComposeKeyEvent): Boolean = postponeInvalidation {
        return focusedOwner?.sendKeyEvent(event) == true
    }

    private var isFocused = true

    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     */
    @ExperimentalComposeUiApi
    fun releaseFocus() {
        list.forEach {
            it.focusManager.releaseFocus()
        }
        isFocused = false
    }

    @ExperimentalComposeUiApi
    fun requestFocus() {
        list.forEach {
            it.focusManager.takeFocus()
        }
        isFocused = true
    }

    /**
     * Moves focus in the specified [direction][FocusDirection].
     *
     * If you are not satisfied with the default focus order, consider setting a custom order using
     * [Modifier.focusProperties()][focusProperties].
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    @ExperimentalComposeUiApi
    fun moveFocus(focusDirection: FocusDirection): Boolean =
        list.lastOrNull()?.focusManager?.moveFocus(focusDirection) ?: false
}

private class DefaultPointerStateTracker {
    fun onPointerEvent(eventType: PointerEventType) {
        when (eventType) {
            PointerEventType.Press -> buttons = PointerButtons(isPrimaryPressed = true)
            PointerEventType.Release -> buttons = PointerButtons()
        }
    }

    var buttons = PointerButtons()
        private set

    var keyboardModifiers = PointerKeyboardModifiers()
        private set
}

private fun pointerInputEvent(
    eventType: PointerEventType,
    position: Offset,
    timeMillis: Long,
    nativeEvent: Any?,
    type: PointerType,
    scrollDelta: Offset,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers
): PointerInputEvent {
    return PointerInputEvent(
        eventType,
        timeMillis,
        listOf(
            PointerInputEventData(
                PointerId(0),
                timeMillis,
                position,
                position,
                buttons.areAnyPressed,
                type,
                scrollDelta = scrollDelta
            )
        ),
        buttons,
        keyboardModifiers,
        nativeEvent
    )
}

private fun createMoveEvent(
    nativeEvent: Any?,
    sourceEvent: PointerInputEvent,
    positionSourceEvent: PointerInputEvent
) = pointerInputEvent(
    eventType = PointerEventType.Move,
    position = positionSourceEvent.pointers.first().position,
    timeMillis = sourceEvent.uptime,
    nativeEvent = nativeEvent,
    type = sourceEvent.pointers.first().type,
    scrollDelta = Offset(0f, 0f),
    buttons = sourceEvent.buttons,
    keyboardModifiers = sourceEvent.keyboardModifiers
)

internal expect fun createSkiaLayer(): SkiaLayer