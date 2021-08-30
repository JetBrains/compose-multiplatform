import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.foundation.layout.Box
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.background
import org.jetbrains.compose.common.ui.size
import org.jetbrains.compose.common.ui.unit.dp
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierTests {
    @Test
    fun backgroundModifier() = runTest {
         {
            Box(
                Modifier.background(Color(255, 0, 0))
            ) { }
        }

        assertEquals("background-color: rgb(255, 0, 0);", nextChild().style.cssText)
    }

    @Test
    fun size() = runTest {
        composition {
            Box(
                Modifier.size(40.dp)
            ) { }
        }

        assertEquals("width: 40px; height: 40px;", nextChild().style.cssText)
    }
}
