import androidx.compose.runtime.Composable
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

    /** Default args for overridden composable produces corrupted function definitions
     *  https://github.com/JetBrains/compose-multiplatform/issues/3318
     */
    @Test
    fun testDefaultArgsForOverridden() = runTest {
        class Impl : DefaultComposableContent
        
        val root = composeText {
            Impl().ComposableContent()
        }

        assertEquals(
            expected = "root:{DefaultComposableContent - any}",
            actual = root.dump()
        )
    }

    /** Override a protected @Composable method leads to Compilation Failed on iOS target
     * https://github.com/JetBrains/compose-multiplatform/issues/4055
     */
    @Test
    fun testOverrideProtected() = runTest {
        val root = composeText {
            Greeter("Bob").Hi()
        }

        assertEquals(
            expected = "root:{Hello, Bob!}",
            actual = root.dump()
        )
    }

    /** Default params for value type defined in separate module may result in compilation failure on iOS
     * https://github.com/JetBrains/compose-multiplatform/issues/3643
     */
    @Test
    fun testDefaultParamValueClass() = runTest {
        @Composable
        fun test(qualifiers: ValClass = ValClass(123)): String = "${qualifiers.key}"

        val root = composeText { TextLeafNode(test()) }

        assertEquals(
            expected = "root:{123}",
            actual = root.dump()
        )
    }
}


