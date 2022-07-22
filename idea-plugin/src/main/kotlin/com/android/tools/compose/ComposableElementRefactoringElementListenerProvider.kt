/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.android.tools.modules.*
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Renames KtFile if a @Composable function with the same name was renamed.
 */
class ComposableElementAutomaticRenamerFactory : AutomaticRenamerFactory {

  override fun isApplicable(element: PsiElement): Boolean {
    if (element.inComposeModule() != true ||
        element !is KtNamedFunction ||
        element.parent !is KtFile ||
        !element.isComposableFunction()
    ) return false

    val virtualFile = element.containingKtFile.virtualFile
    return virtualFile?.nameWithoutExtension == element.name
  }

  override fun getOptionName() = null

  override fun isEnabled() = true

  override fun setEnabled(enabled: Boolean) {}

  override fun createRenamer(element: PsiElement, newName: String?, usages: MutableCollection<UsageInfo>?): AutomaticRenamer {
    return object : AutomaticRenamer() {
      init {
        val file = element.containingFile
        myElements.add(file)
        suggestAllNames(file.name, newName + "." + KotlinFileType.EXTENSION)
      }

      override fun getDialogTitle() = ComposeBundle.message("rename.file")

      override fun getDialogDescription() = ComposeBundle.message("rename.files.with.following.names")

      override fun entityName() = ComposeBundle.message("file.name")
    }
  }
}