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
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.PlatformInput
import androidx.compose.ui.platform.SkiaRootForTest
import androidx.compose.ui.platform.DummyPlatformComponent
import androidx.compose.ui.platform.EmptyDispatcher
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Volatile

internal val LocalComposeScene = staticCompositionLocalOf<ComposeScene> {
    error("CompositionLocal LocalComposeScene not provided")
}

/**
 * Virtual container that encapsulates Compose UI content. UI content can be constructed via
 * [setContent] method and with any Composable that manipulates [LayoutNode] tree.
 *
 * To draw content on [Canvas], you can use [render] method.
 *
 * To specify available size for the content, you should use [constraints].
 *
 * After [ComposeScene] will no longer needed, you should call [dispose] method, so all resources
 * and subscriptions will be properly closed. Otherwise there can be a memory leak.
 */
class ComposeScene internal constructor(
    coroutineContext: CoroutineContext,
    private val component: PlatformComponent,
    density: Density,
    private val invalidate: () -> Unit
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
        coroutineContext: CoroutineContext = EmptyDispatcher,
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
        if (hasPendingDraws && !isInvalidationDisabled) {
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
     * All currently registered [SkiaRootForTest]s. After calling [setContent] the first root
     * will be added. If there is an any Popup is present in the content, it will be added as
     * another [SkiaRootForTest]
     */
    val roots: Set<RootForTest> get() = list

    private var pointerId = 0L
    private var isMousePressed = false

    private val job = Job()
    private val dispatcher = FlushCoroutineDispatcher(CoroutineScope(coroutineContext + job))
    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)
    private val coroutineScope = CoroutineScope(coroutineContext + job + dispatcher + frameClock)

    private val recomposer = Recomposer(coroutineScope.coroutineContext)
    internal val platformInputService: PlatformInput = PlatformInput(component)

    private var mainOwner: SkiaBasedOwner? = null
    private var composition: Composition? = null

    /**
     * Density of the content which will be used to convert [dp] units.
     */
    var density: Density = density
        set(value) {
            check(!isDisposed) { "ComposeScene is disposed" }
            field = value
            mainOwner?.density = value
        }

    private var isDisposed = false

    init {
        GlobalSnapshotManager.ensureStarted()
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
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
    fun dispose() {
        composition?.dispose()
        mainOwner?.dispose()
        recomposer.cancel()
        job.cancel()
        isDisposed = true
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
        dispatcher.hasTasks()

    internal fun attach(skiaBasedOwner: SkiaBasedOwner) {
        check(!isDisposed) { "ComposeScene is disposed" }
        list.add(skiaBasedOwner)
        skiaBasedOwner.onNeedRender = ::invalidateIfNeeded
        skiaBasedOwner.onDispatchCommand = ::dispatchCommand
        skiaBasedOwner.constraints = constraints
        skiaBasedOwner.containerCursor = component
        invalidateIfNeeded()
        if (skiaBasedOwner.isFocusable) {
            focusedOwner = skiaBasedOwner
        }
    }

    internal fun detach(skiaBasedOwner: SkiaBasedOwner) {
        check(!isDisposed) { "ComposeScene is disposed" }
        list.remove(skiaBasedOwner)
        skiaBasedOwner.onDispatchCommand = null
        skiaBasedOwner.onNeedRender = null
        invalidateIfNeeded()
        if (skiaBasedOwner == focusedOwner) {
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
        check(!isDisposed) { "ComposeScene is disposed" }
        composition?.dispose()
        mainOwner?.dispose()
        val mainOwner = SkiaBasedOwner(
            platformInputService,
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
        dispatcher.flush()
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
            check(!isDisposed) { "ComposeScene is disposed" }
            val mainOwner = mainOwner ?: return IntSize.Zero
            mainOwner.measureAndLayout()
            return IntSize(mainOwner.root.width, mainOwner.root.height)
        }

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(canvas: Canvas, nanoTime: Long) {
        check(!isDisposed) { "ComposeScene is disposed" }
        postponeInvalidation {
            // TODO(https://github.com/JetBrains/compose-jb/issues/1135):
            //  Temporarily workaround for flaky tests in WithComposeUiTest.
            //  It fails when we remove synchronized and run:
            //  ./gradlew desktopTest -Pandroidx.compose.multiplatformEnabled=true
            //  We should make a minimal reproducer, and fix race condition somewhere
            //  else, not here.
            //  See also GlobalSnapshotManager.
            synchronized(Snapshot.current) {
                // We must see the actual state before we will render the frame
                Snapshot.sendApplyNotifications()
                dispatcher.flush()
                frameClock.sendFrame(nanoTime)
            }

            forEachOwner {
                it.render(canvas)
            }
        }
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
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     * @param nativeEvent The original native event.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        timeMillis: Long = System.nanoTime() / 1_000_000L,
        type: PointerType = PointerType.Mouse,
        nativeEvent: Any? = null,
        // TODO(demin): support PointerButtons, PointerKeyboardModifiers
//        buttons: PointerButtons? = null,
//        keyboardModifiers: PointerKeyboardModifiers? = null,
    ): Unit = postponeInvalidation {
        check(!isDisposed) { "ComposeScene is disposed" }
        when (eventType) {
            PointerEventType.Press -> isMousePressed = true
            PointerEventType.Release -> isMousePressed = false
        }
        val event = pointerInputEvent(
            eventType, position, timeMillis, nativeEvent, type, isMousePressed, pointerId
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
        }
    }

    // TODO(demin): remove/change when we will have scroll event support in the common code
    // TODO(demin): return Boolean (when it is consumed).
    //  see ComposeLayer todo about AWTDebounceEventQueue
    /**
     * Send pointer scroll event to the content.
     *
     * @param position The [Offset] of the current pointer event, relative to the content
     * @param delta Change of mouse scroll.
     * Positive if scrolling down, negative if scrolling up.
     * @param orientation Orientation in which scrolling event occurs.
     * Up/down wheel scrolling causes events in vertical orientation.
     * Left/right wheel scrolling causes events in horizontal orientation.
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     * @param nativeEvent The original native event
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Suppress("UNUSED_PARAMETER")
    @ExperimentalComposeUiApi // it is more experimental than ComposeScene itself
    fun sendPointerScrollEvent(
        position: Offset,
        delta: MouseScrollUnit,
        orientation: MouseScrollOrientation = MouseScrollOrientation.Vertical,
        timeMillis: Long = System.nanoTime() / 1_000_000L,
        type: PointerType = PointerType.Mouse,
        nativeEvent: Any? = null,
//        buttons: PointerButtons? = null,
//        keyboardModifiers: PointerKeyboardModifiers? = null,
    ): Unit = postponeInvalidation {
        check(!isDisposed) { "ComposeScene is disposed" }
        hoveredOwner?.onMouseScroll(
            position,
            MouseScrollEvent(delta, orientation),
            pointerInputEvent = pointerInputEvent(
                PointerEventType.Unknown,
                position,
                timeMillis,
                nativeEvent,
                type,
                isMousePressed,
                pointerId
            )
        )
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

    internal fun onInputMethodEvent(event: Any) = this.onPlatformInputMethodEvent(event)
}

internal expect fun ComposeScene.onPlatformInputMethodEvent(event: Any)

internal expect fun pointerInputEvent(
    eventType: PointerEventType,
    position: Offset,
    timeMillis: Long,
    nativeEvent: Any?,
    type: PointerType,
    isMousePressed: Boolean,
    pointerId: Long
): PointerInputEvent
