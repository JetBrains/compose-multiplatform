/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AwtWindowDragTargetListener.WindowDragValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.density
import java.awt.Point
import java.awt.Window
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener

/**
 * Represent data that is being dragged (or dropped) to a component from outside an application.
 */
@ExperimentalComposeUiApi
interface DragData {
    /**
     * Represents list of files drag and dropped to a component.
     */
    interface FilesList : DragData {
        /**
         * Returns list of file paths drag and droppped to an application in a URI format.
         */
        fun readFiles(): List<String>
    }

    /**
     * Represents an image drag and dropped to a component.
     */
    interface Image : DragData {
        /**
         * Returns an image drag and dropped to an application as a [Painter] type.
         */
        fun readImage(): Painter
    }

    /**
     * Represent text drag and dropped to a component.
     */
    interface Text : DragData {
        /**
         * Provides the best MIME type that describes text returned in [readText]
         */
        val bestMimeType: String

        /**
         * Returns a text dropped to an application.
         */
        fun readText(): String
    }
}

/**
 * Represent the current state of drag and drop to a component from outside an application.
 * This state is passed to external drag callbacks.
 *
 * @see onExternalDrag
 */
@ExperimentalComposeUiApi
@Immutable
class ExternalDragValue(
    /**
     * Position of the pointer relative to the component
     */
    val dragPosition: Offset,
    /**
     * Data that it being dragged (or dropped) in a component bounds
     */
    val dragData: DragData
)

/**
 * Adds detector of external drag and drop (e.g. files DnD from Finder to an application)
 *
 * @param onDragStart will be called when the pointer with external content entered the component.
 * @param onDrag will be called for pointer movements inside the component.
 * @param onDragExit is called if the pointer exited the component bounds.
 * @param onDrop is called when the pointer is released.
 */
@ExperimentalComposeUiApi
@Composable
fun Modifier.onExternalDrag(
    enabled: Boolean = true,
    onDragStart: (ExternalDragValue) -> Unit = {},
    onDrag: (ExternalDragValue) -> Unit = {},
    onDragExit: () -> Unit = {},
    onDrop: (ExternalDragValue) -> Unit = {},
): Modifier = composed {
    if (!enabled) {
        return@composed Modifier
    }
    val window = LocalWindow.current ?: return@composed Modifier

    val componentDragHandler = rememberUpdatedState(
        AwtWindowDropTarget.ComponentDragHandler(onDragStart, onDrag, onDragExit, onDrop)
    )

    var componentDragHandleId by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(window) {
        when (val currentDropTarget = window.dropTarget) {
            is AwtWindowDropTarget -> {
                // if our drop target is already assigned simply add new drag handler for the current component
                componentDragHandleId =
                    currentDropTarget.installComponentDragHandler(componentDragHandler)
            }

            null -> {
                // drop target is not installed for the window, so assign it and add new drag handler for the current component
                val newDropTarget = AwtWindowDropTarget(window)
                componentDragHandleId =
                    newDropTarget.installComponentDragHandler(componentDragHandler)
                window.dropTarget = newDropTarget
            }

            else -> {
                error("Window already has unknown external dnd handler, cannot attach onExternalDrag")
            }
        }

        onDispose {
            // stop drag events handling for this component when window is changed
            // or the component leaves the composition
            val dropTarget = window.dropTarget as? AwtWindowDropTarget ?: return@onDispose
            val handleIdToRemove = componentDragHandleId ?: return@onDispose
            dropTarget.stopDragHandling(handleIdToRemove)
        }
    }

    Modifier
        .onGloballyPositioned { position ->
            // provide new component bounds to Swing to properly detect drag events
            val dropTarget = window.dropTarget as? AwtWindowDropTarget
                ?: return@onGloballyPositioned
            val handleIdToUpdate = componentDragHandleId ?: return@onGloballyPositioned
            val componentBounds = position.boundsInWindow()
            dropTarget.updateComponentBounds(handleIdToUpdate, componentBounds)
        }
}

/**
 * Provides a way to subscribe on external drag for given [window] using [installComponentDragHandler]
 *
 * [Window] allows having only one [DropTarget], so this is the main [DropTarget] that handles all the drag subscriptions
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class AwtWindowDropTarget(
    private val window: Window
) : DropTarget(window, DnDConstants.ACTION_MOVE, null, true) {
    private var idsCounter = 0

    // all components that are subscribed to external drag and drop for the window
    // handler's callbacks can be changed on recompositions, so State is kept here
    private val handlers = mutableMapOf<Int, State<ComponentDragHandler>>()

    // bounds of all components that are subscribed to external drag and drop for the window
    private val componentBoundsHolder = mutableMapOf<Int, Rect>()

    // state of ongoing external drag and drop in the [window], contains pointer coordinates and data that is dragged
    private var currentDragValue: WindowDragValue? = null

    val dragTargetListener = AwtWindowDragTargetListener(
        window,
        // notify components on window border that drag is started.
        onDragEnterWindow = { newDragValue ->
            currentDragValue = newDragValue
            forEachPositionedComponent { handler, componentBounds ->
                handleDragEvent(
                    handler,
                    oldComponentBounds = componentBounds, currentComponentBounds = componentBounds,
                    oldDragValue = null, currentDragValue = newDragValue,
                )
            }
        },
        // drag moved inside window, we should calculate whether drag entered/exited components or just moved inside them
        onDragInsideWindow = { newDragValue ->
            val oldDragValue = currentDragValue
            currentDragValue = newDragValue
            forEachPositionedComponent { handler, componentBounds ->
                handleDragEvent(
                    handler,
                    oldComponentBounds = componentBounds, currentComponentBounds = componentBounds,
                    oldDragValue, newDragValue
                )
            }
        },
        // notify components on window border drag exited window
        onDragExit = {
            val oldDragValue = currentDragValue
            currentDragValue = null
            forEachPositionedComponent { handler, componentBounds ->
                handleDragEvent(
                    handler,
                    oldComponentBounds = componentBounds, currentComponentBounds = componentBounds,
                    oldDragValue = oldDragValue, currentDragValue = null
                )
            }
        },
        // notify all components under the pointer that drop happened
        onDrop = { newDragValue ->
            var anyDrops = false
            currentDragValue = null
            forEachPositionedComponent { handler, componentBounds ->
                val isInside = isExternalDragInsideComponent(
                    componentBounds,
                    newDragValue.dragPositionInWindow
                )
                if (isInside) {
                    val offset = calculateOffset(componentBounds, newDragValue.dragPositionInWindow)
                    handler.onDrop(ExternalDragValue(offset, newDragValue.dragData))
                    anyDrops = true
                }
            }
            // tell swing whether some components accepted the drop
            return@AwtWindowDragTargetListener anyDrops
        }
    )

    init {
        addDropTargetListener(dragTargetListener)
    }

    override fun setActive(isActive: Boolean) {
        super.setActive(isActive)
        if (!isActive) {
            currentDragValue = null
        }
    }

    /**
     * Adds handler that will be notified on drag events for [window].
     * If component bounds are provided using [updateComponentBounds],
     * given lambdas will be called on drag events.
     *
     * [handlerState]'s callbacks can be changed on recompositions.
     * New callbacks won't be called with old events, they will be called on new AWT events only.
     *
     * @return handler id that can be used later to remove subscription using [stopDragHandling]
     * or to update component bounds using [updateComponentBounds]
     */
    fun installComponentDragHandler(handlerState: State<ComponentDragHandler>): Int {
        isActive = true
        val handleId = idsCounter++
        handlers[handleId] = handlerState
        return handleId
    }

    /**
     * Unsubscribes handler with [handleId].
     * Calls [ComponentDragHandler.onDragCancel] if drag is going and handler's component is under pointer
     *
     * Disable drag handling for [window] if there are no more handlers.
     *
     * @param handleId id provided by [installComponentDragHandler] function
     */
    fun stopDragHandling(handleId: Int) {
        val handler = handlers.remove(handleId)
        val componentBounds = componentBoundsHolder.remove(handleId)
        if (handler != null && componentBounds != null &&
            isExternalDragInsideComponent(componentBounds, currentDragValue?.dragPositionInWindow)
        ) {
            handler.value.onDragCancel()
        }

        if (handlers.isEmpty()) {
            isActive = false
        }
    }

    /**
     * Updates component bounds within the [window], so drag events will be properly handled.
     * If drag is going and component is under the pointer, onDragStart and onDrag will be called.
     * If drag is going and component moved/became smaller, so that pointer now is not the component, onDragCancel is called.
     *
     * All further drag events will use [newComponentBounds] to notify handler with [handleId].
     *
     * @param newComponentBounds new bounds of the component inside [window] used to properly detect when drag entered/exited component
     */
    fun updateComponentBounds(handleId: Int, newComponentBounds: Rect) {
        val handler = handlers[handleId] ?: return
        val oldComponentBounds = componentBoundsHolder.put(handleId, newComponentBounds)
        handleDragEvent(
            handler.value, oldComponentBounds, newComponentBounds,
            oldDragValue = currentDragValue,
            currentDragValue = currentDragValue
        )
    }

    private inline fun forEachPositionedComponent(action: (handler: ComponentDragHandler, bounds: Rect) -> Unit) {
        for ((handleId, handler) in handlers) {
            val bounds = componentBoundsHolder[handleId] ?: continue
            action(handler.value, bounds)
        }
    }

    data class ComponentDragHandler(
        val onDragStart: (ExternalDragValue) -> Unit,
        val onDrag: (ExternalDragValue) -> Unit,
        val onDragCancel: () -> Unit,
        val onDrop: (ExternalDragValue) -> Unit
    )

    companion object {
        private fun isExternalDragInsideComponent(
            componentBounds: Rect?,
            windowDragCoordinates: Offset?
        ): Boolean {
            if (componentBounds == null || windowDragCoordinates == null) {
                return false
            }

            return componentBounds.contains(windowDragCoordinates)
        }

        private fun calculateOffset(
            componentBounds: Rect,
            windowDragCoordinates: Offset
        ): Offset {
            return windowDragCoordinates - componentBounds.topLeft
        }

        /**
         * Notifies [handler] about drag events.
         *
         * Note: this function is pure, so it doesn't update any states
         */
        private fun handleDragEvent(
            handler: ComponentDragHandler,
            oldComponentBounds: Rect?,
            currentComponentBounds: Rect?,
            oldDragValue: WindowDragValue?,
            currentDragValue: WindowDragValue?,
        ) {
            val wasDragInside = isExternalDragInsideComponent(
                oldComponentBounds,
                oldDragValue?.dragPositionInWindow
            )
            val newIsDragInside = isExternalDragInsideComponent(
                currentComponentBounds,
                currentDragValue?.dragPositionInWindow
            )
            if (!wasDragInside && newIsDragInside) {
                val dragOffset = calculateOffset(
                    currentComponentBounds!!,
                    currentDragValue!!.dragPositionInWindow
                )
                handler.onDragStart(ExternalDragValue(dragOffset, currentDragValue.dragData))
                return
            }

            if (wasDragInside && !newIsDragInside) {
                handler.onDragCancel()
                return
            }

            if (newIsDragInside) {
                val dragOffset = calculateOffset(
                    currentComponentBounds!!,
                    currentDragValue!!.dragPositionInWindow
                )
                handler.onDrag(ExternalDragValue(dragOffset, currentDragValue.dragData))
                return
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal class AwtWindowDragTargetListener(
    private val window: Window,
    val onDragEnterWindow: (WindowDragValue) -> Unit,
    val onDragInsideWindow: (WindowDragValue) -> Unit,
    val onDragExit: () -> Unit,
    val onDrop: (WindowDragValue) -> Boolean,
) : DropTargetListener {
    private val density = window.density.density

    override fun dragEnter(dtde: DropTargetDragEvent) {
        onDragEnterWindow(
            WindowDragValue(
                dtde.location.windowOffset(),
                dtde.transferable.dragData()
            )
        )
    }

    override fun dragOver(dtde: DropTargetDragEvent) {
        onDragInsideWindow(
            WindowDragValue(
                dtde.location.windowOffset(),
                dtde.transferable.dragData()
            )
        )
    }

    // takes title bar and other insets into account
    private fun Point.windowOffset(): Offset {
        val offsetX = (x - window.insets.left) * density
        val offsetY = (y - window.insets.top) * density

        return Offset(offsetX, offsetY)
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent) {
        // Should we notify about it?
    }

    override fun dragExit(dte: DropTargetEvent) {
        onDragExit()
    }

    override fun drop(dtde: DropTargetDropEvent) {
        dtde.acceptDrop(dtde.dropAction)

        val transferable = dtde.transferable
        try {
            onDrop(WindowDragValue(dtde.location.windowOffset(), transferable.dragData()))
            dtde.dropComplete(true)
        } catch (e: Exception) {
            onDragExit()
            dtde.dropComplete(false)
        }
    }

    data class WindowDragValue(
        val dragPositionInWindow: Offset,
        val dragData: DragData
    )
}