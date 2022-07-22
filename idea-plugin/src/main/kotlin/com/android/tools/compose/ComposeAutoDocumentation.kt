/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.compose

import com.android.tools.idea.flags.StudioFlags.COMPOSE_AUTO_DOCUMENTATION
import com.android.tools.idea.flags.StudioFlags.COMPOSE_EDITOR_SUPPORT
import com.android.tools.modules.*
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.CompletionService
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.Alarm
import java.beans.PropertyChangeListener

/**
 * Automatically shows quick documentation for Compose functions during code completion
 */
class ComposeAutoDocumentation(private val project: Project) {
  private var documentationOpenedByCompose = false

  private val lookupListener = PropertyChangeListener { evt ->
    if (COMPOSE_EDITOR_SUPPORT.get() &&
        COMPOSE_AUTO_DOCUMENTATION.get () &&
        LookupManager.PROP_ACTIVE_LOOKUP == evt.propertyName &&
        evt.newValue is Lookup) {
      val lookup = evt.newValue as Lookup

      val moduleSystem = FileDocumentManager.getInstance().getFile(lookup.editor.document)
        ?.let { ModuleUtilCore.findModuleForFile(it, lookup.project) }

      if (moduleSystem?.isComposeModule() == true) {
        lookup.addLookupListener(object : LookupListener {
          override fun currentItemChanged(event: LookupEvent) {
            showJavaDoc(lookup)
          }
        })
      }
    }
  }

  fun onProjectOpened() {
    if (COMPOSE_EDITOR_SUPPORT.get() && COMPOSE_AUTO_DOCUMENTATION.get() && !ApplicationManager.getApplication().isUnitTestMode) {
      LookupManager.getInstance(project).addPropertyChangeListener(lookupListener)
    }
  }

  class MyStartupActivity : StartupActivity {
    override fun runActivity(project: Project) = getInstance(project).onProjectOpened()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ComposeAutoDocumentation = project.getService(ComposeAutoDocumentation::class.java)
  }

  private fun showJavaDoc(lookup: Lookup) {
    if (LookupManager.getInstance(project).activeLookup !== lookup) {
      return
    }

    // If we open doc when lookup is not visible, doc will have wrong parent window (editor window instead of lookup).
    if ((lookup as? LookupImpl)?.isVisible != true) {
      Alarm().addRequest({ showJavaDoc(lookup) }, CodeInsightSettings.getInstance().JAVADOC_INFO_DELAY)
      return
    }

    val psiElement = lookup.currentItem?.psiElement ?: return
    val docManager = DocumentationManager.getInstance(project)
    if (!psiElement.isComposableFunction()) {
      // Close documentation for not composable function if it was opened by [AndroidComposeAutoDocumentation].
      // Case docManager.docInfoHint?.isFocused == true: user clicked on doc window and after that clicked on lookup and selected another
      // element. Due to bug docManager.docInfoHint?.isFocused == true even after clicking on lookup element, in that case if we close
      // docManager.docInfoHint, lookup will be closed as well.
      if (documentationOpenedByCompose && docManager.docInfoHint?.isFocused == false) {
        docManager.docInfoHint?.cancel()
        documentationOpenedByCompose = false
      }
      return
    }

    // It's composable function and documentation already opened
    if (docManager.docInfoHint != null) return  // will auto-update

    val currentItem = lookup.currentItem
    if (currentItem != null && currentItem.isValid && CompletionService.getCompletionService().currentCompletion != null) {
      try {
        docManager.showJavaDocInfo(lookup.editor, lookup.psiFile, false) {
          documentationOpenedByCompose = false
        }
        documentationOpenedByCompose = true
      }
      catch (ignored: IndexNotReadyException) {
      }
    }
  }
}
