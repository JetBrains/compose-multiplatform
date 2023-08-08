import androidx.compose.runtime.remember
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import my.abc.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TestExpectClass {

    @Test
    fun testExpectClass() = runTest {
        val root = composeText {
            val instance = remember { ExpectClass() }
            instance.ExpectComposableFunWithDefaultInt()
        }

        assertEquals("root:{${currentPlatform.name()}(i = 11011)}", root.dump())
    }

    @Test
    fun testExpectClassOverrideDefault() = runTest {
        val root = composeText {
            val instance = remember { ExpectClass() }
            instance.ExpectComposableFunWithDefaultInt(22122)
        }

        assertEquals("root:{${currentPlatform.name()}(i = 22122)}", root.dump())
    }

    @Test
    fun testExpectClassWithString() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithString() }
            instance.ExpectComposableFunWithDefaultComposableLambda("tTt")
        }

        assertEquals("root:{${currentPlatform.name()}(s = tTt)}", root.dump())
    }

    @Test
    fun testExpectClassWithStringOverrideDefault() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithString() }
            instance.ExpectComposableFunWithDefaultComposableLambda("tTt") {
                it.replace('T', 'A')
            }
        }

        assertEquals("root:{${currentPlatform.name()}(s = tAt)}", root.dump())
    }

    @Test
    fun testExpectClassWithStringProperty() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithStringProperty("aeouiyu") }
            instance.ExpectComposableFunWithDefaultComposableLambda()
        }

        assertEquals("root:{${currentPlatform.name()}(s = aeouiyu)}", root.dump())
    }

    @Test
    fun testExpectClassWithT() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithT<String>() }
            instance.ExpectComposableFunWithDefaultComposableLambda("tTt")
        }

        assertEquals("root:{${currentPlatform.name()}(t = tTt)}", root.dump())
    }

    @Test
    fun testExpectClassWithTProp() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithTProp("genby-[eqkj") }
            instance.ExpectComposableFunWithDefaultComposableLambda()
        }

        assertEquals("root:{${currentPlatform.name()}(tProp = genby-[eqkj)}", root.dump())
    }

    @Test
    fun testExpectClassWithTPropOverrideDefault() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithTProp("genby-[eqkj") }
            instance.ExpectComposableFunWithDefaultComposableLambda {
                "$it!!!"
            }
        }

        assertEquals(
            "root:{${currentPlatform.name()}(tProp = genby-[eqkj!!!)}",
            root.dump()
        )
    }

    @Test
    fun testExpectClassWithTProp2() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithTProp2("RWGFY") }
            instance.ExpectComposableFunWithDefaultComposableLambda()
        }

        assertEquals("root:{${currentPlatform.name()}(tProp = RWGFY)}", root.dump())
    }

    @Test
    fun testExpectClassWithTProp2OverrideDefault() = runTest {
        val root = composeText {
            val instance = remember { ExpectClassWithTProp2("RWGFY") }
            instance.ExpectComposableFunWithDefaultComposableLambda("COTTON!")
        }

        assertEquals("root:{${currentPlatform.name()}(tProp = COTTON!)}", root.dump())
    }
}