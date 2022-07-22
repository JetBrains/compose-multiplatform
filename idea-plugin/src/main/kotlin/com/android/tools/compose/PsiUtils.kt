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
@file:JvmName("AndroidComposablePsiUtils")

package com.android.tools.compose

import com.intellij.openapi.roots.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import org.jetbrains.kotlin.idea.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.*

fun KtAnnotationEntry.getQualifiedName(): String? {
  return analyze(BodyResolveMode.PARTIAL).get(BindingContext.ANNOTATION, this)?.fqName?.asString()
}

fun KtAnnotationEntry.fqNameMatches(fqName: Set<String>): Boolean {
  val qualifiedName by lazy { getQualifiedName() }
  val shortName = shortName?.asString() ?: return false
  return fqName.filter { it.endsWith(shortName) }.any { it == qualifiedName }
}

fun PsiElement.isComposableFunction(): Boolean {
  if (this !is KtNamedFunction) return false

  return CachedValuesManager.getCachedValue(this) {
    val hasComposableAnnotation = annotationEntries.any {
      // fqNameMatches is expensive, so we first verify that the short name of the annotation matches.
      it.shortName?.identifier == COMPOSABLE_ANNOTATION_NAME &&
      it.fqNameMatches(COMPOSABLE_FQ_NAMES)
    }
    val containingKtFile = this.containingKtFile

    CachedValueProvider.Result.create(
      // TODO: see if we can handle alias imports without ruining performance.
      hasComposableAnnotation,
      containingKtFile,
      ProjectRootModificationTracker.getInstance(project)
    )
  }
}

fun PsiElement.isComposableAnnotation():Boolean =
  when (this) {
      is KtAnnotationEntry -> this.fqNameMatches(COMPOSABLE_FQ_NAMES)
    else -> false
  }

fun PsiElement.isInsideComposableCode(): Boolean {
  // TODO: also handle composable lambdas.
  return language == KotlinLanguage.INSTANCE && parentOfType<KtNamedFunction>()?.isComposableFunction() == true
}
