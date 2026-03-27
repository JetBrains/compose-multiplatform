import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import app.group.resources_test.generated.resources.Res
import app.group.resources_test.generated.resources.app_name
import app.group.resources_test.generated.resources.test_string
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class CommonUiTest {

    @Test
    fun checkTestResources() = runComposeUiTest {
        setContent {
            val mainStr = stringResource(Res.string.app_name)
            val testStr = stringResource(Res.string.test_string)
            assertEquals("Compose Resources App", mainStr)
            assertEquals("Common test", testStr)
        }
    }

    @Test
    fun checkTestFileResource() = runTest {
        val commonFile = Res.readBytes("files/common.txt").decodeToString()
        assertEquals("common 777", commonFile)
        val testFile = Res.readBytes("files/data.txt").decodeToString()
        assertEquals("1234567890", testFile)
    }

}