package org.jetbrains.compose.intentions.remove_composable

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import org.jetbrains.compose.desktop.ide.preview.PreviewIcons
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinder
import org.jetbrains.compose.intentions.utils.composable_finder.ComposableFunctionFinderImpl
import org.jetbrains.compose.intentions.utils.get_root_psi_element.GetRootPsiElement
import org.jetbrains.compose.intentions.utils.is_intention_available.IsIntentionAvailable
import javax.swing.Icon

class RemoveComposableIntention :
    PsiElementBaseIntentionAction(),
    Iconable,
    LowPriorityAction,
    IsIntentionAvailable {

    override fun getText(): String {
        return "Remove this Composable"
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    private val composableFunctionFinder: ComposableFunctionFinder = ComposableFunctionFinderImpl()

    private val getRootElement = GetRootPsiElement()

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return element.isAvailable(composableFunctionFinder)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        getRootElement(element.parent)?.delete()
    }

    override fun getIcon(flags: Int): Icon = PreviewIcons.COMPOSE
}
