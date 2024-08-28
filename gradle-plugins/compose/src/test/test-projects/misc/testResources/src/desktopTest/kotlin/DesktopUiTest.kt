import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import app.group.resources_test.generated.resources.Res
import app.group.resources_test.generated.resources.app_name
import app.group.resources_test.generated.resources.desktop_str
import app.group.resources_test.generated.resources.desktop_test_str
import app.group.resources_test.generated.resources.test_string
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DesktopUiTest {

    @Test
    fun checkTestResources() = runComposeUiTest {
        setContent {
            val mainStr = stringResource(Res.string.app_name)
            val testStr = stringResource(Res.string.test_string)
            val desktopMainStr = stringResource(Res.string.desktop_str)
            val desktopTestStr = stringResource(Res.string.desktop_test_str)
            assertEquals("Compose Resources App", mainStr)
            assertEquals("Common test", testStr)
            assertEquals("Desktop string", desktopMainStr)
            assertEquals("Desktop test string", desktopTestStr)
        }
    }

}