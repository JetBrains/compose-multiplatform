package androidx.compose.plugins.idea.editor

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.completion.handlers.isCharAt
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import androidx.compose.plugins.idea.parentOfType

class KtxAttributeAutoParensHandler : TypedHandlerDelegate() {

    private fun EditorEx.prevCharIs(char: Char): Boolean {
        return document.charsSequence.isCharAt(caretModel.offset - 2, char)
    }

    private fun wrapWithParensIfAtEndOfAttribute(
        file: PsiFile,
        editor: EditorEx,
        project: Project
    ): Boolean {
        if (editor.caretModel.caretCount > 1) return false

        PsiDocumentManager.getInstance(project).commitDocument(editor.document)

        val el = file.findElementAt(editor.caretModel.offset - 1)
        val errorElement = el?.parent as? PsiErrorElement ?: return false
        if (errorElement.parent !is KtxElement) return false
        val attr =
            errorElement.getPrevSiblingIgnoringWhitespaceAndComments(withItself = false) as?
                    KtxAttribute ?: return false
        val valueExpr = attr.value ?: return false

        // This can happen if someone has started a parenthesized element with an open paren but hasn't typed the close paren yet. If
        // this is the case, insert only the close paren
        if (valueExpr.node.elementType ==
            KtNodeTypes.PARENTHESIZED && valueExpr.lastChild is PsiErrorElement) {
            editor.document.insertString(editor.caretModel.offset, ")")
        } else {
            editor.document.insertString(valueExpr.startOffset, "(")
            editor.document.insertString(editor.caretModel.offset, ")")
        }
        return true
    }

    private fun wrapValueExpressionIfNotTagCloseForChar(
        c: Char,
        file: PsiFile,
        editor: EditorEx
    ): Boolean {

        if (editor.caretModel.caretCount > 1) return false

        val el = file.findElementAt(editor.caretModel.offset - 1) ?: return false
        val ktxElement = el.parentOfType<KtxElement>() ?: return false

        val bracketElements = ktxElement.node.getChildren(BRACKET_SET)
        val gtCount = bracketElements.count { it.elementType == KtTokens.GT }
        val divCount = bracketElements.count { it.elementType == KtTokens.DIV }

        // there is no GT so the tag is unclosed, we definitely don't want to do anything here...
        if (gtCount == 0) return false

        val offset = editor.caretModel.offset

        // if we are typing after the first close bracket, we don't want to do anything
        if (bracketElements.first { it.elementType == KtTokens.GT }.startOffset < offset)
            return false

        when (c) {
            '>' -> {
                if (gtCount <= 1 && !(gtCount == 1 && divCount == 1)) return false
            }
            '/' -> {
                if (divCount == 0) return false
            }
            else -> return false
        }

        // if we made it here, we can attempt to wrap the value expression we are in...

        val attr = el.getPrevSiblingIgnoringWhitespaceAndComments(withItself = true) as?
                KtxAttribute ?: return false
        val valueExpr = attr.value ?: return false

        // This can happen if someone has started a parenthesized element with an open paren but hasn't typed the close paren yet. If
        // this is the case, insert only the close paren
        if (valueExpr.node.elementType == KtNodeTypes.PARENTHESIZED &&
            valueExpr.lastChild is PsiErrorElement) {
            editor.document.insertString(editor.caretModel.offset, "$c)")
            EditorModificationUtil.moveCaretRelatively(editor, 1)
        } else {
            editor.document.insertString(valueExpr.startOffset, "(")
            editor.document.insertString(editor.caretModel.offset, "$c)")
            EditorModificationUtil.moveCaretRelatively(editor, 1)
        }

        return true
    }

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType
    ): Result {
        val editorEx = editor as EditorEx
        when (c) {
            '>' -> {
                val didWrap = wrapValueExpressionIfNotTagCloseForChar(c, file, editorEx)
                if (didWrap) return Result.STOP
            }
            '/' -> {
                val didWrap = wrapValueExpressionIfNotTagCloseForChar(c, file, editorEx)
                if (didWrap) return Result.STOP
            }
        }
        return Result.CONTINUE
    }

    override fun charTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile
    ): Result {
        val editorEx = editor as EditorEx
        when (c) {
            // equality
            '=' -> {
                if (editorEx.prevCharIs('=') || editorEx.prevCharIs('!')) {
                    val didWrap = wrapWithParensIfAtEndOfAttribute(file, editorEx, project)
                    if (didWrap) return Result.STOP
                }
            }
            // single char binary operators, excluding '/' and '>'
            '+', '-', '%', '|', '&', '*', '<' -> {
                val didWrap = wrapWithParensIfAtEndOfAttribute(file, editorEx, project)
                if (didWrap) return Result.STOP
            }
            // nullary ?:
            ':' -> {
                if (editorEx.prevCharIs('?')) {
                    val didWrap = wrapWithParensIfAtEndOfAttribute(file, editorEx, project)
                    if (didWrap) return Result.STOP
                }
            }
        }
        return Result.CONTINUE
    }

    companion object {
        private val BRACKET_SET = TokenSet.create(KtTokens.DIV, KtTokens.GT)
    }
}