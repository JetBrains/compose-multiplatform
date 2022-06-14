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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AccessibilityController
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.PlatformInput
import androidx.compose.ui.platform.DummyPlatformComponent
import androidx.compose.ui.platform.FlushCoroutineDispatcher
import androidx.compose.ui.platform.GlobalSnapshotManager
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Volatile

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
 */
class ComposeScene internal constructor(
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    internal val component: PlatformComponent,
    density: Density = Density(1f),
    private val invalidate: () -> Unit = {}
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
        DummyPlatformComponent,
        density,
        invalidate
    )

    private var isInvalidationDisabled = false

    @Volatile
    private var hasPendingDraws = true
    private inline fun <T> postponeInvalidation(block: () -> T): T {
        isInvalidationDisabled = true
        val result = try {
            block()
        } finally {
            isInvalidationDisabled = false
        }
        invalidateIfNeeded()
        return result
    }

    private fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters || list.any(SkiaBasedOwner::needRender)
        if (hasPendingDraws && !isInvalidationDisabled && !isClosed) {
            invalidate()
        }
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

    private var pointerId = 0L
    private var isMousePressed = false

    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)
    // We use FlushCoroutineDispatcher for effectDispatcher not because we need `flush` for
    // LaunchEffect tasks, but because we need to know if it is idle (hasn't scheduled tasks)
    private val effectDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposeDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)

    private val recomposer = Recomposer(coroutineContext + job + effectDispatcher)

    internal val platformInputService: PlatformInput = PlatformInput(component)

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

    private fun dispatchCommand(command: () -> Unit) {
        coroutineScope.launch {
            command()
        }
    }

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
        owner.onNeedRender = ::invalidateIfNeeded
        owner.onDispatchCommand = ::dispatchCommand
        owner.constraints = constraints
        owner.accessibilityController = makeAccessibilityController(
            owner,
            component
        )
        invalidateIfNeeded()
        if (owner.isFocusable) {
            focusedOwner = owner
        }
    }

    internal fun detach(owner: SkiaBasedOwner) {
        check(!isClosed) { "ComposeScene is closed" }
        list.remove(owner)
        owner.onDispatchCommand = null
        owner.onNeedRender = null
        invalidateIfNeeded()
        if (owner == focusedOwner) {
            focusedOwner = list.lastOrNull { it.isFocusable }
        }
    }

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
        composition?.dispose()
        mainOwner?.dispose()
        val mainOwner = SkiaBasedOwner(
            platformInputService,
            component,
            density,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        attach(mainOwner)
        composition = mainOwner.setContent(parentComposition ?: recomposer) {
            CompositionLocalProvider(
                LocalComposeScene provides this,
                content = content
            )
        }
        this.mainOwner = mainOwner

        // to perform all pending work synchronously. to start LaunchedEffect for example
        recomposeDispatcher.flush()
    }

    /**
     * Set constraints, which will be used to measure and layout content.
     */
    var constraints: Constraints = Constraints()
        set(value) {
            field = value
            forEachOwner {
                it.constraints = constraints
            }
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
    fun render(canvas: Canvas, nanoTime: Long) {
        check(!isClosed) { "ComposeScene is closed" }
        postponeInvalidation {
            // We must see the actual state before we will render the frame
            Snapshot.sendApplyNotifications()
            recomposeDispatcher.flush()
            frameClock.sendFrame(nanoTime)

            forEachOwner {
                it.render(canvas)
            }
        }
    }

    // for TestComposeWindow backward compatibility
    internal fun flushEffects() {
        effectDispatcher.flush()
    }

    private var focusedOwner: SkiaBasedOwner? = null
    private val hoveredOwner: SkiaBasedOwner?
        get() = list.lastOrNull { it.isHovered(pointLocation) } ?: list.lastOrNull()

    private fun SkiaBasedOwner?.isAbove(
        targetOwner: SkiaBasedOwner?
    ) = list.indexOf(this) > list.indexOf(targetOwner)

    // TODO(demin): return Boolean (when it is consumed).
    //  see ComposeLayer todo about AWTDebounceEventQueue
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
        timeMillis: Long = currentMillis(),
        type: PointerType = PointerType.Mouse,
        buttons: PointerButtons? = null,
        keyboardModifiers: PointerKeyboardModifiers? = null,
        nativeEvent: Any? = null,
    ): Unit = postponeInvalidation {
        check(!isClosed) { "ComposeScene is closed" }
        defaultPointerStateTracker.onPointerEvent(eventType)

        val actualButtons = buttons ?: defaultPointerStateTracker.buttons
        val actualKeyboardModifiers =
            keyboardModifiers ?: defaultPointerStateTracker.keyboardModifiers

        when (eventType) {
            PointerEventType.Press -> isMousePressed = true
            PointerEventType.Release -> isMousePressed = false
        }
        val event = pointerInputEvent(
            eventType,
            position,
            timeMillis,
            nativeEvent,
            type,
            isMousePressed,
            pointerId,
            scrollDelta,
            actualButtons,
            actualKeyboardModifiers
        )
        when (eventType) {
            PointerEventType.Press -> onMousePressed(event)
            PointerEventType.Release -> onMouseReleased(event)
            PointerEventType.Move -> {
                pointLocation = position
                hoveredOwner?.processPointerInput(event)
            }
            PointerEventType.Enter -> hoveredOwner?.processPointerInput(event)
            PointerEventType.Exit -> hoveredOwner?.processPointerInput(event)
            PointerEventType.Scroll -> hoveredOwner?.processPointerInput(event)
        }
    }

    private fun onMousePressed(event: PointerInputEvent) {
        val currentOwner = hoveredOwner
        if (currentOwner != null) {
            if (focusedOwner.isAbove(currentOwner)) {
                focusedOwner?.onDismissRequest?.invoke()
            } else {
                currentOwner.processPointerInput(event)
            }
        } else {
            focusedOwner?.processPointerInput(event)
        }
    }

    private fun onMouseReleased(event: PointerInputEvent) {
        val owner = hoveredOwner ?: focusedOwner
        owner?.processPointerInput(event)
        pointerId += 1
    }

    private var pointLocation = Offset.Zero

    /**
     * Send [KeyEvent] to the content.
     * @return true if the event was consumed by the content
     */
    fun sendKeyEvent(event: ComposeKeyEvent): Boolean = postponeInvalidation {
        return focusedOwner?.sendKeyEvent(event) == true
    }

    internal fun setCurrentKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
        mainOwner?.setCurrentKeyboardModifiers(modifiers)
    }

    internal fun onInputMethodEvent(event: Any) = this.onPlatformInputMethodEvent(event)
}

private class DefaultPointerStateTracker {
    fun onPointerEvent(eventType: PointerEventType) {
        when (eventType) {
            PointerEventType.Press -> buttons = PrimaryPressedPointerButtons
            PointerEventType.Release -> buttons = DefaultPointerButtons
        }
    }

    var buttons = DefaultPointerButtons
        private set

    var keyboardModifiers = DefaultPointerKeyboardModifiers
        private set
}

internal expect fun ComposeScene.onPlatformInputMethodEvent(event: Any)

internal expect fun pointerInputEvent(
    eventType: PointerEventType,
    position: Offset,
    timeMillis: Long,
    nativeEvent: Any?,
    type: PointerType,
    isMousePressed: Boolean,
    pointerId: Long,
    scrollDelta: Offset,
    buttons: PointerButtons,
    keyboardModifiers: PointerKeyboardModifiers,
): PointerInputEvent

internal expect val DefaultPointerButtons: PointerButtons
internal expect val DefaultPointerKeyboardModifiers: PointerKeyboardModifiers
internal expect val PrimaryPressedPointerButtons: PointerButtons

internal expect fun makeAccessibilityController(
    skiaBasedOwner: SkiaBasedOwner,
    component: PlatformComponent
): AccessibilityController

internal expect fun currentMillis(): Long
