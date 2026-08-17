package org.jetbrains.compose.web.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringHtmlApplierTest {
    @Test
    fun appliesInsertRemoveAndMoveOperations() {
        val root = StringHtmlElementNode.root()
        val wrapper = StringHtmlNodeWrapper(root)

        listOf("A", "B", "C", "D", "E").forEachIndexed { index, value ->
            wrapper.insert(index, StringHtmlNodeWrapper(StringHtmlTextNode(value)))
        }
        wrapper.remove(index = 3, count = 1)
        wrapper.move(from = 1, to = 4, count = 2)

        assertEquals("AEBC", root.toHtmlString())
    }

    @Test
    fun appliesNestedApplierOperations() {
        val root = StringHtmlElementNode.root()
        val applier = StringHtmlApplier(StringHtmlNodeWrapper(root))
        val div = StringHtmlNodeWrapper(StringHtmlElementNode("div"))

        applier.insertBottomUp(0, div)
        applier.down(div)
        applier.insertBottomUp(0, StringHtmlNodeWrapper(StringHtmlTextNode("content")))
        applier.up()

        assertEquals("<div>content</div>", root.toHtmlString())
    }

    @Test
    fun movesSingleNodeForward() {
        val (root, wrapper) = textChildren("A", "B", "C", "D", "E")

        // `to` is an index in the original list. Move B before the original E.
        wrapper.move(from = 1, to = 4, count = 1)

        assertEquals("ACDBE", root.toHtmlString())
    }

    @Test
    fun movesSingleNodeBackward() {
        val (root, wrapper) = textChildren("A", "B", "C", "D", "E")

        wrapper.move(from = 3, to = 1, count = 1)

        assertEquals("ADBCE", root.toHtmlString())
    }

    @Test
    fun movesConsecutiveNodesAsOneOrderedBlock() {
        val (root, wrapper) = textChildren("A", "B", "C", "D", "E", "F")

        // Move [B, C] before the original F while preserving B-before-C order.
        wrapper.move(from = 1, to = 5, count = 2)

        assertEquals("ADEBCF", root.toHtmlString())
    }

    @Test
    fun movingZeroNodesDoesNothing() {
        val (root, wrapper) = textChildren("A", "B", "C")

        wrapper.move(from = 1, to = 3, count = 0)

        assertEquals("ABC", root.toHtmlString())
    }

    @Test
    fun rejectsChildrenOnTextNodes() {
        val wrapper = StringHtmlNodeWrapper(StringHtmlTextNode("text"))

        val exception = assertFailsWith<IllegalStateException> {
            wrapper.insert(0, StringHtmlNodeWrapper(StringHtmlTextNode("child")))
        }

        assertEquals("Cannot perform 'insert' on a text node", exception.message)
    }

    private fun textChildren(
        vararg values: String
    ): Pair<StringHtmlElementNode, StringHtmlNodeWrapper> {
        val root = StringHtmlElementNode.root()
        val wrapper = StringHtmlNodeWrapper(root)
        values.forEachIndexed { index, value ->
            wrapper.insert(index, StringHtmlNodeWrapper(StringHtmlTextNode(value)))
        }
        return root to wrapper
    }
}
