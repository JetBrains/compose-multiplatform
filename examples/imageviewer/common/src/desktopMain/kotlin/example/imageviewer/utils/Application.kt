package example.imageviewer.utils

import androidx.compose.desktop.AppManager
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.WindowEvents
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.MenuBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun Application(
    content: @Composable ApplicationScope.() -> Unit
) = SwingUtilities.invokeLater {
    AppManager.setEvents(onWindowsEmpty = null)
    val scope = ApplicationScope(content)
    scope.start()
}

@OptIn(ExperimentalComposeApi::class, ExperimentalCoroutinesApi::class)
class ApplicationScope(
    private val content: @Composable ApplicationScope.() -> Unit
) {
    private val frameClock = ImmediateFrameClock()
    private val context = Dispatchers.Main + frameClock
    private val scope = CoroutineScope(context)

    private val recomposer = Recomposer(context)
    private val composition = Composition(EmptyApplier(), recomposer)

    private val windows = mutableSetOf<AppWindow>()
    private var windowsVersion by mutableStateOf(Any())

    fun start() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        composition.setContent {
            content()
            WindowsMonitor()
        }
    }

    @Composable
    private fun WindowsMonitor() {
        LaunchedEffect(windowsVersion) {
            if (windows.isEmpty()) {
                dispose()
                exitProcess(0)
            }
        }
    }

    private fun dispose() {
        composition.dispose()
        scope.cancel()
    }

    // TODO make parameters observable (now if any parameter is changed we don't change the window)
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
        if (isOpened) {
            DisposableEffect(Unit) {
                lateinit var window: AppWindow

                fun onClose() {
                    if (isOpened) {
                        windows.remove(window)
                        onDismissRequest?.invoke()
                        windowsVersion = Any()
                        isOpened = false
                    }
                }

                window = AppWindow(
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
                        onClose()
                    }
                )

                windows.add(window)
                window.show(recomposer, content)

                onDispose {
                    if (!window.isClosed) {
                        window.close()
                    }
                    onClose()
                }
            }
        }
    }
}

private class ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (frameTimeNanos: Long) -> R
    ) = onFrame(System.nanoTime())
}

@OptIn(ExperimentalComposeApi::class)
private class EmptyApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) = Unit
    override fun up() = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun clear() = Unit
}
