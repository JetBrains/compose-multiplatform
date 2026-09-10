import kotlinx.browser.document
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ComposeWebInternalApi::class)
class DomNodeWrapperTests {
    @Test
    fun movesMultipleNodesForward() {
        assertMove("ABCDEFG", from = 1, to = 4, count = 2, expected = "ADBCEFG")
    }

    @Test
    fun movesMultipleNodesForwardNearTheEnd() {
        assertMove("ABCDE", from = 1, to = 4, count = 2, expected = "ADBCE")
    }

    @Test
    fun movesSingleNodeToTheEnd() {
        assertMove("ABCDE", from = 1, to = 5, count = 1, expected = "ACDEB")
    }

    private fun assertMove(initial: String, from: Int, to: Int, count: Int, expected: String) {
        val root = document.createElement("div")
        initial.forEach { label ->
            root.appendChild(document.createElement("span").apply {
                textContent = label.toString()
            })
        }

        DomNodeWrapper(root).move(from, to, count)

        assertEquals(expected, root.textContent)
    }
}
