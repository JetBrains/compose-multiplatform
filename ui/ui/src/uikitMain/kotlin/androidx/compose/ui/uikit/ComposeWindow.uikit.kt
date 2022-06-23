package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.createSkiaLayer
import androidx.compose.ui.native.ComposeLayer
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.useContents
import platform.Foundation.NSCoder
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import org.jetbrains.skiko.SkikoUIView

// The only difference with macos' Window is that
// it has return type of UIViewController rather than unit.
fun Application(
    title: String = "JetpackNativeWindow",
    content: @Composable () -> Unit = { }

) = ComposeWindow().apply {
    setTitle(title)
    setContent(content)
} as UIViewController


@ExportObjCClass
internal actual class ComposeWindow : UIViewController {
    @OverrideInit
    actual constructor() : super(nibName = null, bundle = null)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    private lateinit var layer: ComposeLayer
    private lateinit var content: @Composable () -> Unit

    actual fun setTitle(title: String) {
        println("TODO: set title to SkiaWindow")
    }

    override fun loadView() {
        val (width, height) = UIScreen.mainScreen.bounds.useContents {
            this.size.width to this.size.height
        }
        val skiaLayer = createSkiaLayer()
        val skikoUIView = SkikoUIView(skiaLayer).load()
        view = skikoUIView
        layer = ComposeLayer(
            layer = skiaLayer,
            showSoftwareKeyboard = {
                skikoUIView.showScreenKeyboard()
            },
            hideSoftwareKeyboard = {
                skikoUIView.hideScreenKeyboard()
            },
        )
        layer.setContent(content = content)
        layer.setSize(width.toInt(), height.toInt())
    }

    // viewDidUnload() is deprecated and not called.
    override fun viewDidDisappear(animated: Boolean) {
        this.dispose()
    }

    actual fun setContent(
        content: @Composable () -> Unit
    ) {
        println("ComposeWindow.setContent")
        this.content = content
    }

    actual fun dispose() {
        layer.dispose()
    }
}
