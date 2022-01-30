package org.jetbrains.compose.intentions.remove_parent_composable

import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import org.jetbrains.compose.desktop.ide.preview.PreviewIcons
import org.jetbrains.compose.intentions.utils.composable_finder.ChildComposableFinder
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.get_root_psi_element.GetRootPsiElement
import org.jetbrains.compose.intentions.utils.is_intention_available.IsIntentionAvailable
import org.jetbrains.kotlin.psi.KtCallExpression
import javax.swing.Icon

class RemoveParentComposableIntention :
    PsiElementBaseIntentionAction(),
    Iconable,
    PriorityAction,
    IsIntentionAvailable {

    override fun getText(): String {
        return "Remove the parent Composable"
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    private val getRootElement = GetRootPsiElement()

    private val composableFunctionFinder: ComposableFunctionFinder = ChildComposableFinder()

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isAvailable(composableFunctionFinder)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val callExpression = getRootElement(element.parent) as? KtCallExpression ?: return
        val lambdaBlock =
            callExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.functionLiteral?.bodyExpression
                ?: return
        callExpression.replace(lambdaBlock)
    }

    override fun getIcon(flags: Int): Icon = PreviewIcons.COMPOSE

    override fun getPriority(): PriorityAction.Priority {
        return PriorityAction.Priority.NORMAL
    }
}
