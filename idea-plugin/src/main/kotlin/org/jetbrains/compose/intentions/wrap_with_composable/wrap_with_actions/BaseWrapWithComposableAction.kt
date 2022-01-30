package org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.template.impl.InvokeTemplateAction
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinderImpl
import org.jetbrains.compose.intentions.utils.get_root_psi_element.GetRootPsiElement
import org.jetbrains.compose.intentions.utils.is_intention_available.IsIntentionAvailable

abstract class BaseWrapWithComposableAction :
    PsiElementBaseIntentionAction(),
    HighPriorityAction,
    IsIntentionAvailable {

    private val composableFunctionFinder: ComposableFunctionFinder by lazy {
        ComposableFunctionFinderImpl()
    }

    private val getRootElement by lazy {
        GetRootPsiElement()
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isAvailable(composableFunctionFinder)
    }

    override fun startInWriteAction(): Boolean = true

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        getRootElement(element.parent)?.let { rootElement ->
            val selectionModel = editor!!.selectionModel
            val textRange = rootElement.textRange
            selectionModel.setSelection(textRange.startOffset, textRange.endOffset)

            InvokeTemplateAction(
                getTemplate(),
                editor,
                project,
                HashSet()
            ).perform()
        }
    }

    protected abstract fun getTemplate(): TemplateImpl?
}
