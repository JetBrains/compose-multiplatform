package org.jetbrains.compose.codeeditor.codecompletion

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.codeeditor.CodeCompletionElement
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionListState.ScrollType.DOWN
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionListState.ScrollType.FILTER
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionListState.ScrollType.PAGE_DOWN
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionListState.ScrollType.PAGE_UP
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionListState.ScrollType.UP
import org.jetbrains.compose.codeeditor.codecompletion.filters.Contains
import org.jetbrains.compose.codeeditor.codecompletion.filters.Matches
import org.jetbrains.compose.codeeditor.codecompletion.filters.StartsWith
import kotlin.math.max
import kotlin.math.min

@Stable
internal class CodeCompletionListState {
    companion object {
        // todo add calculation of page size by line height and list height
        private const val MAX_PAGE_SIZE = 8
        private const val NUM_VISIBLE_TOP_ELEMENTS = 2
    }

    var scope: CoroutineScope? = null

    private var initialList = emptyList<CodeCompletionIndexedElement>()
    var list = mutableStateListOf<CodeCompletionIndexedElement>()
        private set

    var lazyListState = LazyListState()
        private set

    var noSuggestions by mutableStateOf(false)

    private var elementOffset = 0
    private var selectedId = 0
    private var explicitSelect = false

    private var prefix = ""
    private var initialPrefix = ""
    private var closeOnEmptyPrefix = true
    private val filters = listOf(Matches(), StartsWith(), Contains())

    fun clear() {
        scope?.cancel()
        scope = null
        initialList = emptyList()
        list.clear()
        noSuggestions = false
        elementOffset = 0
        selectedId = 0
        explicitSelect = false
        prefix = ""
        initialPrefix = ""
        closeOnEmptyPrefix = true
    }

    fun setInitialList(list: List<CodeCompletionIndexedElement>) {
        initialList = list
        filter()
    }

    fun setInitialPrefix(prefix: String) {
        if (prefix.isNotEmpty() && prefix[0] == '.') { // dot operator. todo: add other operators like ::
            closeOnEmptyPrefix = false
            initialPrefix = prefix.substring(1)
        } else {
            closeOnEmptyPrefix = true
            initialPrefix = prefix
        }
        this.prefix = initialPrefix
    }

    private fun filter() {
        if (initialList.isEmpty()) {
            noSuggestions = true
            return
        }

        var savedSelectedElement: CodeCompletionIndexedElement? = null
        if (list.isNotEmpty()) {
            if (explicitSelect) {
                savedSelectedElement = list[selectedId]
            } else {
                list[selectedId].unselect()
                selectedId = 0
            }
        }

        val result = LinkedHashSet<CodeCompletionIndexedElement>()
        filters.forEach {
            result.addAll(initialList.filter { element ->
                it.matches(element, prefix, false)
            })
        }

        filters.forEach {
            result.addAll(initialList.filter { element ->
                it.matches(element, prefix, true)
            })
        }

        list.clear()
        list.addAll(result)

        if (list.isNotEmpty()) {
            noSuggestions = false
            if (explicitSelect) {
                savedSelectedElement?.let {
                    val indexOf = list.indexOf(savedSelectedElement)
                    if (indexOf != -1) {
                        selectedId = indexOf
                    } else {
                        savedSelectedElement.unselect()
                        selectedId = 0
                        explicitSelect = false
                    }
                }
            }
            list[selectedId].select()
            if (scope != null) {
                scrollTo(FILTER)
            }
        } else {
            noSuggestions = true
            savedSelectedElement?.unselect()
            selectedId = 0
            explicitSelect = false
        }
    }

    fun appendPrefix(char: Char) {
        prefix += char
        filter()
    }

    /**
     * returns the required action for the list
     */
    fun truncatePrefix(): RequiredAction {
        return if (prefix.isEmpty()) { // nothing to truncate
            RequiredAction.HIDE
        } else {
            prefix = prefix.dropLast(1)
            if (prefix.isEmpty()) {
                if (closeOnEmptyPrefix) {
                    RequiredAction.HIDE
                } else { // dot operator
                    if (initialPrefix.isEmpty()) { // started from the dot
                        filter()
                        RequiredAction.NOTHING
                    } else { // need to reload
                        RequiredAction.RELOAD
                    }
                }
            } else if (prefix.length >= initialPrefix.length) { // we can still filter the list
                filter()
                RequiredAction.NOTHING
            } else { // need to reload
                RequiredAction.RELOAD
            }
        }
    }

    fun up() {
        if (selectedId > 0) {
            selectElementAndScrollTo(selectedId - 1, UP)
        }
    }

    fun down() {
        if (selectedId < list.size - 1) {
            selectElementAndScrollTo(selectedId + 1, DOWN)
        }
    }

    fun pageUp() {
        turnPage(max(selectedId - MAX_PAGE_SIZE, 0), PAGE_UP)
    }

    fun pageDown() {
        turnPage(min(selectedId + MAX_PAGE_SIZE, list.size - 1), PAGE_DOWN)
    }

    fun home() {
        turnPage(0, UP)
    }

    fun end() {
        turnPage(list.size - 1, DOWN)
    }

    private fun turnPage(newSelectedId: Int, scrollType: ScrollType) {
        if (newSelectedId != selectedId) {
            selectElementAndScrollTo(newSelectedId, scrollType)
        }
    }

    private fun selectElementAndScrollTo(newSelectedId: Int, scrollType: ScrollType) {
        list[selectedId].unselect()
        selectedId = newSelectedId
        list[selectedId].select()
        explicitSelect = true
        scrollTo(scrollType)
    }

    private fun scrollTo(scrollType: ScrollType) {
        val layoutInfo = lazyListState.layoutInfo
        val pageSize = min(MAX_PAGE_SIZE, list.size)
        val elementToScroll = when (scrollType) {
            UP -> {
                val topVisibleId = max(selectedId - NUM_VISIBLE_TOP_ELEMENTS, 0)
                if (isVisible(layoutInfo, selectedId)) {
                    if (isVisible(layoutInfo, topVisibleId)) {
                        -1
                    } else {
                        topVisibleId
                    }
                } else {
                    minTopId(selectedId, pageSize)
                }
            }

            DOWN -> {
                if (isVisible(layoutInfo, selectedId)) {
                    -1
                } else {
                    minTopId(selectedId, pageSize)
                }
            }

            PAGE_UP -> {
                max(selectedId - elementOffset, 0)
            }

            PAGE_DOWN -> {
                min(selectedId - elementOffset, list.size - 1)
            }

            FILTER -> {
                minTopId(selectedId, pageSize)
            }
        }

        elementOffset = if (elementToScroll == -1) {
            selectedId - lazyListState.firstVisibleItemIndex
        } else {
            selectedId - min(elementToScroll, list.size - pageSize)
        }
        if (elementToScroll != -1) {
            scope?.launch { lazyListState.scrollToItem(elementToScroll) }
        }
    }

    private fun isVisible(layoutInfo: LazyListLayoutInfo, elementId: Int): Boolean {
        val element = layoutInfo.visibleItemsInfo.firstOrNull { it.index == elementId }
        return element != null
            && element.offset >= layoutInfo.viewportStartOffset
            && element.offset + element.size <= layoutInfo.viewportEndOffset
    }

    /**
     * The minimum identifier of the top element, such that the specified element is visible
     */
    private fun minTopId(id: Int, pageSize: Int) = max(id - pageSize + 1, 0)

    fun getSelected(): CodeCompletionElement {
        return list[selectedId].element
    }

    fun select(elementId: Int) {
        if (list.isNotEmpty()) {
            unselect()
            for ((listId, element) in list.withIndex()) {
                if (element.id == elementId) {
                    selectedId = listId
                    element.select()
                    break
                }
            }
        }
    }

    private fun unselect() {
        if (list.isNotEmpty()) {
            list[selectedId].unselect()
            selectedId = 0
        }
    }

    fun isMaxPageSizeExceeded(): Boolean = list.size > MAX_PAGE_SIZE

    private enum class ScrollType {
        UP, DOWN, PAGE_UP, PAGE_DOWN, FILTER
    }

    enum class RequiredAction {
        NOTHING, HIDE, RELOAD
    }
}
