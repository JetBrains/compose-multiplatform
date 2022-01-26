package org.jetbrains.compose.intentions.remove_parent_composable

import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import javax.swing.Icon
import org.jetbrains.compose.desktop.ide.preview.PreviewIcons
import org.jetbrains.compose.intentions.utils.is_intention_available.IsIntentionAvailable
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.composable_finder.NestedComposableFinder
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList

class RemoveParentComposableIntention : PsiElementBaseIntentionAction(),
    Iconable,
    PriorityAction,
    IsIntentionAvailable {

    override fun getText(): String {
        return "Remove the parent Composable"
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    private val composableFunctionFinder: ComposableFunctionFinder = NestedComposableFinder()

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isAvailable(composableFunctionFinder)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val wrapper = if (element.parent is KtValueArgumentList) {
            element.parent.prevSibling as? KtNameReferenceExpression ?: return
        } else {
            element.parentOfType() ?: return
        }
        val callExpression = (wrapper.parent as? KtCallExpression) ?: return
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
