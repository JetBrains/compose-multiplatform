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
package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.LayoutWithWorkaround
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.round
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy
import javax.swing.SwingUtilities

val NoOpUpdate: Component.() -> Unit = {}

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory]
 * block will be called to obtain the [Component] to be composed.
 *
 * The Swing component is placed on
 * top of the Compose layer (that means that Compose content can't overlap or clip it).
 * This can be changed in the future, when the better interop with Swing will be implemented. See related issues:
 * https://github.com/JetBrains/compose-jb/issues/1521
 * https://github.com/JetBrains/compose-jb/issues/1202
 * https://github.com/JetBrains/compose-jb/issues/1449
 *
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be reexecuted to set the new properties.
 *
 * @param background Background color of SwingPanel
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
public fun <T : Component> SwingPanel(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate
) {
    val componentInfo = remember { ComponentInfo<T>() }

    val root = LocalLayerContainer.current
    val density = LocalDensity.current.density
    val focusManager = LocalFocusManager.current
    val focusSwitcher = remember { FocusSwitcher(componentInfo, focusManager) }

    Box(
        modifier = modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val location = coordinates.localToWindow(Offset.Zero).round()
            val size = coordinates.size
            componentInfo.container.setBounds(
                (location.x / density).toInt(),
                (location.y / density).toInt(),
                (size.width / density).toInt(),
                (size.height / density).toInt()
            )
            componentInfo.container.validate()
            componentInfo.container.repaint()
        }
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(factory) {
        val focusListener = object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (componentInfo.container.isParentOf(e.oppositeComponent)) {
                    when (e.cause) {
                        FocusEvent.Cause.TRAVERSAL_FORWARD -> focusSwitcher.moveForward()
                        FocusEvent.Cause.TRAVERSAL_BACKWARD -> focusSwitcher.moveBackward()
                        else -> Unit
                    }
                }
            }

            override fun focusLost(e: FocusEvent) = Unit
        }
        root.addFocusListener(focusListener)
        componentInfo.component = factory()
        componentInfo.container = JPanel().apply {
            layout = BorderLayout(0, 0)
            focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
                override fun getComponentAfter(aContainer: Container?, aComponent: Component?): Component? {
                    return if (aComponent == getLastComponent(aContainer)) {
                        root
                    } else {
                        super.getComponentAfter(aContainer, aComponent)
                    }
                }

                override fun getComponentBefore(aContainer: Container?, aComponent: Component?): Component? {
                    return if (aComponent == getFirstComponent(aContainer)) {
                        root
                    } else {
                        super.getComponentBefore(aContainer, aComponent)
                    }
                }
            }
            isFocusCycleRoot = true
            add(componentInfo.component)
        }
        componentInfo.updater = Updater(componentInfo.component, update)
        root.add(componentInfo.container)
        onDispose {
            root.remove(componentInfo.container)
            componentInfo.updater.dispose()
            root.removeFocusListener(focusListener)
        }
    }

    SideEffect {
        componentInfo.container.background = parseColor(background)
        componentInfo.updater.update = update
    }
}

private class FocusSwitcher<T : Component>(
    private val info: ComponentInfo<T>,
    private val focusManager: FocusManager
) {
    private val backwardRequester = FocusRequester()
    private val forwardRequester = FocusRequester()
    private var isRequesting = false

    fun moveBackward() {
        try {
            isRequesting = true
            backwardRequester.requestFocus()
        } finally {
            isRequesting = false
        }
        focusManager.moveFocus(FocusDirection.Previous)
    }

    fun moveForward() {
        try {
            isRequesting = true
            forwardRequester.requestFocus()
        } finally {
            isRequesting = false
        }
        focusManager.moveFocus(FocusDirection.Next)
    }

    @Composable
    fun Content() {
        Box(
            Modifier
                .focusRequester(backwardRequester)
                .onFocusChanged {
                    if (it.isFocused && !isRequesting) {
                        focusManager.clearFocus(force = true)
                        info.container.focusTraversalPolicy
                            .getFirstComponent(info.container)
                            .requestFocus(FocusEvent.Cause.TRAVERSAL_FORWARD)
                    }
                }
                .focusTarget()
        )
        Box(
            Modifier
                .focusRequester(forwardRequester)
                .onFocusChanged {
                    if (it.isFocused && !isRequesting) {
                        focusManager.clearFocus(force = true)
                        info.container.focusTraversalPolicy
                            .getLastComponent(info.container)
                            .requestFocus(FocusEvent.Cause.TRAVERSAL_BACKWARD)
                    }
                }
                .focusTarget()
        )
    }
}

@Composable
private fun Box(modifier: Modifier, content: @Composable () -> Unit = {}) {
    LayoutWithWorkaround(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(
                placeables.maxOfOrNull { it.width } ?: 0,
                placeables.maxOfOrNull { it.height } ?: 0
            ) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        }
    )
}

private fun parseColor(color: Color): java.awt.Color {
    return java.awt.Color(
        color.component1(),
        color.component2(),
        color.component3(),
        color.component4()
    )
}

private class ComponentInfo<T : Component> {
    lateinit var container: Container
    lateinit var component: T
    lateinit var updater: Updater<T>
}

private class Updater<T : Component>(
    private val component: T,
    update: (T) -> Unit
) {
    private var isDisposed = false
    private val isUpdateScheduled = AtomicBoolean()
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            SwingUtilities.invokeLater {
                isUpdateScheduled.set(false)
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}
