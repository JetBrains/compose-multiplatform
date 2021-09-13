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
package androidx.compose.ui.platform

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
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import java.awt.event.InputMethodEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import kotlin.coroutines.CoroutineContext
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal val LocalDesktopOwners = staticCompositionLocalOf<DesktopOwners> {
    error("CompositionLocal LocalDesktopOwners not provided")
}

/**
 * Virtual container that encapsulates Compose UI content. UI content can be constructed via
 * [setContent] method and with any Composable that manipulates [LayoutNode] tree.
 *
 * To draw content on [Canvas], you can use [render] method.
 *
 * To specify available size for the content, you should use [constraints].
 *
 * After [DesktopOwners] will no longer needed, you should call [dispose] method, so all resources
 * and subscriptions will be properly closed. Otherwise there can be a memory leak.
 */
internal class DesktopOwners internal constructor(
    coroutineContext: CoroutineContext,
    component: DesktopComponent,
    density: Density,
    private val invalidate: () -> Unit
) {
    /**
     * Constructs [DesktopOwners]
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
        DummyDesktopComponent,
        density,
        invalidate
    )

    private var isInvalidationDisabled = false

    @Volatile
    private var hasPendingDraws = true
    private inline fun postponeInvalidation(block: () -> Unit) {
        isInvalidationDisabled = true
        try {
            block()
        } finally {
            isInvalidationDisabled = false
        }
        invalidateIfNeeded()
    }

    private fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters || list.any(DesktopOwner::needsRender)
        if (hasPendingDraws && !isInvalidationDisabled) {
            invalidate()
        }
    }

    private val list = LinkedHashSet<DesktopOwner>()
    private val listCopy = mutableListOf<DesktopOwner>()

    private inline fun forEachOwner(action: (DesktopOwner) -> Unit) {
        listCopy.addAll(list)
        listCopy.forEach(action)
        listCopy.clear()
    }

    /**
     * All currently registered [DesktopRootForTest]s. After calling [setContent] the first root
     * will be added. If there is an any [Popup] is present in the content, it will be added as
     * another [DesktopRootForTest]
     */
    val roots: Set<RootForTest> get() = list

    private var pointerId = 0L
    private var isMousePressed = false

    private val job = Job()
    private val dispatcher = FlushCoroutineDispatcher(CoroutineScope(coroutineContext + job))
    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)
    private val coroutineScope = CoroutineScope(coroutineContext + job + dispatcher + frameClock)

    private val recomposer = Recomposer(coroutineScope.coroutineContext)
    internal val platformInputService: DesktopPlatformInput = DesktopPlatformInput(component)

    private var mainOwner: DesktopOwner? = null
    private var composition: Composition? = null

    /**
     * Density of the content which will be used to convert [dp] units.
     */
    var density: Density = density
        set(value) {
            check(!isDisposed) { "DesktopOwners is disposed" }
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
     * Close all resources and subscriptions. Not calling this method when [DesktopOwners] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [DesktopOwners].
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

    internal fun attach(desktopOwner: DesktopOwner) {
        check(!isDisposed) { "DesktopOwners is disposed" }
        list.add(desktopOwner)
        desktopOwner.onNeedsRender = ::invalidateIfNeeded
        desktopOwner.onDispatchCommand = ::dispatchCommand
        desktopOwner.constraints = constraints
        invalidateIfNeeded()
        if (desktopOwner.isFocusable) {
            focusedOwner = desktopOwner
        }
    }

    internal fun detach(desktopOwner: DesktopOwner) {
        check(!isDisposed) { "DesktopOwners is disposed" }
        list.remove(desktopOwner)
        desktopOwner.onDispatchCommand = null
        desktopOwner.onNeedsRender = null
        invalidateIfNeeded()
        if (desktopOwner == focusedOwner) {
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
     * @param content Content of the [DesktopOwners]
     */
    fun setContent(
        content: @Composable () -> Unit
    ) = setContent(
        parentComposition = null,
        content = content
    )

    // TODO(demin): We should configure routing of key events if there
    //  are any popups/root present:
    //   - DesktopOwners.sendKeyEvent
    //   - DesktopOwners.onPreviewKeyEvent (or Window.onPreviewKeyEvent)
    //   - Popup.onPreviewKeyEvent
    //   - NestedPopup.onPreviewKeyEvent
    //   - NestedPopup.onKeyEvent
    //   - Popup.onKeyEvent
    //   - DesktopOwners.onKeyEvent
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
        check(!isDisposed) { "DesktopOwners is disposed" }
        composition?.dispose()
        mainOwner?.dispose()
        val mainOwner = DesktopOwner(
            platformInputService,
            density,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        attach(mainOwner)
        composition = mainOwner.setContent(parentComposition ?: recomposer) {
            CompositionLocalProvider(
                LocalDesktopOwners provides this,
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
            check(!isDisposed) { "DesktopOwners is disposed" }
            val mainOwner = mainOwner ?: return IntSize.Zero
            mainOwner.measureAndLayout()
            return IntSize(mainOwner.root.width, mainOwner.root.height)
        }

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(canvas: Canvas, nanoTime: Long) {
        check(!isDisposed) { "DesktopOwners is disposed" }
        postponeInvalidation {
            // We must see the actual state before we will render the frame
            Snapshot.sendApplyNotifications()
            dispatcher.flush()
            frameClock.sendFrame(nanoTime)

            forEachOwner {
                it.render(canvas)
            }
        }
    }

    private var focusedOwner: DesktopOwner? = null
    private val hoveredOwner: DesktopOwner?
        get() = list.lastOrNull { it.isHovered(pointLocation) } ?: list.lastOrNull()

    private fun DesktopOwner?.isAbove(
        targetOwner: DesktopOwner?
    ) = list.indexOf(this) > list.indexOf(targetOwner)

    fun onMousePressed(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        isMousePressed = true
        val currentOwner = hoveredOwner
        if (currentOwner != null) {
            if (focusedOwner.isAbove(currentOwner)) {
                focusedOwner?.onDismissRequest?.invoke()
                return
            } else {
                currentOwner.processPointerInput(
                    pointerInputEvent(nativeEvent, x, y, isMousePressed)
                )
                return
            }
        }
        focusedOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
    }

    fun onMouseReleased(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        isMousePressed = false
        val currentOwner = hoveredOwner
        if (currentOwner != null) {
            currentOwner.processPointerInput(
                pointerInputEvent(nativeEvent, x, y, isMousePressed)
            )
            pointerId += 1
            return
        }
        focusedOwner?.processPointerInput(pointerInputEvent(nativeEvent, x, y, isMousePressed))
        pointerId += 1
    }

    private var pointLocation = IntOffset.Zero

    fun onMouseMoved(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        pointLocation = IntOffset(x, y)
        val event = pointerInputEvent(nativeEvent, x, y, isMousePressed)
        hoveredOwner?.processPointerInput(event)
    }

    fun onMouseScroll(x: Int, y: Int, event: MouseScrollEvent) {
        val position = Offset(x.toFloat(), y.toFloat())
        hoveredOwner?.onMouseScroll(position, event)
    }

    fun onMouseEntered(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        val event = pointerInputEvent(nativeEvent, x, y, isMousePressed)
        hoveredOwner?.processPointerInput(event)
    }

    fun onMouseExited(x: Int, y: Int, nativeEvent: MouseEvent? = null) {
        val event = pointerInputEvent(nativeEvent, x, y, isMousePressed)
        hoveredOwner?.processPointerInput(event)
    }

    private fun consumeKeyEvent(event: KeyEvent): Boolean {
        return focusedOwner?.sendKeyEvent(ComposeKeyEvent(event)) == true
    }

    fun onKeyPressed(event: KeyEvent): Boolean = consumeKeyEvent(event)

    fun onKeyReleased(event: KeyEvent): Boolean = consumeKeyEvent(event)

    fun onKeyTyped(event: KeyEvent): Boolean = consumeKeyEvent(event)

    fun onInputMethodEvent(event: InputMethodEvent) {
        if (!event.isConsumed) {
            when (event.id) {
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED -> {
                    platformInputService.replaceInputMethodText(event)
                    event.consume()
                }
                InputMethodEvent.CARET_POSITION_CHANGED -> {
                    platformInputService.inputMethodCaretPositionChanged(event)
                    event.consume()
                }
            }
        }
    }

    private fun pointerInputEvent(
        nativeEvent: MouseEvent?,
        x: Int,
        y: Int,
        down: Boolean
    ): PointerInputEvent {
        val time = System.nanoTime() / 1_000_000L
        val position = Offset(x.toFloat(), y.toFloat())
        return PointerInputEvent(
            time,
            listOf(
                PointerInputEventData(
                    PointerId(pointerId),
                    time,
                    position,
                    position,
                    down,
                    PointerType.Mouse
                )
            ),
            nativeEvent
        )
    }
}
