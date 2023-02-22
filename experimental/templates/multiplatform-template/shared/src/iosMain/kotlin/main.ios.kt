import androidx.compose.ui.window.Application
import platform.UIKit.UIViewController

actual fun getPlatformName(): String = "iOS"

fun MainViewController() : UIViewController =
    Application("Demo") {
        App()
    }
