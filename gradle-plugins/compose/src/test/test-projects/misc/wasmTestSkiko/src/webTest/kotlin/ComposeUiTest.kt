import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

class ComposeUiTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun runsComposeUiTest() = runComposeUiTest {}
}
