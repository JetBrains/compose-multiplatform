import kotlinx.browser.document
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.DomRangeNodeWrapper
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

    @Test
    fun logicalMovesAndRemovalRespectElementAndRangeParents() {
        for (rangeParent in listOf(false, true)) {
            val host = document.createElement("div")
            host.appendChild(document.createTextNode("before"))
            host.appendChild(document.createTextNode("after"))
            val parent = if (rangeParent) DomRangeNodeWrapper()
                else DomNodeWrapper(document.createElement("div"))
            DomNodeWrapper(host).insert(1, parent)

            fun range(label: String): DomRangeNodeWrapper = DomRangeNodeWrapper().apply {
                insert(0, DomNodeWrapper(document.createTextNode(label)))
                insert(1, DomNodeWrapper(document.createTextNode(label.lowercase())))
            }

            parent.insert(0, DomNodeWrapper(document.createTextNode("A")))
            parent.insert(1, range("B"))
            parent.insert(2, DomNodeWrapper(document.createTextNode("C")))
            parent.insert(3, range("D"))
            assertEquals("beforeABbCDdafter", host.textContent)

            parent.move(from = 1, to = 4, count = 2)
            assertEquals("beforeADdBbCafter", host.textContent)
            parent.move(from = 1, to = 0, count = 2)
            assertEquals("beforeDdBbACafter", host.textContent)
            parent.remove(index = 0, count = 2)
            assertEquals("beforeACafter", host.textContent)
            parent.insert(2, range("E"))
            assertEquals("beforeACEeafter", host.textContent)
            parent.clear()
            assertEquals("beforeafter", host.textContent)
        }
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
