import com.example.common.TextContainerNode
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Tests {

    @Test
    fun testExample() = runTest {
        val root = composeText {
            SimpleComposable()
        }

        assertEquals("root:{SimpleComposable}", root.dump())
    }

    @Test
    fun testExample2() = runTest {
        val root = composeText {
            TextLeafNode("Leaf")
            TextContainerNode("node") {
                TextLeafNode("child1")
                TextLeafNode("child2")
                TextLeafNode("child3")
            }
        }

        assertEquals("root:{Leaf, node:{child1, child2, child3}}", root.dump())
    }
}
