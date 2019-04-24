package org.jetbrains.kotlin.r4a.idea.quickfix

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.r4a.idea.R4aBundle

abstract class R4aImportFix(
    protected var expression: KtExpression
) : KotlinQuickFixAction<KtExpression>(expression), HighPriorityAction, HintAction {
    private val project = expression.project

    private val modificationCountOnCreate =
        PsiModificationTracker.SERVICE.getInstance(project).modificationCount

    private lateinit var suggestions: Collection<ImportVariant>

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!).execute()
        }
    }

    override fun getFamilyName(): String = R4aBundle.message("import.fix")

    override fun getText(): String = R4aBundle.message("import.fix")

    override fun startInWriteAction() = true

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: KtFile
    ) = element != null && suggestions.isNotEmpty()

    fun isOutdated() =
        modificationCountOnCreate !=
                PsiModificationTracker.SERVICE.getInstance(project).modificationCount

    override fun showHint(editor: Editor): Boolean {
        if (!expression.isValid || isOutdated()) return false

        if (
            ApplicationManager.getApplication().isUnitTestMode &&
            HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)
        ) return false

        if (suggestions.isEmpty()) return false

        return createAction(project, editor).showHint()
    }

    fun collectSuggestions() {
        suggestions = computeSuggestions()
    }

    abstract fun computeSuggestions(): List<ImportVariant>

    private fun createAction(project: Project, editor: Editor): R4aAddImportAction {
        return R4aAddImportAction(project, editor, expression, suggestions.toList())
    }
}