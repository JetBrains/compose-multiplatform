package html2

import androidx.compose.runtime.Composable
import org.jetbrains.compose.html2.internal.ComposeHtml2Context

class TestInternal {
}


private val testComposeHtml2Context = object : ComposeHtml2Context {

    @Composable
    override fun TagElement(
        tag: String,
        attrsScope: () -> Unit,
        content: @Composable (() -> Unit)
    ) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun TagElement(tag: String) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun TextElement(text: String) {
        TODO("Not yet implemented")
    }

    override fun composeHtmlToString(content: @Composable (() -> Unit)): String {
        TODO("Not yet implemented")
    }

}