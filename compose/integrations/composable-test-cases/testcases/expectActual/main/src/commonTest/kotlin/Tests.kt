import com.example.common.TextLeafNode
import com.example.common.composeText
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
    fun composableImplicitExpectActualValGetter() = runTest {
        val root = composeText {
            val v = GetIntVal()
            TextLeafNode("$v")
        }

        assertEquals("root:{100}", root.dump())
    }

    @Test
    fun composableImplicitExpectActualValGetterWithDefault() = runTest {
        val root = composeText {
            val v = GetIntValWithDefault()
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

}
