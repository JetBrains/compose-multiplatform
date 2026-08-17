package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class StringHtmlRecompositionTest {
    @Test
    fun insertsKeyedSiblingAtTheRequestedPosition() = withStringComposition { composition ->
        val items = mutableStateOf(listOf("A", "C"))
        composition.setContent { ItemList(items.value) }
        val initialNodes = composition.itemNodes()

        items.value = listOf("A", "B", "C")
        composition.recomposeAfter(items)
        val recomposedNodes = composition.itemNodes()

        assertEquals(
            "<div><span>A</span><span>B</span><span>C</span></div>",
            composition.toHtmlString(),
        )
        assertSame(initialNodes[0], recomposedNodes[0])
        assertSame(initialNodes[1], recomposedNodes[2])
    }

    @Test
    fun removesKeyedSiblingsAndClosesTheGap() = withStringComposition { composition ->
        val items = mutableStateOf(listOf("A", "B", "C", "D"))
        composition.setContent { ItemList(items.value) }
        val initialNodes = composition.itemNodes()

        items.value = listOf("A", "D")
        composition.recomposeAfter(items)
        val recomposedNodes = composition.itemNodes()

        assertEquals(
            "<div><span>A</span><span>D</span></div>",
            composition.toHtmlString(),
        )
        assertSame(initialNodes[0], recomposedNodes[0])
        assertSame(initialNodes[3], recomposedNodes[1])
    }

    @Test
    fun movesKeyedSiblingsWithoutChangingTheirContent() = withStringComposition { composition ->
        val items = mutableStateOf(listOf("A", "B", "C", "D", "E"))
        composition.setContent { ItemList(items.value) }
        val initialNodes = composition.itemNodes()

        items.value = listOf("A", "D", "E", "B", "C")
        composition.recomposeAfter(items)
        val recomposedNodes = composition.itemNodes()

        assertEquals(
            "<div><span>A</span><span>D</span><span>E</span><span>B</span><span>C</span></div>",
            composition.toHtmlString(),
        )
        listOf(0, 3, 4, 1, 2).forEachIndexed { recomposedIndex, initialIndex ->
            assertSame(initialNodes[initialIndex], recomposedNodes[recomposedIndex])
        }
    }
}

@Composable
private fun ItemList(items: List<String>) {
    Div {
        items.forEach { item ->
            key(item) {
                Span { Text(item) }
            }
        }
    }
}

private inline fun withStringComposition(block: (TestStringComposition) -> Unit) {
    val composition = TestStringComposition()
    try {
        block(composition)
    } finally {
        composition.dispose()
    }
}

private class TestStringComposition {
    private val root = StringHtmlElementNode.root()
    private val recomposer = Recomposer(Dispatchers.Default)
    private val composition = ControlledComposition(
        applier = StringHtmlApplier(StringHtmlNodeWrapper(root)),
        parent = recomposer,
    )

    fun setContent(content: @Composable () -> Unit) {
        composition.setContent {
            CompositionLocalProvider(
                LocalComposeHtmlContext provides StringComposeHtmlContext,
            ) {
                content()
            }
        }
    }

    fun recomposeAfter(changed: Any) {
        composition.recordModificationsOf(setOf(changed))
        if (composition.recompose()) {
            composition.applyChanges()
            composition.applyLateChanges()
        }
    }

    fun toHtmlString(): String = root.toHtmlString()

    fun itemNodes(): List<StringHtmlNode> =
        (root.children.single() as StringHtmlElementNode).children.toList()

    fun dispose() {
        composition.dispose()
        recomposer.close()
    }
}
