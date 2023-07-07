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
        // invokeLater here to dispatch a blocking operation
        SwingUtilities.invokeLater {
            // if there was an error during window init, we can't use it as a parent,
            // otherwise we will have two exceptions in the log
            showErrorDialog(window.takeIf { it.isDisplayable }, throwable)
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

private fun showErrorDialog(parentComponent: Window?, throwable: Throwable) {
    val title = "Error"
    val message = throwable.message ?: "Unknown error"
    val pane = object : JOptionPane(message, ERROR_MESSAGE) {
        // Limit width for long messages
        override fun getMaxCharactersPerLineCount(): Int = 120
    }
    val dialog = pane.createDialog(parentComponent, title)
    dialog.isVisible = true
    dialog.dispose()
}
