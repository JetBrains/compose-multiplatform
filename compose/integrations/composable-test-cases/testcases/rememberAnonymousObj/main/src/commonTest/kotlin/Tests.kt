import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Tests {

    @Test
    fun testCallsRememberAnonymousObject() = runTest {
        val root = composeText {
            CallsRememberAnonymousObject()
        }

        assertEquals("root:{obj1}", root.dump())
    }

    @Test
    fun testCallsRememberAnonymousObjectImplInterface() = runTest {
        val root = composeText {
            CallsRememberAnonymousObjectImplInterface()
        }

        assertEquals("root:{obj2}", root.dump())
    }

    @Test
    fun testCallsRememberAnonymousObjectExplicitType() = runTest {
        val root = composeText {
            CallsRememberAnonymousObjectExplicitType()
        }

        assertEquals("root:{obj3}", root.dump())
    }

    @Test
    fun testCallsRememberAnonymousObjectExplicitType2() = runTest {
        val root = composeText {
            CallsRememberAnonymousObjectExplicitType2()
        }

        assertEquals("root:{obj4}", root.dump())
    }
}
