package androidx.compose.plugins.idea.quickfix

import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.psi.KtElement
import androidx.compose.plugins.idea.ComposeBundle

class ComposeAddImportAction(
    private val project: Project,
    private val editor: Editor,
    private val element: KtElement,
    private val variants: List<ImportVariant>
) : QuestionAction {
    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        if (!element.isValid) return false
        if (variants.isEmpty()) return false

        if (variants.size == 1 || ApplicationManager.getApplication().isUnitTestMode) {
            addImport(variants.first())
            return true
        }

        JBPopupFactory.getInstance().createListPopup(
            getVariantSelectionPopup()
        ).showInBestPositionFor(editor)
        return true
    }

    fun showHint(): Boolean {
        if (variants.isEmpty()) return false

        val hintText = ShowAutoImportPass.getMessage(variants.size > 1, variants.first().hint)
        HintManager.getInstance()
            .showQuestionHint(
                editor,
                hintText,
                element.textOffset,
                element.textRange!!.endOffset,
                this
            )

        return true
    }

    private fun getVariantSelectionPopup(): BaseListPopupStep<ImportVariant> {
        return object : BaseListPopupStep<ImportVariant>(
            ComposeBundle.message("imports.chooser.title"),
            variants
        ) {
            override fun isAutoSelectionEnabled() = false

            override fun isSpeedSearchEnabled() = true

            override fun onChosen(
                selectedValue: ImportVariant?,
                finalChoice: Boolean
            ): PopupStep<String>? {
                if (selectedValue == null || project.isDisposed) return null
                addImport(selectedValue)
                return null
            }

            override fun hasSubstep(selectedValue: ImportVariant?) = false
            override fun getTextFor(value: ImportVariant) = value.hint
        }
    }

    private fun addImport(variant: ImportVariant) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        CommandProcessor.getInstance().executeCommand(project, {
            ApplicationManager.getApplication().runWriteAction {
                ImportInsertHelper.getInstance(project)
                    .importDescriptor(element.containingKtFile, variant.descriptor)
            }
        }, ComposeBundle.message("add.import"), null)
    }
}