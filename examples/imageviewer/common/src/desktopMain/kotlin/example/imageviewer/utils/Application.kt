package example.imageviewer.utils

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.WindowEvents
import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.v1.MenuBar
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.image.BufferedImage

fun Application(
    content: @Composable ApplicationScope.() -> Unit
) {
    GlobalScope.launch(Dispatchers.Swing + ImmediateFrameClock()) {
        AppManager.setEvents(onWindowsEmpty = null)

        withRunningRecomposer { recomposer ->
            val latch = CompletableDeferred<Unit>()
            val applier = ApplicationApplier { latch.complete(Unit) }

            val composition = Composition(applier, recomposer)
            try {
                val scope = ApplicationScope(recomposer)

                composition.setContent { scope.content() }

                latch.join()
            } finally {
                composition.dispose()
            }
        }
    }
}

class ApplicationScope internal constructor(private val recomposer: Recomposer) {
    @Composable
    fun ComposableWindow(
        title: String = "JetpackDesktopWindow",
        size: IntSize = IntSize(800, 600),
        location: IntOffset = IntOffset.Zero,
        centered: Boolean = true,
        icon: BufferedImage? = null,
        menuBar: MenuBar? = null,
        undecorated: Boolean = false,
        resizable: Boolean = true,
        events: WindowEvents = WindowEvents(),
        onDismissRequest: (() -> Unit)? = null,
        content: @Composable () -> Unit = {}
    ) {
        var isOpened by remember { mutableStateOf(true) }
        if (!isOpened) return
        ComposeNode<AppWindow, ApplicationApplier>(
            factory = {
                val window = AppWindow(
                    title = title,
                    size = size,
                    location = location,
                    centered = centered,
                    icon = icon,
                    menuBar = menuBar,
                    undecorated = undecorated,
                    resizable = resizable,
                    events = events,
                    onDismissRequest = {
                        onDismissRequest?.invoke()
                        isOpened = false
                    }
                )
                window.show(recomposer, content)
                window
            },
            update = {
                set(title) { setTitle(it) }
                set(size) { setSize(it.width, it.height) }
                // set(location) { setLocation(it.x, it.y) }
                set(icon) { setIcon(it) }
                // set(menuBar) { if (it != null) setMenuBar(it) else removeMenuBar() }
                // set(resizable) { setResizable(it) }
                // set(events) { setEvents(it) }
                // set(onDismissRequest) { setDismiss(it) }
            }
        )
    }
}

private class ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (frameTimeNanos: Long) -> R
    ) = onFrame(System.nanoTime())
}

private class ApplicationApplier(
    private val onWindowsEmpty: () -> Unit
) : Applier<AppWindow?> {
    private val windows = mutableListOf<AppWindow>()

    override var current: AppWindow? = null

    override fun insertBottomUp(index: Int, instance: AppWindow?) {
        requireNotNull(instance)
        check(current == null) { "Windows cannot be nested!" }
        windows.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            val window = windows.removeAt(index)
            if (!window.isClosed) {
                window.close()
            }
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        if (from > to) {
            var current = to
            repeat(count) {
                val node = windows.removeAt(from)
                windows.add(current, node)
                current++
            }
        } else {
            repeat(count) {
                val node = windows.removeAt(from)
                windows.add(to - 1, node)
            }
        }
    }

    override fun clear() {
        windows.forEach { if (!it.isClosed) it.close() }
        windows.clear()
    }

    override fun onEndChanges() {
        if (windows.isEmpty()) {
            onWindowsEmpty()
        }
    }

    override fun down(node: AppWindow?) {
        requireNotNull(node)
        check(current == null) { "Windows cannot be nested!" }
        current = node
    }

    override fun up() {
        check(current != null) { "Windows cannot be nested!" }
        current = null
    }

    override fun insertTopDown(index: Int, instance: AppWindow?) {
        // ignored. Building tree bottom-up
    }
}
