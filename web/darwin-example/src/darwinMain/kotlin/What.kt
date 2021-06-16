import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.Text
import org.jetbrains.compose.web.VStack
import org.jetbrains.compose.web.renderComposable
import platform.UIKit.UIView
import platform.UIKit.UIViewController

object What {
    fun attachMain(view: UIView) {
        renderComposable(view) {
            HelloWorld()
        }
    }

    @Composable
    internal fun HelloWorld() {
        VStack {
            Hello()
            World()
        }
    }

    @Composable
    internal fun Hello() {
        Text("hello")
    }

    @Composable
    internal fun World() {
        Text("world")
    }
}