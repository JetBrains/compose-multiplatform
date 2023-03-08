import androidx.compose.runtime.mutableStateOf
import com.example.common.TextLeafNode
import my.abc.*
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TestExpectActuals {

    @Test
    fun simpleExpectComposable() = runTest {
        val root = composeText {
            SimpleComposable()
        }

        assertEquals("root:{SimpleComposable-${currentPlatform.name()}}", root.dump())
    }

    @Test
    fun testWithDefaultIntParam() = runTest {
        val root = composeText {
            WithDefaultIntParam()
        }

        assertEquals("root:{SimpleComposable-${currentPlatform.name()}-10}", root.dump())
    }

    @Test
    fun testWithDefaultStringParam() = runTest {
        val root = composeText {
            WithDefaultStringParam()
        }

        assertEquals("root:{SimpleComposable-${currentPlatform.name()}-defaultStringValue}", root.dump())
    }

    @Test
    fun testExpectTakesComposableLambda() = runTest {
        savedComposableLambda = null

        TakesComposableLambda {
            TextLeafNode("12345")
        }
        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{${currentPlatform.name()}:{12345}}", root.dump())
    }

    @Test
    fun testTakesComposableLambdaWithDefaultIntNotExpect() = runTest {
        savedComposableLambda = null

        TakesComposableLambdaWithDefaultIntNotExpect {
            TextLeafNode("ABC")
        }

        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{Common-100:{ABC}}", root.dump())
    }

    @Test
    fun testTakesComposableLambdaWithDefaultInt() = runTest {
        savedComposableLambda = null

        TakesComposableLambdaWithDefaultInt {
            TextLeafNode("ABC")
        }

        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{${currentPlatform.name()}-100:{ABC}}", root.dump())
    }

    @Test
    fun testTakesComposableLambdaWithDefault() = runTest {
        savedComposableLambda = null

        TakesComposableLambdaWithDefault()

        val root = composeText {
            savedComposableLambda!!.invoke()
        }

        assertEquals("root:{${currentPlatform.name()}:{Default}}", root.dump())
    }

    @Test
    fun testExpectComposableDefaultValueProvidedByAnotherComposable() = runTest {
        val root = composeText {
            ExpectComposableDefaultValueProvidedByAnotherComposable {
                TextLeafNode(it)
            }
        }
        assertEquals("root:{${currentPlatform.name()}-defaultStringValueComposable}", root.dump())
    }

    @Test
    fun testUseRememberInDefaultValueOfExpectFun() = runTest {
        val root = composeText {
            UseRememberInDefaultValueOfExpectFun {
                TextLeafNode(it)
            }
        }
        assertEquals("root:{${currentPlatform.name()}-defaultRememberedValue}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameter() = runTest {
        val root = composeText {
            ExpectWithTypeParameter("TTT") { s ->
                TextLeafNode(s)
            }
        }
        assertEquals("root:{${currentPlatform.name()}:{TTT}}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameterAndDefaultLambda() = runTest {
        val root = composeText {
            ExpectWithTypeParameterAndDefaultLambda("3.1415")
        }
        assertEquals("root:{${currentPlatform.name()}:{3.1415}}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameterAndDefaultValue() = runTest {
        val root = composeText {
            ExpectWithTypeParameterAndDefaultComposableLambda("QWERTY")
        }
        assertEquals("root:{${currentPlatform.name()}:{QWERTY}}", root.dump())
    }

    @Test
    fun testExpectWithTypeParameterInReturnAndDefaultComposableLambda() = runTest {
        val argument = mutableStateOf("aAbBcCdDeE")
        val job = Job()

        val root = composeText(coroutineContext + job) {
            val text = ExpectWithTypeParameterInReturnAndDefaultComposableLambda(argument.value)
            TextLeafNode(text)
        }

        assertEquals("root:{aAbBcCdDeE}", root.dump())
        argument.value = "1123581321"

        testScheduler.advanceUntilIdle()
        assertEquals("root:{1123581321}", root.dump())

        job.cancel()
    }
}
