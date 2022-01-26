package org.jetbrains.compose.intentions.remove_composable

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import javax.swing.Icon
import org.jetbrains.compose.desktop.ide.preview.PreviewIcons
import org.jetbrains.compose.intentions.utils.is_intention_available.IsIntentionAvailable
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.composable_finder.DeepComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.get_root_element.GetRootElement

class RemoveComposableIntention : PsiElementBaseIntentionAction(), Iconable, LowPriorityAction, IsIntentionAvailable {

    override fun getText(): String {
        return "Remove this Composable"
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    private val composableFunctionFinder: ComposableFunctionFinder = DeepComposableFunctionFinder()

    private val getRootElement = GetRootElement()

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isAvailable(composableFunctionFinder)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        getRootElement(element.parent)?.delete()
    }

    override fun getIcon(flags: Int): Icon = PreviewIcons.COMPOSE

}
