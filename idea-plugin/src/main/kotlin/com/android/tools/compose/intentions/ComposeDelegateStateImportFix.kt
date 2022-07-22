/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.compose.intentions

import com.android.tools.compose.ComposeBundle
import com.android.tools.compose.isClassOrExtendsClass
import com.android.tools.idea.AndroidTextUtils
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Registers ComposeDelegateStateImportFixFactory for DELEGATE_SPECIAL_FUNCTION_MISSING error.
 *
 * DELEGATE_SPECIAL_FUNCTION_MISSING is an error when there is no getValue or setValue function for an object after "by" keyword.
 *
 * TODO(b/157543181):Delete code after JB bug is fixed.
 */
class ComposeDelegateStateImportFixContributor : QuickFixContributor {
  override fun registerQuickFixes(quickFixes: QuickFixes) {
    quickFixes.register(Errors.DELEGATE_SPECIAL_FUNCTION_MISSING, ComposeDelegateStateImportFixFactory())
  }
}

/**
 * Creates an IntentionAction that allow to add [androidx.compose.runtime.getValue] import for [androidx.compose.runtime.MutableState] in delegate position.
 */
private class ComposeDelegateStateImportFixFactory : KotlinSingleIntentionActionFactory() {
  private val stateMethodNames = mapOf(
    "mutableStateOf" to "androidx.compose.runtime.mutableStateOf",
    "state" to "androidx.compose.runtime.state")

  /**
   * Returns true, if the given [callExpression] contains a call to `mutableStateOf` or `state`. This allows suggesting the automatic
   * import of `getValue` even when `mutableStateOf` or `state` have not been imported yet.
   */
  private fun resolveUndefinedStateCall(callExpression: KtCallExpression): String? =
    stateMethodNames[PsiTreeUtil.findChildOfType(callExpression, KtCallExpression::class.java, true)?.callName()]

  override fun createAction(diagnostic: Diagnostic): IntentionAction? {
    val callExpression = diagnostic.psiElement.safeAs<KtCallExpression>()
                         ?: diagnostic.psiElement.getChildOfType()
                         ?: return null
    val delegateType = callExpression.getResolvedCall(callExpression.analyze(BodyResolveMode.FULL))?.getReturnType() ?: return null
    return when {
      delegateType.isClassOrExtendsClass("androidx.compose.runtime.State") -> ComposeDelegateStateImportFixAction()
      // Handle the case where the state is embedded within a remember {} call but we can not infer the type.
      delegateType.isClassOrExtendsClass("androidx.compose.runtime.remember.T") && resolveUndefinedStateCall(callExpression) != null ->
        ComposeDelegateStateImportFixAction(listOfNotNull(resolveUndefinedStateCall(callExpression)))
      else -> null
    }
  }
}

private class ComposeDelegateStateImportFixAction(additionalImports: List<String> = emptyList()) : IntentionAction {
  /**
   * List of all the imports that this fix will add when invoked.
   */
  private val importList = additionalImports + "androidx.compose.runtime.getValue"

  /**
   * List of the short names for the [importList].
   */
  private val importShortNames = importList.map { it.substringAfterLast(".") }.sorted()

  override fun startInWriteAction() = false

  override fun getFamilyName() = text

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

  override fun getText() = ComposeBundle.message("import.compose.state",
                                                 AndroidTextUtils.generateCommaSeparatedList(importShortNames, "and"))

  // Inspired by KotlinAddImportAction#addImport
  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (editor == null || file !is KtFile) return
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    psiDocumentManager.commitAllDocuments()

    project.executeWriteCommand(QuickFixBundle.message("add.import")) {
      importList.forEach { importFqName ->
        val descriptor = file.resolveImportReference(FqName(importFqName)).firstOrNull() ?: return@executeWriteCommand
        ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
      }
    }
  }
}
