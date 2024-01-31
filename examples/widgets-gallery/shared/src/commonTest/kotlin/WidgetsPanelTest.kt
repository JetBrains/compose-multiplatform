import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.jetbrains.compose.demo.widgets.ui.WidgetsPanel
import org.jetbrains.compose.demo.widgets.ui.WidgetsType
import org.jetbrains.compose.demo.widgets.ui.listItemTestTag
import kotlin.test.Test


@OptIn(ExperimentalTestApi::class)
class WidgetsPanelTest {
    /**
     * Tests that clicking on each widget type in the list shows the corresponding widgets view.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clickingOnWidgetListItemShowsCorrectWidgetUi() = runComposeUiTest {
        setContent {
            WidgetsPanel()
        }

        for (widgetsType in WidgetsType.entries) {
            onNodeWithTag(widgetsType.listItemTestTag).performClick()
            onNodeWithTag(widgetsType.testTag).assertExists()
        }
    }
}