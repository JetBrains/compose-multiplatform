import org.jetbrains.compose.web.renderComposable
import platform.UIKit.UIView

object IosApp {
    fun attachMain(view: UIView) {
        renderComposable(view) {
            SomeCode.HelloWorld()
        }
    }
}