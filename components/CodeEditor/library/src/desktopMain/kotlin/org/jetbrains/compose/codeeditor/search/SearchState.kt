package org.jetbrains.compose.codeeditor.search

import org.jetbrains.compose.codeeditor.AppTheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.jetbrains.compose.codeeditor.editor.draw.DrawState
import org.jetbrains.compose.codeeditor.editor.text.TextState
import org.jetbrains.compose.codeeditor.keyevent.KeyEventHandler
import org.jetbrains.compose.codeeditor.keyevent.KeyEventHandlerImpl

@Stable
internal class SearchState(
    private val textState: TextState,
    private val drawState: DrawState
) {

    var isVisible by mutableStateOf(false)
        private set

    var searchString by mutableStateOf("")
        private set

    private var searchFieldSelection by mutableStateOf(TextRange.Zero)

    val fieldValue by derivedStateOf {
        TextFieldValue(searchString, searchFieldSelection)
    }

    private val searchResults by derivedStateOf {
        if (isVisible) {
            selectedResultIndex = -1
            textState.getTextRangesOf(searchString)
        } else emptyList()
    }

    val notFound by derivedStateOf { searchString.isNotEmpty() && searchResults.isEmpty() }
    val resultIsNotEmpty by derivedStateOf { searchResults.isNotEmpty() }

    private var selectedResultIndex by mutableStateOf(-1)

    val selectedResultIndexByCaret by derivedStateOf {
        if (textState.selection.collapsed) {
            val offset = textState.selection.start
            searchResults.indexOfFirst { it.start <= offset && offset <= it.end }
        } else {
            -1
        }
    }

    private var reachedEnd = false
    private var selectResultOnFocus = true
    private var firstCallAfterShow = true

    val status by derivedStateOf {
        if (searchResults.isNotEmpty()) {
            if (selectedResultIndex == -1) {
                "${searchResults.size} results"
            } else {
                "${selectedResultIndex + 1}/${searchResults.size}"
            }
        } else "0 results"
    }

    val focusRequester = FocusRequester()
    var focusBackRequester: FocusRequester? = null

    private val searchDrawer = drawState.createBackgroundDrawer(
        AppTheme.colors.searchSelectionFillColor,
        AppTheme.colors.searchSelectionStrokeColor,
        1
    )

    private val selectedResultDrawer = drawState.createBackgroundDrawer(
        borderColor = AppTheme.colors.searchSelectionActiveStrokeColor,
        zIndex = 20
    )

    init {
        drawState.putLineSegments(searchDrawer,
            derivedStateOf {
                searchResults.flatMap { // todo optimize it
                    drawState.getLineSegments(it.start, it.end)
                }
            }
        )
        drawState.putLineSegments(selectedResultDrawer,
            derivedStateOf {
                if (selectedResultIndex != -1 && searchResults.isNotEmpty()) {
                    val selectedResult = searchResults[selectedResultIndex]
                    drawState.getLineSegments(selectedResult.start, selectedResult.end)
                } else {
                    emptyList()
                }
            }
        )
    }

    fun onFieldValueChange(textFieldValue: TextFieldValue) {
        searchString = textFieldValue.text
        searchFieldSelection = textFieldValue.selection
    }

    fun onSelectedResultIndexByCaretChange() {
        if (selectedResultIndexByCaret != -1) selectedResultIndex = selectedResultIndexByCaret
    }

    fun show(searchString: String, focusBackRequester: FocusRequester? = null) {
        this.focusBackRequester = focusBackRequester
        if (isVisible) {
            firstCallAfterShow = false
            if (searchString.isEmpty()) {
                selectResultOnFocus = false
            } else {
                if (this.searchString == searchString) {
                    selectResultOnFocus = true
                } else {
                    selectResultOnFocus = false
                    this.searchString = searchString
                }
            }
            requestFocusAndSelect()
        } else {
            firstCallAfterShow = true
            if (searchString.isEmpty()) {
                selectResultOnFocus = false
            } else {
                selectResultOnFocus = true
                this.searchString = searchString
            }
            isVisible = true
        }
    }

    fun nextOnFocus() {
        if (!selectResultOnFocus || searchResults.isEmpty()) return
        val nextIndex = getNextOrCurrentIndex()
        selectIndex(nextIndex, 0, true)
    }

    fun nextOnInput() {
        if (firstCallAfterShow) {
            firstCallAfterShow = false
            return
        }
        if (searchString.isEmpty() || searchResults.isEmpty()) {
            textState.unselect()
            return
        }
        val nextIndex = getNextOrCurrentIndex()
        selectIndex(nextIndex, 0, true)
    }

    fun next() {
        if (searchResults.isEmpty()) return
        val nextIndex = getNextIndex()
        selectIndex(nextIndex, 0)
    }

    fun prev() {
        if (searchResults.isEmpty()) return
        val prevIndex = getPrevIndex()
        selectIndex(prevIndex, searchResults.size - 1)
    }

    private fun getNextIndex(): Int {
        val caretOffset = textState.selection.end
        val nextIndex = searchResults.indexOfFirst {
            it.end > caretOffset
        }
        return nextIndex
    }

    private fun getNextOrCurrentIndex(): Int {
        val caretOffset = textState.selection.start
        val nextIndex = searchResults.indexOfFirst {
            it.end >= caretOffset
        }
        return nextIndex
    }

    private fun getPrevIndex(): Int {
        val caretOffset = textState.selection.start
        val prevIndex = searchResults.indexOfLast {
            it.start < caretOffset
        }
        return prevIndex
    }

    private fun selectIndex(index: Int, afterEndIndex: Int, ignoreEnd: Boolean = false) {
        val idx = if (index != -1) {
            index
        } else {
            if (reachedEnd || ignoreEnd) {
                afterEndIndex
            } else {
                reachedEnd = true
                return
            }
        }
        reachedEnd = false
        selectedResultIndex = idx
        textState.selectTextRange(searchResults[selectedResultIndex])
    }

    fun close() {
        isVisible = false
        focusBackRequester?.requestFocus()
    }

    fun requestFocusAndSelect() {
        focusRequester.requestFocus()
        searchFieldSelection = TextRange(0, searchString.length)
    }

    val previewInnerKeyEventHandler = KeyEventHandlerImpl("Search inner preview")
        .onEscape(this::close)
        .onCtrlEnter { focusBackRequester?.requestFocus() }
        .onF3(this::next)
        .onShiftF3(this::prev)
        .onEnter(this::next)
        .onShiftEnter(this::prev)
        .onDown(this::next)
        .onUp(this::prev)

    private val previewOuterKeyEventHandler = KeyEventHandlerImpl("Search outer preview")
        .onEscape(this::close)
        .onF3(this::next)
        .onShiftF3(this::prev)

    private fun getEventHandler(eventHandler: KeyEventHandler): KeyEventHandler = KeyEventHandler {
        if (isVisible) eventHandler.onKeyEvent(it)
        else false
    }

    fun getPreviewKeyEventHandler() = getEventHandler(previewOuterKeyEventHandler)
}
