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
import co.touchlab.compose.darwin.UIViewWrapper
import org.jetbrains.compose.web.Button
import org.jetbrains.compose.web.HStack
import org.jetbrains.compose.web.KotlinButton

object What {
    fun attachMain(view: UIView) {
        renderComposable(view) {
            HelloWorld()
        }
    }

    fun makeWrapper(): UIViewWrapper<*> {
        TODO()
    }

    @Composable
    internal fun HelloWorld() {
        var spacing by remember { mutableStateOf(0.0) }
        var textRows by remember { mutableStateOf(1) }
        VStack(spacing) {
            Hello()
            World()
            HStack(spacing = spacing) {
                repeat(textRows) {
                    Text("H-$it")
                }
            }
            Text("Button Below")
            Button("+ Spacing ($spacing)") {
                spacing -= 1.toDouble()
            }
            Text("Kotlin Button Below")
            Button("+ Spacing ($spacing)") {
                spacing += 1.toDouble()
            }
            Text("Text Minus")
            Button("Minus Text") {
                textRows--
            }
            Text("Text Plus")
            Button("Plut Text") {
                textRows++
            }
            repeat(textRows) {
                ShowText("Text Row $it")
            }
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