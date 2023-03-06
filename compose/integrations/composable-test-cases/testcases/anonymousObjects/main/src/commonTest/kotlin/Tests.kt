import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    // Issue: content.Abc$composable_z540rc_k$ is not a function
    // https://github.com/JetBrains/compose-jb/issues/2549
    fun testComposableInAnonymousObject() = runTest {
        val root = composeText {
            val content: HasComposable2 = createHasComposable()
            content.Abc()
        }

        assertEquals("root:{div:{Abc}}", root.dump())
    }

    @Test
    // Issue:
    // java.lang.IllegalArgumentException: Could not find local implementation for Abc$composable
    // at androidx.compose.compiler.plugins.kotlin.lower.decoys.DecoyTransformBase$DefaultImpls.getComposableForDecoy(DecoyTransformBase.kt:110)
    fun testLocalClassWithComposable() = runTest {
        val root = composeText {
            HasLocalClassWithComposable()
        }

        assertEquals("root:{div:{Abc2}}", root.dump())
    }

    @Test
    fun testConstructorWithComposable() = runTest {
        val root = composeText {
            TestConstructor { return@TestConstructor 111 }.otherComposable!!.invoke()
        }

        assertEquals("root:{div:{Abc-111}}", root.dump())
    }
}

@Composable
internal fun HasLocalClassWithComposable() {
    class Abc : HasComposable2 {
        @Composable
        override fun Abc() {
            TextContainerNode("div") {
                TextLeafNode("Abc2")
            }
        }
    }

    val abc = remember { Abc() }
    abc.Abc()
}

@Composable
internal fun createHasComposable(): HasComposable2 {
    return object : HasComposable2 {
        @Composable
        override fun Abc() {
            TextContainerNode("div") {
                TextLeafNode("Abc")
            }
        }
    }
}
