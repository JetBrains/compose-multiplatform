package androidx.compose.plugins.idea.editor

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.KtxLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import androidx.compose.plugins.idea.getNextLeafIgnoringWhitespace
import androidx.compose.plugins.idea.getPrevLeafIgnoringWhitespace
import androidx.compose.plugins.idea.parentOfType

class KtxEnterHandler : EnterHandlerDelegateAdapter() {
    companion object {
        private val LOG = Logger.getInstance(KtxEnterHandler::class.java)
    }

    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffsetRef: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result? {

        val (
            smartIndent,
            insertClosingTag,
            indentClosingTag,
            element
        ) = getIndentMeta(file, editor, caretOffsetRef)
            ?: return EnterHandlerDelegate.Result.Continue

        if (insertClosingTag) {
            element?.let { ktxTag ->
                val openTag = ktxTag.simpleTagName ?: ktxTag.qualifiedTagName
                    ?: error("malformed tag")
                val tagText = editor.document.charsSequence.subSequence(
                    openTag.startOffset,
                    openTag.endOffset
                )
                EditorModificationUtil.insertStringAtCaret(
                    editor,
                    "</$tagText>",
                    true,
                    false
                )
            }
        }

        if (indentClosingTag) {
            element?.let { ktxTag ->
                val closingTag =
                    ktxTag.node.children().lastOrNull {
                            child -> child.elementType == KtTokens.LT
                    } ?: error("expected to find")
                editor.document.insertString(
                    closingTag.startOffset,
                    "\n"
                )
                PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)
                try {
                    CodeStyleManager.getInstance(file.project)!!.adjustLineIndent(
                        file,
                        closingTag.startOffset
                    )
                } catch (e: IncorrectOperationException) {
                }
            }
        }

        if (smartIndent) {
            originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
            PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)
            try {
                CodeStyleManager.getInstance(
                    file.project
                )!!.adjustLineIndent(file, editor.caretModel.offset)
            } catch (e: IncorrectOperationException) {
                LOG.error(e)
            }
        }

        val autocomplete = shouldPopupAutocomplete(file, caretOffsetRef)

        if (autocomplete) {
            AutoPopupController.getInstance(editor.project)?.scheduleAutoPopup(editor)
        }

        return if (smartIndent) EnterHandlerDelegate.Result.DefaultForceIndent
            else EnterHandlerDelegate.Result.Continue
    }

    private data class Meta(
        var smartIndent: Boolean = false,
        var insertClosingTag: Boolean = false,
        var indentClosingTag: Boolean = false,
        var element: KtxElement? = null
    )

    /**
     * We want to do a few "smart" enter handlers for KTX-specific edits.
     *
     * 1. Last attribute:
     *
     *          <Foo{{CURSOR}}></Foo>
     *
     *      should transform into:
     *
     *          <Foo
     *              {{CURSOR}}
     *          ></Foo>
     *
     *      Note that we can also do this transformation when it is a self-closing tag
     *
     * 2. Between Open/Closing:
     *
     *          <Foo>{{CURSOR}}</Foo>
     *
     *      should transform into:
     *
     *          <Foo>
     *              {{CURSOR}}
     *          </Foo>
     *
     *      Note that in this transformation, we can also have a parameter list to the left side of the cursor.
     */
    private fun getIndentMeta(
        file: PsiFile,
        editor: Editor,
        caretOffsetRef: Ref<Int>
    ): Meta? {
        if (file !is KtFile) return null

        // honor normal kotlin settings for smart enter
        if (!CodeInsightSettings.getInstance()!!.SMART_INDENT_ON_ENTER) return null

        val document = editor.document
        val text = document.charsSequence
        val caretOffset = caretOffsetRef.get()!!.toInt()

        if (caretOffset !in 0..text.length) return null

        val elementAt = file.findElementAt(caretOffset) ?: return null

        val elementBefore = elementAt.getPrevLeafIgnoringWhitespace(includeSelf = false)
            ?: return null
        val elementAfter = elementAt.getNextLeafIgnoringWhitespace(includeSelf = true)
            ?: return null
        if (elementBefore.endOffset > caretOffset) return null
        if (caretOffset > elementAfter.startOffset) return null

        val returnsBetweenLeftAndCaret =
            document.getText(TextRange(elementBefore.endOffset, caretOffset)).contains('\n')
        val returnsBetweenRightAndCaret =
            document.getText(TextRange(caretOffset, elementAfter.startOffset)).contains('\n')

        val beforeParent = elementBefore.parent
        val afterParent = elementAfter.parent

        val isAfterGt = elementBefore.node?.elementType == KtTokens.GT
        val isAfterArrow = elementBefore.node?.elementType == KtTokens.ARROW
        val isBeforeGt = elementAfter.node?.elementType == KtTokens.GT
        val isBeforeDivAndGt =
            elementAfter.node?.elementType == KtTokens.DIV &&
                    elementAfter.nextSibling?.node?.elementType == KtTokens.GT
        val isBeforeLtAndDiv =
            elementAfter.node?.elementType == KtTokens.LT &&
                    elementAfter.nextSibling?.node?.elementType == KtTokens.DIV
        val afterIsSameKtxTag = afterParent is KtxElement &&
                beforeParent?.parentOfType<KtxElement>() == afterParent

        val ktxElement = if (beforeParent is KtxElement)
            beforeParent
        else if (isAfterArrow && beforeParent?.parent is KtxLambdaExpression)
            beforeParent.parent.parent as? KtxElement
        else null

        val isUnclosed = ktxElement?.let {
            it.node.children().none { child -> child.elementType == KtTokens.DIV }
        } ?: false
        val isSelfClosing = !isUnclosed && ktxElement?.let {
            it.simpleClosingTagName ?: it.qualifiedClosingTagName
        } == null
        val isBetweenOpenAndCloseOfKtxElement = ktxElement?.let {
            it.simpleClosingTagName ?: it.qualifiedClosingTagName
        }?.let {
            val closeTagStart = it.startOffset - 2
            caretOffset <= closeTagStart
        } ?: false

        val closingTagLt = ktxElement?.let {
            it.node.children().lastOrNull { child -> child.elementType == KtTokens.LT }
        }
        val newLineExistsBeforeClosingTag = closingTagLt?.let { lt ->
            lt
                .leaves(forward = false)
                .takeWhile { leaf ->
                    when (leaf) {
                        is PsiWhiteSpace -> true
                        is PsiComment -> true
                        is PsiBlockStatement -> true
                        else -> false
                    }
                }
                .any { it.textContains('\n') }
        } ?: false

        return Meta(
            smartIndent = when {
                returnsBetweenLeftAndCaret || (returnsBetweenRightAndCaret && afterIsSameKtxTag) ->
                    false
                isBeforeGt && afterIsSameKtxTag -> true // // <Foo{{CURSOR}}></Foo>
                isBeforeDivAndGt && afterIsSameKtxTag -> true // <Foo{{CURSOR}}/>
                isAfterGt && isBeforeLtAndDiv && afterIsSameKtxTag ->
                    true // <Foo>{{CURSOR}}</Foo>
                isAfterArrow && isBeforeLtAndDiv && afterIsSameKtxTag ->
                    true // <Foo> x ->{{CURSOR}}</Foo>

                // <Foo>{{CURSOR}}<Bar /></Foo>
                (isAfterGt || isAfterArrow) && !isUnclosed && !afterIsSameKtxTag &&
                        !isSelfClosing && isBetweenOpenAndCloseOfKtxElement &&
                        !returnsBetweenRightAndCaret -> true
                // <Foo>{{CURSOR}}<Bar />
                (isAfterGt || isAfterArrow) && isUnclosed -> true
                else -> false
            },
            insertClosingTag = when {
                isSelfClosing -> false
                (isAfterGt || isAfterArrow) && isUnclosed -> true
                else -> false
            },
            indentClosingTag = when {
                isSelfClosing -> false
                !isUnclosed && !newLineExistsBeforeClosingTag &&
                        !afterIsSameKtxTag && isBetweenOpenAndCloseOfKtxElement -> true
                else -> false
            },
            element = ktxElement
        )
    }

    private fun shouldPopupAutocomplete(
        file: PsiFile,
        caretOffsetRef: Ref<Int>
    ): Boolean {
        if (file !is KtFile) return false
        val caretOffset = caretOffsetRef.get()!!.toInt()

        val cursorEl = file.findElementAt(caretOffset) as? PsiWhiteSpace ?: return false

        val ktxElement = cursorEl.parent as? KtxElement ?: return false

        val firstDiv = ktxElement.node.findChildByType(KtTokens.DIV)
        val firstGt = ktxElement.node.findChildByType(KtTokens.GT)

        if (firstDiv == null && firstGt == null) return false

        val endOfOpenTag = Math.min(firstDiv?.startOffset ?: Int.MAX_VALUE, firstGt?.startOffset
            ?: Int.MAX_VALUE)

        if (caretOffset > endOfOpenTag) return false

        // TODO(lmr): if element to the right is not white space, false???

        return true
    }
}
