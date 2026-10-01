import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Extra inset (in points) to clear a native SwiftUI header/footer drawn on top of this
// Compose content, on top of what WindowInsets.systemBars already accounts for.
fun ChatViewController(extraTopInset: Double, extraBottomInset: Double): UIViewController = ComposeUIViewController {
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val combinedPadding = PaddingValues(
        start = systemBars.calculateStartPadding(layoutDirection),
        top = systemBars.calculateTopPadding() + extraTopInset.dp,
        end = systemBars.calculateEndPadding(layoutDirection),
        bottom = systemBars.calculateBottomPadding() + extraBottomInset.dp,
    )
    ChatApp(
        displayTextField = false,
        contentPadding = combinedPadding,
    )
}

fun sendMessage(text: String) {
    store.send(Action.SendMessage(Message(myUser, text)))
}

fun gradient3Colors() = ChatColors.GRADIENT_3

fun surfaceColor() = ChatColors.SURFACE
