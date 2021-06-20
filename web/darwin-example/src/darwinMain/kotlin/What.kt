import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.VStack
import org.jetbrains.compose.web.renderComposable
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import co.touchlab.compose.darwin.UIViewWrapper
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.foundation.layout.Box
import org.jetbrains.compose.common.material.Button
import org.jetbrains.compose.common.material.Text
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.background
import org.jetbrains.compose.common.ui.padding
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.web.HStack

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
            repeat(80) {
                Text("Row $it")
            }
            /*Hello()
            World()
            Box(modifier = Modifier.background(Color.Yellow)) {
                Text("In Box")
            }
            HStack(spacing = spacing) {
                repeat(textRows) {
                    Text("H-$it")
                }
            }
            Text("Button Below")
            Button(onClick = {
                spacing -= 1.toDouble()
            }){
                Text("+ Spacing ($spacing)")
            }
            Text("Kotlin Button Below")
            Button(onClick = {
                spacing += 1.toDouble()
            }){
                Text("+ Spacing ($spacing)")
            }
            Text("Text Minus",
                color = Color.Magenta,
                modifier = Modifier
                    .background(Color.Blue)
                    .padding(Dp(80F))
            )
            Button(onClick = {
                textRows--
            }){
                Text("Minus Text")
            }
            Text("Text Plus")
            Button(onClick = {
                textRows++
            }){
                Text("Plus Text")
            }
            repeat(textRows) {
                ShowText("Text Row $it")
            }*/
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