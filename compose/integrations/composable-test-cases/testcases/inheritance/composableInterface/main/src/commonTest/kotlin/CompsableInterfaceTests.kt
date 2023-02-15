import androidx.compose.runtime.Composable
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CompsableInterfaceTests {

    @Test
    fun testComposableContentInterfaceImpl() = runTest {
        val impl: ComposableContent = ComposableContentImpl()

        val root = composeText {
            impl.ComposableContent()
        }

        assertEquals("root:{ComposableContent}", root.dump())
    }

    @Test
    fun testComposableContentInterfaceImpl2() = runTest {
        val impl: ComposableContent = ComposableContentImpl()

        val root = composeText {
            impl.ComposableContentWithChildren {
                TextLeafNode("Leaf")
            }
        }

        assertEquals("root:{ComposableContent:{Leaf}}", root.dump())
    }

    @Test
    fun testComposableContentDelegation() = runTest {
        val impl: ComposableContent = DelegateComposableContent(
            object : ComposableContent {
                @Composable
                override fun ComposableContent() {
                    TextLeafNode("anonymous_ComposableContent")
                }

                @Composable
                override fun ComposableContentWithChildren(moreContent: @Composable () -> Unit) {
                    TextContainerNode("anonymous_ComposableContent2", moreContent)
                }
            }
        )

        val root = composeText {
            impl.ComposableContent()
            impl.ComposableContentWithChildren {
                TextLeafNode("Leaf")
            }
        }

        assertEquals(
            "root:{anonymous_ComposableContent, anonymous_ComposableContent2:{Leaf}}",
            root.dump()
        )
    }

    @Test
    fun testOverrideOpenComposableContentImpl() = runTest {
        val impl = FinalComposableContentImpl()

        val root = composeText {
            impl.ComposableContent()
            impl.ComposableContentWithChildren {
                TextLeafNode("Leaf")
            }
        }

        assertEquals(
            "root:{FinalComposableContentImpl:{OpenComposableContentImpl}, FinalComposableContentImpl:{OpenComposableContentImpl:{Leaf}}}",
            root.dump()
        )
    }
}

private class FinalComposableContentImpl : OpenComposableContentImpl() {

    @Composable
    override fun ComposableContent() {
        TextContainerNode("FinalComposableContentImpl") {
            super.ComposableContent()
        }
    }

    @Composable
    override fun ComposableContentWithChildren(moreContent: @Composable () -> Unit) {
        TextContainerNode("FinalComposableContentImpl") {
            super.ComposableContentWithChildren(moreContent)
        }
    }
}
