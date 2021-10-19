package org.jetbrains.compose.codeeditor.codecompletion

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.codeeditor.CodeCompletionElement
import org.jetbrains.compose.codeeditor.codecompletion.CodeCompletionListState.RequiredAction
import org.jetbrains.compose.codeeditor.editor.text.Prefix
import org.jetbrains.compose.codeeditor.keyevent.KeyEventHandler
import org.jetbrains.compose.codeeditor.keyevent.KeyEventHandlerImpl

@Stable
internal class CodeCompletionState(
    private val scope: CoroutineScope,
    private val paste: (Int, CodeCompletionElement) -> Unit
) {
    companion object {
        private const val autosuggestionDelay = 800L
    }

    private var isRunning = false
    var isLoading by mutableStateOf(false)
    var isVisible by mutableStateOf(false)

    var ccListState = CodeCompletionListState()
    private var pasteOffset: Int = 0
    var popupPosition: IntOffset = IntOffset.Zero
        private set

    private var job: Job? = null
    private var autosuggestionJob: Job? = null
    private var isAutosuggestion = false
    private var restart = false
    private var restartWithDelay = false

    fun start(
        autosuggestion: Boolean = false,
        getPrefix: () -> Prefix,
        getList: () -> List<CodeCompletionElement>
    ) {
        if (isRunning) {
            if (!isVisible) {
                isAutosuggestion = autosuggestion
                isVisible = true
            }
            return
        }

        isRunning = true
        isLoading = true
        isVisible = true

        setParametersFromPrefix(getPrefix)
        isAutosuggestion = autosuggestion

        job = scope.launch {
            loadCodeCompletionList(getList)
        }
    }

    fun startWithDelay(
        autosuggestion: Boolean = true,
        getPrefix: () -> Prefix,
        getList: () -> List<CodeCompletionElement>
    ) {
        if (isRunning) return

        isRunning = true
        isLoading = true

        setParametersFromPrefix(getPrefix)
        isAutosuggestion = autosuggestion

        autosuggestionJob = scope.launch {
            launch {
                delay(autosuggestionDelay)
                if (isRunning) isVisible = true
            }
            loadCodeCompletionList(getList)
        }
    }

    private fun setParametersFromPrefix(getPrefix: () -> Prefix) {
        val prefix = getPrefix()
        pasteOffset = prefix.offset
        popupPosition = prefix.position
        ccListState.setInitialPrefix(prefix.prefix)
    }

    fun restart(getPrefix: () -> Prefix, getList: () -> List<CodeCompletionElement>) {
        if (restart) {
            restart = false
            start(isAutosuggestion, getPrefix, getList)
        } else if (restartWithDelay) {
            restartWithDelay = false
            startWithDelay(isAutosuggestion, getPrefix, getList)
        }
    }

    private suspend fun loadCodeCompletionList(getList: () -> List<CodeCompletionElement>) {
        coroutineScope {
            val list = withContext(Dispatchers.IO) { mapCodeCompletionList(getList()) }
            isLoading = false
            ccListState.setInitialList(list)
            if (ccListState.noSuggestions && isAutosuggestion) hide()
        }
    }

    private fun mapCodeCompletionList(list: List<CodeCompletionElement>): List<CodeCompletionIndexedElement> {
        return list.mapIndexed { id, element ->
            CodeCompletionIndexedElement(
                id = id,
                element = element,
                onClick = {
                    ccListState.select(id)
                },
                onDoubleClick = {
                    ccListState.select(id)
                    paste(pasteOffset, ccListState.getSelected())
                    hide()
                }
            )
        }
    }

    fun hide() {
        if (isRunning) {
            isVisible = false
            isRunning = false
            isLoading = false
            job?.cancel()
            autosuggestionJob?.cancel()
            job = null
            autosuggestionJob = null
            pasteOffset = 0
            ccListState.clear()
        }
    }

    private fun checkStateAndRun(action: () -> Unit): () -> Boolean = {
        if (isVisible && !isLoading && !ccListState.noSuggestions) {
            action()
            true
        } else {
            hide()
            false
        }
    }

    private val previewKeyEventHandler = KeyEventHandlerImpl("CodeCompletion preview")
        .onNavigationKey(
            up = checkStateAndRun(ccListState::up),
            down = checkStateAndRun(ccListState::down),
            pageUp = checkStateAndRun(ccListState::pageUp),
            pageDown = checkStateAndRun(ccListState::pageDown),
            home = checkStateAndRun(ccListState::home),
            end = checkStateAndRun(ccListState::end),
            hide = this::hide
        )
        .onEnter(
            consume = { isVisible && !isLoading && !ccListState.noSuggestions },
            action = {
                paste(pasteOffset, ccListState.getSelected())
                hide()
                true
            },
            hide = this::hide
        )
        .onEscape(this::hide)
        .onTab(this::hide)

    private val preKeyEventHandler = KeyEventHandlerImpl("CodeCompletion pre")
        .onBackspace {
            when (ccListState.truncatePrefix()) {
                RequiredAction.HIDE -> {
                    hide()
                    true
                }

                RequiredAction.RELOAD -> {
                    if (isVisible) {
                        restart = true
                    } else {
                        restartWithDelay = true
                    }
                    hide()
                    false
                }

                RequiredAction.NOTHING -> {
                    true
                }
            }
        }
        .onCharacter { ch ->
            if (!isLoading && ccListState.noSuggestions) {
                hide()
                false
            } else if (ch.isLetterOrDigit() || ch == '_') {
                ccListState.appendPrefix(ch)
                if (!isLoading && ccListState.noSuggestions) {
                    hide()
                }
                true
            } else {
                false
            }
        }

    private val postKeyEventHandler = KeyEventHandlerImpl("CodeCompletion post")
        .onAnyOtherKeyExceptCharactersAndBackspace(
            noSuggestions = { !isLoading && ccListState.noSuggestions },
            hide = this::hide
        )

    private fun getEventHandler(eventHandler: KeyEventHandler): KeyEventHandler = KeyEventHandler {
        if (isRunning) eventHandler.onKeyEvent(it)
        else false
    }

    fun getPreviewKeyEventHandler() = getEventHandler(previewKeyEventHandler)
    fun getPreKeyEventHandler() = getEventHandler(preKeyEventHandler)
    fun getPostKeyEventHandler() = getEventHandler(postKeyEventHandler)

}
