import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class MainTest {

    @Test
    fun testReversedTextView() = runComposeUiTest {
        setContent {
            ReversedTextView("Hello")
        }

        onNodeWithText("olleH").assertExists()
    }

}