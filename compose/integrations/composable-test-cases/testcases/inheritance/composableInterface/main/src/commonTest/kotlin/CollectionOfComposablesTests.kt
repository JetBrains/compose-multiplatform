import com.example.common.TextContainerNode
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionOfComposablesTests {

    @Test
    fun testCanAddItemsAndIterate() = runTest {
        val impl = CollectionOfComposablesImpl()

        impl.add {
            TextLeafNode("leaf1")
            TextLeafNode("leaf2")
            TextLeafNode("leaf3")
        }
        impl.add {
            TextContainerNode("node") {
                TextLeafNode("child1")
                TextLeafNode("child2")
                TextLeafNode("child3")
            }
        }

        val root = composeText {
            impl.iterator().forEach {
                it()
            }
        }

        assertEquals(
            expected = "root:{leaf1, leaf2, leaf3, node:{child1, child2, child3}}",
            actual = root.dump()
        )
    }
}
