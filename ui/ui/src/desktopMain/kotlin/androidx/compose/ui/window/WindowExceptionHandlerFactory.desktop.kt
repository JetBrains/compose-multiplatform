package androidx.compose.ui.window

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * Default [WindowExceptionHandlerFactory], which will show standard error dialog, and close the window after that
 */
@ExperimentalComposeUiApi
object DefaultWindowExceptionHandlerFactory : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: Window) = WindowExceptionHandler { throwable ->
        // invokeLater here to dispatch a blocking operation (showMessageDialog)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                // if there was an error during window init, we can't use it as a parent,
                // otherwise we will have two exceptions in the log
                window.takeIf { it.isDisplayable },
                throwable.message ?: "Unknown error", "Error",
                JOptionPane.ERROR_MESSAGE
            )
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        }
        throw throwable
    }
}
/**
 * Factory of window exception handlers.
 * These handlers catch exceptions during rendering frames, handling events, or processing background Compose operations.
 */
@ExperimentalComposeUiApi
fun interface WindowExceptionHandlerFactory {
    /**
     * Create an exception handler for passed [window]. Handlers run in the UI thread.
     */
    fun exceptionHandler(window: Window): WindowExceptionHandler
}

/**
 * The CompositionLocal that provides [WindowExceptionHandlerFactory].
 */
@ExperimentalComposeUiApi
val LocalWindowExceptionHandlerFactory = staticCompositionLocalOf<WindowExceptionHandlerFactory> {
    DefaultWindowExceptionHandlerFactory
}
