package org.jetbrains.kotlin.r4a.idea.editor


import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtxEnterHandler: EnterHandlerDelegateAdapter() {
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

        if (file !is KtFile) return EnterHandlerDelegate.Result.Continue

        // honor normal kotlin settings for smart enter
        if (!CodeInsightSettings.getInstance()!!.SMART_INDENT_ON_ENTER) return EnterHandlerDelegate.Result.Continue

        val document = editor.document
        val text = document.charsSequence
        val caretOffset = caretOffsetRef.get()!!.toInt()

        if (caretOffset !in 0..text.length) return EnterHandlerDelegate.Result.Continue

        val elementAt = file.findElementAt(caretOffset)
        if (elementAt is PsiWhiteSpace && ("\n" in elementAt.getText()!!)) return EnterHandlerDelegate.Result.Continue

        val elementBefore = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceAfter(file, caretOffset)
        val elementAfter = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceBefore(file, caretOffset)

        val isAfterGt = elementBefore?.node?.elementType == KtTokens.GT
        val isAfterArrow = elementBefore?.node?.elementType == KtTokens.ARROW
        val isBeforeGt = elementAfter?.node?.elementType == KtTokens.GT
        val isBeforeDivAndGt = elementAfter?.node?.elementType == KtTokens.DIV && elementAfter?.nextSibling?.node?.elementType == KtTokens.GT
        val isBeforeLtAndDiv = elementAfter?.node?.elementType == KtTokens.LT && elementAfter?.nextSibling?.node?.elementType == KtTokens.DIV
        val afterIsKtxElement = elementAfter?.parent is KtxElement

        val smartIndent = if (isBeforeGt && afterIsKtxElement) true // // <Foo{{CURSOR}}></Foo>
        else if (isBeforeDivAndGt && afterIsKtxElement) true // <Foo{{CURSOR}}/>
        else if (isAfterGt && isBeforeLtAndDiv && afterIsKtxElement) true // <Foo>{{CURSOR}}</Foo>
        else if (isAfterArrow && isBeforeLtAndDiv && afterIsKtxElement) true // <Foo> x ->{{CURSOR}}</Foo>
        else false

        if (smartIndent) {
            originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document)
            try {
                CodeStyleManager.getInstance(file.getProject())!!.adjustLineIndent(file, editor.caretModel.offset)
            }
            catch (e: IncorrectOperationException) {
                LOG.error(e)
            }

            return EnterHandlerDelegate.Result.DefaultForceIndent
        }

        return EnterHandlerDelegate.Result.Continue
    }
}
