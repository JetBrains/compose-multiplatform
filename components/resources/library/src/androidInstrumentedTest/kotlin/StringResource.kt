import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
fun getStringResource(@StringRes resId: Int) = runComposeUiTest {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val expectString = context.getString(resId)
    var actualString by mutableStateOf("")
    setContent {
         actualString = stringResource(resId)
    }
    assertEquals(expectString, actualString)
}

