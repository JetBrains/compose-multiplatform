/*
 * Copyright 2019 The Android Open Source Project
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
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

fun isModifierChainLongerThanTwo(element: KtElement): Boolean {
  if (element.getChildrenOfType<KtDotQualifiedExpression>().isNotEmpty()) {
    val fqName = element.resolveToCall(BodyResolveMode.PARTIAL)?.getReturnType()?.fqName?.asString()
    if (fqName == COMPOSE_MODIFIER_FQN) {
      return true
    }
  }
  return false
}

internal fun KotlinType.isClassOrExtendsClass(classFqName:String): Boolean {
  return fqName?.asString() == classFqName || supertypes().any { it.fqName?.asString() == classFqName }
}