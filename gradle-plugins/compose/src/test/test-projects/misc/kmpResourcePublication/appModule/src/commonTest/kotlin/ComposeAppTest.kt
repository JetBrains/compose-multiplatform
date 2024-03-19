import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import me.sample.app.App
import me.sample.app.MyFeatureText
import me.sample.library.MyLibraryText
import org.jetbrains.compose.resources.stringResource
import kmpresourcepublication.appmodule.generated.resources.*
import me.sample.library.resources.Res as LibRes
import me.sample.library.resources.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ComposeAppTest {
    @Test
    fun checkApp() = runComposeUiTest {
        val txt = "test text: "
        setContent {
            Column {
                App()

                Text(
                    modifier = Modifier.testTag("app-text"),
                    text = txt + stringResource(Res.string.str_1)
                )
                MyFeatureText(Modifier.testTag("feature-text"), txt)
                MyLibraryText(Modifier.testTag("library-text"), txt)

                //direct read a resource from library
                Text(
                    modifier = Modifier.testTag("library-resource-text"),
                    text = stringResource(LibRes.string.str_1)
                )
            }
        }

        onNodeWithTag("app-text").assertTextEquals("test text: App text str_1")
        onNodeWithTag("feature-text").assertTextEquals("test text: Feature text str_1")
        onNodeWithTag("library-text").assertTextEquals("test text: Library text str_1")
        onNodeWithTag("library-resource-text").assertTextEquals("Library text str_1")
    }
}