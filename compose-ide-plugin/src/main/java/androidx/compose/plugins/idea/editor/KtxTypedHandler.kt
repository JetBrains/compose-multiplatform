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
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.tree.IElementType
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import androidx.compose.plugins.idea.getNextLeafIgnoringWhitespace
import androidx.compose.plugins.idea.parentOfType

class KtxTypedHandler : TypedHandlerDelegate() {

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
            '{' -> {
                // if '{' is typed at the start of an attribute value expression and right before a '>' or a '/', the '}' won't get
                // inserted by kotlin, so we do it for you...
                if (betweenEqOfAttributeAndCloseTag(editor, file)) {
                    EditorModificationUtil.insertStringAtCaret(
                        editor,
                        "{}",
                        true,
                        false
                    )
                    EditorModificationUtil.moveCaretRelatively(editor, 1)
                    return Result.STOP
                }
            }
        }
        return Result.CONTINUE
    }

    private fun betweenEqOfAttributeAndCloseTag(editor: Editor, file: PsiFile): Boolean {
        val el = file.findElementAt(editor.caretModel.offset) ?: return false
        if (el.node.elementType == KtTokens.DOUBLE_ARROW) {
            // this is a special case where <Foo x=> will end in a double arrow, but is really just a user in the middle of typing
            // an attribute value. If they type `{` after the `=` we want to do the right thing here
            if (editor.caretModel.offset != el.startOffset + 1) return false
            if (el.parent?.parent !is KtxElement) return false
            if (el.parent?.getPrevSiblingIgnoringWhitespace(withItself = false) !is KtxAttribute)
                return false
            return true
        }
        val next = el.nextSibling
        if (next == null) {
            return false
        }
        if (next.node.elementType != KtTokens.DIV && next.node.elementType != KtTokens.GT)
            return false
        if (el.getPrevSiblingIgnoringWhitespace(withItself = false) !is KtxAttribute) return false

        return true
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
        @Suppress("NAME_SHADOWING") val editor = editor as EditorEx
        when (c) {
            '/' -> {
                // if `</` is typed, we can complete the closing tag...
                if (justTypedTheDivOfAClosingTag(editor)) {
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                    val div = file.findElementAt(editor.caretModel.offset - 1)
                        ?: return Result.CONTINUE
                    assert(div.node.elementType == KtTokens.DIV)
                    val errorKtxElement = div.parent as? PsiErrorElement ?: return Result.CONTINUE
                    // when the closing tag isn't fully formed, we actually parse it as an error tag in the body of the unclosed
                    // parent tag. Thus, to find the tag we want to close, we find the first parent ktx element
                    val el = errorKtxElement.parentOfType<KtxElement>()
                        ?: return Result.CONTINUE
                    val openTag = el.qualifiedTagName ?: el.simpleTagName
                        ?: return Result.CONTINUE
                    val tagText = editor.document.charsSequence.subSequence(
                        openTag.startOffset,
                        openTag.endOffset
                    )
                    EditorModificationUtil.insertStringAtCaret(
                        editor,
                        "$tagText>",
                        false,
                        true
                    )
                    PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)
                    try {
                        CodeStyleManager.getInstance(file.project)!!.adjustLineIndent(
                            file,
                            openTag.endOffset + 1
                        )
                    } catch (e: IncorrectOperationException) {
                    }
                    return Result.STOP
                }
                // if `/` is typed before a tag is closed, we can add `>` to complete the self-closing tag
                if (maybeJustTypedTheDivOfASelfClosingTag(editor)) {
                    // the above check can have false positives, so to be sure that we are in a KTX element, we commit the document,
                    // and then perform an additional check to ensure that the parent of the div is a ktx element
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                    if (justTypedTheDivOfASelfClosingTagAfterCommit(editor, file)) {
                        // now we can for sure check add the closing bracket
                        EditorModificationUtil.insertStringAtCaret(
                            editor,
                            ">",
                            true,
                            false)
                        return Result.STOP
                    }
                }
            }
            '>' -> {
                // if `>` is typed on an open tag with no closing tag, we can insert the full close tag here...
                if (justTypedTheClosingBracketOfOpenTag(editor)) {
                    PsiDocumentManager.getInstance(project).commitDocument(editor.document)

                    val gt = file.findElementAt(editor.caretModel.offset - 1)
                        ?: return Result.CONTINUE
                    assert(gt.node.elementType == KtTokens.GT)

                    val el = gt.parent as? KtxElement ?: return Result.CONTINUE
                    val openTag = el.qualifiedTagName ?: el.simpleTagName ?: return Result.CONTINUE
                    val closeTag = el.qualifiedClosingTagName ?: el.simpleClosingTagName
                    if (closeTag != null) {
                        // this element already has a closing tag, so we don't want to add another one... but we do want
                        // to go ahead and ensure that the indent of the body of this tag is correct
                        try {
                            CodeStyleManager.getInstance(file.project)!!
                                .adjustLineIndent(file, openTag.endOffset + 1)
                        } catch (e: IncorrectOperationException) {
                        }
                        return Result.CONTINUE
                    } else {
                        val next = gt.getNextLeafIgnoringWhitespace()
                        if (next == null || next.node.elementType == KtTokens.RBRACE) {
                            // the next token is a close brace or empty, so we assume that the user's intent is not to wrap
                            // content below with this tag. as a result, we automatically insert the closing tag
                            val tagText = editor.document.charsSequence.subSequence(
                                openTag.startOffset,
                                openTag.endOffset
                            )
                            EditorModificationUtil.insertStringAtCaret(
                                editor,
                                "</$tagText>",
                                true,
                                false)
                            return Result.STOP
                        } else {
                            // since there is content below the tag they are typing, we aren't going to auto-insert the close tag.
                            return Result.CONTINUE
                        }
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
            if (tokenType != KtTokens.GT) return false

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
            if (tokenType != KtTokens.DIV) return false
            retreat()

            if (tokenType != KtTokens.LT) return false // we aren't in a ktx closing tag
        }
        return true
    }

    private fun maybeJustTypedTheDivOfASelfClosingTag(editor: EditorEx): Boolean {
        val iterator = editor.highlighter.createIterator(editor.caretModel.offset)
        with(iterator) {
            retreat()
            if (tokenType != KtTokens.DIV) return false
            retreat()

            if (tokenType == KtTokens.LT) return false // we aren't in a ktx closing tag
        }
        return true
    }

    private fun justTypedTheDivOfASelfClosingTagAfterCommit(
        editor: EditorEx,
        file: PsiFile
    ): Boolean {
        val ktxEl = file.findElementAt(editor.caretModel.offset - 1)?.parent ?: return false
        if (ktxEl.node.elementType != KtNodeTypes.KTX_ELEMENT ||
            ktxEl.lastChild.node.elementType != KtTokens.DIV) return false
        return true
    }
}