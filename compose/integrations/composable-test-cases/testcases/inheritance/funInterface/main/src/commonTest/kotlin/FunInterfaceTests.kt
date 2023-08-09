import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FunInterfaceTests {

    @Test
    fun testFunInterfaceWithComposable() = runTest {
        val root = composeText {
            funInterfaceWithComposable.content()
        }

        assertEquals("root:{FunInterfaceWithComposable}", root.dump())

        val classImplFunInterface = ClassImplementingFunInterface()

        val root2 = composeText {
            classImplFunInterface.content()
        }

        assertEquals("root:{ClassImplementingFunInterface}", root2.dump())
    }

    @Test
    fun testFunInterfaceReturnComposable() = runTest {
        val root = composeText {
            funInterfaceReturnComposable.getContent().invoke()
        }

        assertEquals("root:{FunInterfaceReturnComposable}", root.dump())
    }

    @Test
    fun testFunInterfaceReturnComposable2() = runTest {
        val impl = ClassImplementingFunInterface2()
        val root = composeText {
            impl.getContent().invoke()
        }

        assertEquals("root:{ClassImplementingFunInterface2}", root.dump())
    }

    @Test
    fun testFunInterfaceComposableReturnsComposable() = runTest {
        val impl = ClassImplFunInterfaceComposableReturnsComposable()
        val root = composeText {
            impl.getContent().invoke()
        }

        assertEquals("root:{ClassImplFunInterfaceComposableReturnsComposable}", root.dump())
    }

}
