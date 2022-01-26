package org.jetbrains.compose.intentions.wrap_with_composable

import com.intellij.codeInsight.intention.impl.IntentionActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import com.intellij.ui.popup.list.ListPopupImpl
import org.jetbrains.compose.desktop.ide.preview.PreviewIcons
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.BaseWrapWithComposableAction
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.WrapWithBoxIntention
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.WrapWithCardIntention
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.WrapWithColumnIntention
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.WrapWithLzyColumnIntention
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.WrapWithLzyRowIntention
import org.jetbrains.compose.intentions.wrap_with_composable.wrap_with_actions.WrapWithRowIntention
import javax.swing.Icon

class WrapWithComposableIntentionGroup :
    IntentionActionGroup<BaseWrapWithComposableAction>(
        listOf(
            WrapWithBoxIntention(),
            WrapWithCardIntention(),
            WrapWithColumnIntention(),
            WrapWithRowIntention(),
            WrapWithLzyColumnIntention(),
            WrapWithLzyRowIntention()
        )
    ),
    Iconable {

    private fun createPopup(
        project: Project,
        actions: List<BaseWrapWithComposableAction>,
        invokeAction: (BaseWrapWithComposableAction) -> Unit
    ): ListPopup {

        val step = object : BaseListPopupStep<BaseWrapWithComposableAction>(null, actions) {

            override fun getTextFor(action: BaseWrapWithComposableAction) = action.text

            override fun onChosen(selectedValue: BaseWrapWithComposableAction, finalChoice: Boolean): PopupStep<*>? {
                invokeAction(selectedValue)
                return FINAL_CHOICE
            }
        }

        return ListPopupImpl(project, step)
    }

    override fun getFamilyName(): String {
        return "Compose Multiplatform intentions"
    }

    override fun chooseAction(
        project: Project,
        editor: Editor,
        file: PsiFile,
        actions: List<BaseWrapWithComposableAction>,
        invokeAction: (BaseWrapWithComposableAction) -> Unit
    ) {
        createPopup(project, actions, invokeAction).showInBestPositionFor(editor)
    }

    override fun getGroupText(actions: List<BaseWrapWithComposableAction>): String {
        return "Wrap with Composable"
    }

    override fun getIcon(flags: Int): Icon = PreviewIcons.COMPOSE
}
