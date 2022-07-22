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
package com.android.tools.compose.code.completion.constraintlayout

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonElementTypes
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonReferenceExpression
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType

/**
 * [CompletionContributor] for the JSON5 format supported in ConstraintLayout-Compose (and MotionLayout).
 *
 * See the official wiki in [GitHub](https://github.com/androidx/constraintlayout/wiki/ConstraintSet-JSON5-syntax) to learn more about the
 * supported JSON5 syntax.
 */
class ConstraintLayoutJsonCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      jsonPropertyName().withConstraintSetsParentAtLevel(6),
      ConstraintIdsProvider
    )
  }
}

/**
 * [SmartPsiElementPointer] to the [JsonProperty] corresponding to the ConstraintSets property.
 */
private typealias ConstraintSetsPropertyPointer = SmartPsiElementPointer<JsonProperty>

private val constraintSetsPropertyKey =
  Key.create<ConstraintSetsPropertyPointer>("compose.json.autocomplete.constraint.sets.property")

/**
 * Completion provider that looks for the 'ConstraintSets' declaration and caches it, provides useful functions for inheritors that want to
 * provide completions based con the contents of the 'ConstraintSets' [JsonProperty].
 */
private abstract class ConstraintSetCompletionProvider : CompletionProvider<CompletionParameters>() {
  final override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val setsProperty = if (context[constraintSetsPropertyKey] != null) {
      context[constraintSetsPropertyKey]!!
    }
    else {
      parameters.position.getTopmostParentOfType<JsonObject>()?.getChildrenOfType<JsonProperty>()?.firstOrNull {
        it.name == KeyWords.ConstraintSets
      }?.let {
        val pointer = SmartPointerManager.createPointer(it)
        context.put(constraintSetsPropertyKey, pointer)
        return@let pointer
      }
    }
    addCompletions(setsProperty, parameters, result)
  }

  /**
   * Inheritors should implement this function that may pass a reference to the ConstraintSets property.
   */
  abstract fun addCompletions(
    constraintSetsProperty: ConstraintSetsPropertyPointer?,
    parameters: CompletionParameters,
    result: CompletionResultSet
  )

  /**
   * Returns the available constraint IDs for the given [constraintSetName], this is done by reading all IDs in all ConstraintSets and
   * subtracting the IDs already present in [constraintSetName].
   */
  protected fun ConstraintSetsPropertyPointer.findConstraintIdsForSet(constraintSetName: String): List<String> {
    val availableNames = mutableSetOf(KeyWords.Extends)
    val usedNames = mutableSetOf<String>()
    this.element?.getChildOfType<JsonObject>()?.getChildrenOfType<JsonProperty>()?.forEach { cSetProperty ->
      cSetProperty.getChildOfType<JsonObject>()?.getChildrenOfType<JsonProperty>()?.forEach { constraintNameProperty ->
        if (cSetProperty.name == constraintSetName) {
          usedNames.add(constraintNameProperty.name)
        }
        else {
          availableNames.add(constraintNameProperty.name)
        }
      }
    }
    availableNames.removeAll(usedNames)
    return availableNames.toList()
  }
}

/**
 * Provides options to autocomplete constraint IDs for constraint set declarations, based on the IDs already defined by the user in other
 * constraint sets.
 */
private object ConstraintIdsProvider : ConstraintSetCompletionProvider() {
  override fun addCompletions(constraintSetsProperty: SmartPsiElementPointer<JsonProperty>?,
                              parameters: CompletionParameters,
                              result: CompletionResultSet) {
    val parentName = parameters.position.getParentOfType<JsonProperty>(true)?.getParentOfType<JsonProperty>(true)?.name
    if (constraintSetsProperty != null && parentName != null) {
      constraintSetsProperty.findConstraintIdsForSet(parentName).forEach {
        val template = if (it == KeyWords.Extends) JsonStringValueTemplate else JsonNewObjectTemplate
        result.addLookupElement(name = it, tailText = null, template)
      }
    }
  }
}

private fun jsonPropertyName() = PlatformPatterns.psiElement(JsonElementTypes.IDENTIFIER)

private inline fun <reified T : PsiElement> psiElement() = PlatformPatterns.psiElement(T::class.java)

private fun PsiElementPattern<*, *>.withPropertyParentAtLevel(level: Int, name: String) =
  this.withSuperParent(level, psiElement<JsonProperty>().withChild(psiElement<JsonReferenceExpression>().withText(name)))

private fun PsiElementPattern<*, *>.withConstraintSetsParentAtLevel(level: Int) = withPropertyParentAtLevel(level, "ConstraintSets")

private fun CompletionResultSet.addLookupElement(name: String, tailText: String? = null, format: InsertionFormat? = null) {
  var lookupBuilder = if (format == null) {
    LookupElementBuilder.create(name)
  }
  else {
    LookupElementBuilder.create(format, name).withInsertHandler(InsertionFormatHandler)
  }
  lookupBuilder = lookupBuilder.withCaseSensitivity(false)
  if (tailText != null) {
    lookupBuilder = lookupBuilder.withTailText(tailText, true)
  }
  addElement(lookupBuilder)
}