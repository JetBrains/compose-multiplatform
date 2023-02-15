import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Tests {

    @Test
    fun testExample() = runTest {
        val root = composeText {
            SimpleComposable()
        }

        assertEquals("root:{SimpleComposable}", root.dump())
    }
}
