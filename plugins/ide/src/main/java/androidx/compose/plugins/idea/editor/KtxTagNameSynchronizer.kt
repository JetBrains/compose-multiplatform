package androidx.compose.plugins.idea.editor

import com.intellij.codeInsight.editorActions.XmlTagNameSynchronizer
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.util.SmartList
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import androidx.compose.plugins.idea.parentOfType

private val LOG = Logger.getInstance(XmlTagNameSynchronizer::class.java)
private val SYNCHRONIZER_KEY = Key.create<TagNameSynchronizer>("ktx.tag_name_synchronizer")
private val Document.synchronizers get() =
    EditorFactory.getInstance().getEditors(this).mapNotNull {
        it.getUserData(SYNCHRONIZER_KEY)
    }

/**
 * Much of this file was modeled after the XML tag name synchronizer, which can be found here:
 *
 * https://github.com/JetBrains/intellij-community/blob/6eee222d1cda938673813a809ee5a8550baed005/xml/impl/src/com/intellij/codeInsight/editorActions/XmlTagNameSynchronizer.java
 */
class KtxTagNameSynchronizer(
    editorFactory: EditorFactory,
    private val manager: FileDocumentManager
) : CommandListener {

    init {
        editorFactory.addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorReleased(e: EditorFactoryEvent) = uninstall(e.editor)
            override fun editorCreated(e: EditorFactoryEvent) = install(e.editor)
        }, ApplicationManager.getApplication())

        @Suppress("DEPRECATION")
        CommandProcessor.getInstance().addCommandListener(this)
    }

    private fun install(editor: Editor) {
        val project = editor.project ?: return

        val document = editor.document
        val file = manager.getFile(document) ?: return
        if (!file.isValid) return
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return

        if (psiFile.language == KotlinLanguage.INSTANCE) {
            TagNameSynchronizer(editor, project)
        }
    }

    private fun uninstall(@Suppress("UNUSED_PARAMETER") editor: Editor) {
        // Nothing to do here since we pass in itself as the disposable
    }

    override fun beforeCommandFinished(event: CommandEvent) {
        event.document?.synchronizers?.forEach { it.beforeCommandFinished() }
    }
}

private class TagNameSynchronizer(val editor: Editor, val project: Project) : DocumentListener {

    companion object {
        private fun Char.isValidTagNameChar() =
            isLetter() || isDigit() || this == '.' || this == '_'
    }

    private enum class State {
        INITIAL,
        TRACKING,
        APPLYING
    }

    private var manager = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase
    private var state = State.INITIAL
    private val markers = SmartList<Couple<RangeMarker>>()

    init {
        editor.document.addDocumentListener(this, (editor as EditorImpl).disposable)
        editor.putUserData(SYNCHRONIZER_KEY, this)
    }

    override fun beforeDocumentChange(event: DocumentEvent) {

        val document = event.document
        if (
            state == State.APPLYING ||
            UndoManager.getInstance(editor.project!!).isUndoInProgress ||
            !PomModelImpl.isAllowPsiModification() ||
            (document as DocumentEx).isInBulkUpdate
        ) {
            return
        }

        val offset = event.offset
        val oldLength = event.oldLength
        val fragment = event.newFragment
        val newLength = event.newLength

        for (i in 0 until newLength) {
            if (!fragment[i].isValidTagNameChar()) {
                clearMarkers()
                return
            }
        }

        if (state == State.INITIAL) {
            val file = manager.getPsiFile(document)
            if (file == null || manager.synchronizer.isInSynchronization(document)) return

            val leaders = SmartList<RangeMarker>()
            for (caret in editor.caretModel.allCarets) {
                val leader = createTagNameMarker(caret)
                if (leader == null) {
                    for (marker in leaders) {
                        marker.dispose()
                    }
                    return
                }
                leader.isGreedyToLeft = true
                leader.isGreedyToRight = true
                leaders.add(leader)
            }
            if (leaders.isEmpty()) return

            if (manager.isUncommited(document)) {
                manager.commitDocument(document)
            }

            for (leader in leaders) {
                val support = findSupport(leader, file, document)
                if (support == null) {
                    clearMarkers()
                    return
                }
                support.isGreedyToLeft = true
                support.isGreedyToRight = true
                markers.add(Couple.of(leader, support))
            }

            if (!fitsInMarker(offset, oldLength)) {
                clearMarkers()
                return
            }

            state = State.TRACKING
        }

        if (markers.isEmpty()) return

        val fitsInMarker = fitsInMarker(offset, oldLength)
        if (!fitsInMarker || markers.size != editor.caretModel.caretCount) {
            clearMarkers()
            beforeDocumentChange(event)
        }
    }

    fun beforeCommandFinished() {
        if (markers.isEmpty()) return

        state = State.APPLYING

        val document = editor.document
        val apply = Runnable {
            for (couple in markers) {
                val leader = couple.first
                val support = couple.second
                if (document.textLength < leader.endOffset) {
                    return@Runnable
                }
                val name = document.getText(TextRange(leader.startOffset, leader.endOffset))
                if (document.textLength >= support.endOffset && name != document.getText(
                        TextRange(
                            support.startOffset,
                            support.endOffset
                        )
                    )
                ) {
                    document.replaceString(support.startOffset, support.endOffset, name)
                }
            }
        }
        ApplicationManager.getApplication().runWriteAction {
            val lookup = LookupManager.getActiveLookup(editor) as? LookupImpl
            if (lookup != null) {
                lookup.performGuardedChange(apply)
            } else {
                apply.run()
            }
        }

        state = State.TRACKING
    }

    fun fitsInMarker(offset: Int, oldLength: Int): Boolean {
        var fitsInMarker = false
        for (leaderAndSupport in markers) {
            val leader = leaderAndSupport.first
            if (!leader.isValid) {
                fitsInMarker = false
                break
            }
            fitsInMarker = fitsInMarker or
                    (offset >= leader.startOffset && offset + oldLength <= leader.endOffset)
        }
        return fitsInMarker
    }

    fun clearMarkers() {
        for (leaderAndSupport in markers) {
            leaderAndSupport.first.dispose()
            leaderAndSupport.second.dispose()
        }
        markers.clear()
        state = State.INITIAL
    }

    private fun createTagNameMarker(caret: Caret): RangeMarker? {
        val offset = caret.offset
        val document = editor.document
        val sequence = document.charsSequence
        var start = -1
        var end = -1
        for (i in offset - 1 downTo Math.max(0, offset - 50)) {
            try {
                val c = sequence[i]
                if (c == '<' || c == '/' && i > 0 && sequence[i - 1] == '<') {
                    start = i + 1
                    break
                }
                if (!c.isValidTagNameChar()) break
            } catch (e: IndexOutOfBoundsException) {
                LOG.error(
                    "incorrect offset:$i, initial: $offset",
                    Attachment("document.txt", sequence.toString())
                )
                return null
            }
        }
        if (start < 0) return null
        for (i in offset until Math.min(document.textLength, offset + 50)) {
            val c = sequence[i]
            if (!c.isValidTagNameChar()) {
                end = i
                break
            }
        }
        return if (end < 0 || start > end) null else document.createRangeMarker(start, end, true)
    }

    private fun findSupport(leader: RangeMarker, file: PsiFile, document: Document): RangeMarker? {
        val offset = leader.startOffset
        val element: PsiElement? = file.findElementAt(offset)
        val support: PsiElement? = findSupportElement(element)

        @Suppress("FoldInitializerAndIfToElvis")
        if (support == null) return findSupportForTagList(leader, element, document)

        val range = support.textRange
        return document.createRangeMarker(range.startOffset, range.endOffset, true)
    }

    private fun findSupportForTagList(
        leader: RangeMarker,
        element: PsiElement?,
        document: Document
    ): RangeMarker? {
        if (leader.startOffset != leader.endOffset || element == null) return null

        var support: PsiElement? = null
        if ("<>" == element.text) {
            val last = element.parent.lastChild
            if ("</>" == last.text) {
                support = last
            }
        }
        if ("</>" == element.text) {
            val first = element.parent.firstChild
            if ("<>" == first.text) {
                support = first
            }
        }
        if (support != null) {
            val range = support.textRange
            return document.createRangeMarker(range.endOffset - 1, range.endOffset - 1, true)
        }
        return null
    }

    private fun findSupportElement(el: PsiElement?): PsiElement? {
        if (el == null) return null
        val ktxEl = el.parentOfType<KtxElement>() ?: return null
        val start = ktxEl.simpleTagName ?: ktxEl.qualifiedTagName ?: return null
        val end = ktxEl.simpleClosingTagName ?: ktxEl.qualifiedClosingTagName
        return if (start.startOffset <= el.startOffset && start.endOffset >= el.endOffset) {
            // el is start tag, support is end tag
            end
        } else if (end != null && end.startOffset <= el.startOffset &&
            end.endOffset >= el.endOffset) {
            // el is the end tag, support is the start tag
            start
        } else null
    }
}
