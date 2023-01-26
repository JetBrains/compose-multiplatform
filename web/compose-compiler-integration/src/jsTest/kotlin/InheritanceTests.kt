import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import kotlin.test.Test
import kotlin.test.assertEquals

class InheritanceTests {

    @Test
    // Issues:
    // https://github.com/JetBrains/compose-jb/issues/2291
    // https://github.com/JetBrains/compose-jb/issues/2660
    fun implementComposableCollection() {
        val collection = createComposableCollection().apply {
            add {
                Div { Text("Div1") }
            }
            add {
                Div { Text("Div2") }
            }
            add {
                Div { Text("Div3") }
            }
        }
        val root = document.createElement("div")

        renderComposable(root) {
            collection.list.forEach { it() }
        }

        assertEquals("<div>Div1</div><div>Div2</div><div>Div3</div>", root.innerHTML)
    }

    @Test
    fun implementComposableContent() {
        val contentImpl = createComposableContent()

        val root = document.createElement("div")

        renderComposable(root) {
            contentImpl.ComposableContent()
        }

        assertEquals("<div>ComposableContent</div>", root.innerHTML)
    }

    @Test
    fun implementComposableContentDelegation() {
        val contentImpl = ComposableContentDelegation(createComposableContent())

        val root = document.createElement("div")

        renderComposable(root) {
            contentImpl.ComposableContent()
        }

        assertEquals("<div>ComposableContent</div>", root.innerHTML)
    }

    @Test
    fun testAbstrComposableContentExtendImpl() {
        val contentImpl: ComposableContent = AbstrComposableContentExtendImpl()

        val root = document.createElement("div")

        renderComposable(root) {
            contentImpl.ComposableContent()
        }

        assertEquals("<div>AbstrComposableContent</div><div>AbstrComposableContentImpl</div>", root.innerHTML)
    }

    @Test
    fun testAbstrComposableContentNoExtendImpl() {
        val contentImpl: ComposableContent = AbstrComposableContentNoExtendImpl()

        val root = document.createElement("div")

        renderComposable(root) {
            contentImpl.ComposableContent()
        }

        assertEquals("<div>AbstrComposableContent</div>", root.innerHTML)
    }
}
