package org.jetbrains.kotlin.r4a.idea.editor

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtxTypedHandler : TypedHandlerDelegate() {

    companion object {
        private inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? {
            var node: PsiElement? = this
            while (node != null) {
                if (node is T) return node
                node = node.parent
            }
            return null
        }
    }


    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType
    ): Result {
        when (c) {
            '>' -> {
                if (shouldOverwrite(KtTokens.GT, editor, file)) {
                    EditorModificationUtil.moveCaretRelatively(editor, 1)
                    return Result.STOP
                }
            }
            '/' -> {
                if (shouldOverwrite(KtTokens.DIV, editor, file)) {
                    EditorModificationUtil.moveCaretRelatively(editor, 1)
                    return Result.STOP
                }
            }
        }
        return Result.CONTINUE
    }

    private fun shouldOverwrite(token: IElementType, editor: Editor, file: PsiFile): Boolean {
        val el = file.findElementAt(editor.caretModel.offset) ?: return false
        if (el.node?.elementType != token) return false
        if (el.parent?.node?.elementType != KtNodeTypes.KTX_ELEMENT) return false
        return true
    }

    override fun charTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile
    ): Result {
        val editor = editor as EditorEx
        when (c) {
            '/' -> {
                // if `</` is typed, we can complete the closing tag...
                if (justTypedTheDivOfAClosingTag(editor)) {
                    val errorKtxElement = file.findElementAt(editor.caretModel.offset - 1)?.parent ?: error("expected to find error ktx element")
                    val el = errorKtxElement.parentOfType<KtxElement>() ?: return Result.CONTINUE
                    val openTag = el.qualifiedTagName ?: el.simpleTagName ?: error("no tag name found")
                    val tagText = editor.document.charsSequence.subSequence(openTag.startOffset, openTag.endOffset)
                    EditorModificationUtil.insertStringAtCaret(editor, "$tagText>", false, true)
                    return Result.STOP
                }
                // if `/` is typed before a tag is closed, we can add `>` to complete the self-closing tag
                if (maybeJustTypedTheDivOfASelfClosingTag(editor, file)) {
                    // the above check can have false positives, so to be sure that we are in a KTX element, we commit the document,
                    // and then perform an additional check to ensure that the parent of the div is a ktx element
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                    if (justTypedTheDivOfASelfClosingTagAfterCommit(editor, file)) {
                        // now we can for sure check add the closing bracket
                        EditorModificationUtil.insertStringAtCaret(editor, ">", true, false)
                        return Result.STOP
                    }
                }
            }
            '>' -> {
                // if `>` is typed on an open tag with no closing tag, we can insert the full close tag here...
                if (justTypedTheClosingBracketOfOpenTag(editor)) {
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)

                    val el = file.findElementAt(editor.caretModel.offset - 1)?.parent as? KtxElement ?: return Result.CONTINUE
                    val openTag = el.qualifiedTagName ?: el.simpleTagName ?: error("no tag name found")
                    val closeTag = el.qualifiedClosingTagName ?: el.simpleClosingTagName
                    if (closeTag != null) {
                        // this element already has a closing tag, so we don't want to add another one...
                        return Result.CONTINUE
                    } else {
                        val tagText = editor.document.charsSequence.subSequence(openTag.startOffset, openTag.endOffset)
                        EditorModificationUtil.insertStringAtCaret(editor, "</$tagText>", true, false)
                        return Result.STOP
                    }
                }
            }
        }
        return Result.CONTINUE
    }


    private fun justTypedTheClosingBracketOfOpenTag(editor: EditorEx): Boolean {
        val iterator = editor.highlighter.createIterator(editor.caretModel.offset)
        with(iterator) {
            retreat()
            // if it turns out there are valid reasons for this to not be a div, we can change from assert to early return
            assert(tokenType == KtTokens.GT) { "expected a GT token type but instead was $tokenType" }

            retreat()
            // if it's a DIV then the tag was self-closing and there's nothing we need to do
            if (tokenType == KtTokens.DIV) return false
        }
        return true
    }

    private fun justTypedTheDivOfAClosingTag(editor: EditorEx): Boolean {
        val iterator = editor.highlighter.createIterator(editor.caretModel.offset)
        with(iterator) {
            retreat()
            // if it turns out there are valid reasons for this to not be a div, we can change from assert to early return
            assert(tokenType == KtTokens.DIV) { "expected a div token type but instead was $tokenType"}
            retreat()

            if (tokenType != KtTokens.LT) return false // we aren't in a ktx closing tag
        }
        return true
    }

    private fun maybeJustTypedTheDivOfASelfClosingTag(editor: EditorEx, file: PsiFile): Boolean {
        val iterator = editor.highlighter.createIterator(editor.caretModel.offset)
        with(iterator) {
            retreat()
            // if it turns out there are valid reasons for this to not be a div, we can change from assert to early return
            assert(tokenType == KtTokens.DIV) { "expected a div token type but instead was $tokenType"}
            retreat()

            if (tokenType == KtTokens.LT) return false // we aren't in a ktx closing tag
        }
        return true
    }

    private fun justTypedTheDivOfASelfClosingTagAfterCommit(editor: EditorEx, file: PsiFile): Boolean {
        val ktxEl = file.findElementAt(editor.caretModel.offset - 1)?.parent ?: return false
        if (ktxEl.node.elementType != KtNodeTypes.KTX_ELEMENT || ktxEl.lastChild.node.elementType != KtTokens.DIV) return false
        return true
    }
}