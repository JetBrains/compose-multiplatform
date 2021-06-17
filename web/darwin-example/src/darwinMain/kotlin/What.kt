import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.Text
import org.jetbrains.compose.web.VStack
import org.jetbrains.compose.web.renderComposable
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.Button
import org.jetbrains.compose.web.KotlinButton

object What {
    fun attachMain(view: UIView) {
        renderComposable(view) {
            HelloWorld()
        }
    }

    @Composable
    internal fun HelloWorld() {
        var spacing by remember { mutableStateOf(0.0.toDouble()) }
        VStack(spacing) {
            Hello()
            World()
            Text("Button Below")
            Button("+ Spacing ($spacing)") {
                spacing -= 1.toDouble()
            }
            Text("Kotlin Button Below")
            KotlinButton("+ Spacing ($spacing)") {
                spacing += 1.toDouble()
            }
            ShowText("Making Stuff")
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

    @Composable
    internal fun ShowText(st: String) {
        Text("Some Text $st")
    }
}