package org.jetbrains.compose.intentions

import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import javax.swing.Icon
import org.jetbrains.compose.desktop.ide.preview.PreviewIcons
import org.jetbrains.compose.intentions.utils.composableFinder.ChildComposableFinder
import org.jetbrains.compose.intentions.utils.composableFinder.ComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.getRootPsiElement.GetRootPsiElement
import org.jetbrains.compose.intentions.utils.isIntentionAvailable
import org.jetbrains.kotlin.psi.KtCallExpression

class RemoveParentComposableIntention :
    PsiElementBaseIntentionAction(),
    PriorityAction {

    override fun getText(): String {
        return "Remove the parent Composable"
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    private val getRootElement = GetRootPsiElement()

    private val composableFunctionFinder: ComposableFunctionFinder = ChildComposableFinder()

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isIntentionAvailable(composableFunctionFinder)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val callExpression = getRootElement(element.parent) as? KtCallExpression ?: return
        val lambdaBlock =
            callExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.functionLiteral?.bodyExpression
                ?: return
        callExpression.replace(lambdaBlock)
    }

    override fun getPriority(): PriorityAction.Priority {
        return PriorityAction.Priority.NORMAL
    }
}
