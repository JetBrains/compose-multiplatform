import com.example.common.TextLeafNode
import com.example.common.composeText
import com.example.common.currentPlatform
import com.lib.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Tests {

    @Test
    // K/JS fails. Related: https://github.com/JetBrains/compose-multiplatform/issues/3373
    fun composableExpectActualValGetter() = runTest {
        val root = composeText {
            val v = Abc().composableIntVal
            TextLeafNode("$v")
        }

        assertEquals("root:{100}", root.dump())
    }

    @Test
    fun commonComposableValGetter() = runTest {
        val root = composeText {
            val v = Abc().commonIntVal
            TextLeafNode("$v")
        }

        assertEquals("root:{1000}", root.dump())
    }

    @Test
    fun callExpectActualNotComposableFun() = runTest {
        val root = composeText {
            TextLeafNode(getPlatformName())
        }
        assertEquals("root:{${currentPlatform().name}}", root.dump())
    }

    @Test
    fun callTopLevelExpectActualNotComposableFun() = runTest {
        val root = composeText {
            TextLeafNode(TopLevelExpectActual())
        }
        assertEquals("root:{TopLevelExpectActual-${currentPlatform().name}}", root.dump())
    }

    @Test
    fun callComposableExpectActualFun() = runTest {
        val root = composeText {
            ComposableExpectActual()
        }
        assertEquals("root:{${currentPlatform().name}}", root.dump())
    }

    @Test
    fun callComposableExpectActualFunWithDefaultParameter() = runTest {
        val root = composeText {
            ComposableExpectActualWithDefaultParameter()
        }
        assertEquals("root:{defaultValue-${currentPlatform().name}}", root.dump())
    }

}
