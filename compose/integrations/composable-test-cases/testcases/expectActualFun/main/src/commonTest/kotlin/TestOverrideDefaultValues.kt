import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import my.abc.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TestOverrideDefaultValues {

    @Test
    fun testWithDefaultIntParam() = runTest {
        val root = composeText {
            WithDefaultIntParam(500)
        }

        assertEquals("root:{SimpleComposable-${currentPlatform.name()}-500}", root.dump())
    }

    @Test
    fun testWithDefaultStringParam() = runTest {
        val root = composeText {
            WithDefaultStringParam("OverrideDefaultString")
        }

        assertEquals("root:{SimpleComposable-${currentPlatform.name()}-OverrideDefaultString}", root.dump())
    }

    @Test
    fun testTakesComposableLambdaWithDefaultIntNotExpect() = runTest {
        savedComposableLambda = null

        TakesComposableLambdaWithDefaultIntNotExpect(123123) {
            TextLeafNode("ABC")
        }

        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{Common-123123:{ABC}}", root.dump())
    }

    @Test
    fun testTakesComposableLambdaWithDefaultInt() = runTest {
        savedComposableLambda = null

        TakesComposableLambdaWithDefaultInt(56789) {
            TextLeafNode("ABC")
        }

        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{${currentPlatform.name()}-56789:{ABC}}", root.dump())
    }


    @Test
    fun testTakesComposableLambdaWithDefault() = runTest {
        savedComposableLambda = null

        TakesComposableLambdaWithDefault {
            TextLeafNode("OverrideDefault")
        }

        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{${currentPlatform.name()}:{OverrideDefault}}", root.dump())
    }

    @Test
    fun testExpectComposableDefaultValueProvidedByAnotherComposable() = runTest {
        val root = composeText {
            ExpectComposableDefaultValueProvidedByAnotherComposable("OverrideDefault") {
                TextLeafNode("$it+postfix")
            }
        }
        assertEquals("root:{${currentPlatform.name()}-OverrideDefault+postfix}", root.dump())
    }

    @Test
    fun testUseRememberInDefaultValueOfExpectFun() = runTest {
        val root = composeText {
            UseRememberInDefaultValueOfExpectFun(remember { "OverrideRememberDefault" }) {
                TextLeafNode(it)
            }
        }
        assertEquals("root:{${currentPlatform.name()}-OverrideRememberDefault}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameterAndDefaultLambda() = runTest {
        val root = composeText {
            ExpectWithTypeParameterAndDefaultLambda("3.1415") {
                "π=$it"
            }
        }
        assertEquals("root:{${currentPlatform.name()}:{π=3.1415}}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameterAndDefaultValue() = runTest {
        val root = composeText {
            ExpectWithTypeParameterAndDefaultComposableLambda("QWERTY") { s ->
                TextContainerNode("parent") {
                    TextLeafNode(s)
                }
            }
        }
        assertEquals("root:{${currentPlatform.name()}:{parent:{QWERTY}}}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameterInReturnAndDefaultComposableLambda() = runTest {
        val argument = mutableStateOf("aAbBcCdDeE")
        val job = Job()

        val root = composeText(coroutineContext + job) {
            val text = ExpectWithTypeParameterInReturnAndDefaultComposableLambda(argument.value) {
                it + " " + it.reversed()
            }
            TextLeafNode(text)
        }

        assertEquals("root:{aAbBcCdDeE EeDdCcBbAa}", root.dump())
        argument.value = "1123581321"

        testScheduler.advanceUntilIdle()
        assertEquals("root:{1123581321 1231853211}", root.dump())
        job.cancel()
    }
}