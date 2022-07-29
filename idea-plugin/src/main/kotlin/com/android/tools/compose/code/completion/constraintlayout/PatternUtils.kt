/*
 * Copyright (C) 2022 The Android Open Source Project
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

import com.intellij.json.JsonElementTypes
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonReferenceExpression
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.StringPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

// region ConstraintLayout Pattern Helpers
internal fun jsonPropertyName() = PlatformPatterns.psiElement(JsonElementTypes.IDENTIFIER)

internal fun jsonStringValue() =
  PlatformPatterns.psiElement(JsonElementTypes.SINGLE_QUOTED_STRING).withParent<JsonStringLiteral>()

internal fun PsiElementPattern<*, *>.withConstraintSetsParentAtLevel(level: Int) = withPropertyParentAtLevel(level, KeyWords.ConstraintSets)
internal fun PsiElementPattern<*, *>.withTransitionsParentAtLevel(level: Int) = withPropertyParentAtLevel(level, KeyWords.Transitions)

internal fun PsiElementPattern<*, *>.insideClearArray() = inArrayWithinConstraintBlockProperty {
  // For the 'clear' constraint block property
  matches(KeyWords.Clear)
}

internal fun PsiElementPattern<*, *>.insideConstraintArray() = inArrayWithinConstraintBlockProperty {
  // The parent property name may only be a StandardAnchor
  oneOf(StandardAnchor.values().map { it.keyWord })
}

/**
 * [PsiElementPattern] that matches an element in a [JsonArray] within a Constraint block. Where the property the array is assigned to, has
 * a name that is matched by [matchPropertyName].
 */
internal fun PsiElementPattern<*, *>.inArrayWithinConstraintBlockProperty(matchPropertyName: StringPattern.() -> StringPattern) =
  withSuperParent(2, psiElement<JsonArray>())
    .withSuperParent(
      BASE_DEPTH_FOR_LITERAL_IN_PROPERTY + 1, // JsonArray adds one level
      psiElement<JsonProperty>().withChild(
        // The first expression in a JsonProperty corresponds to the name of the property
        psiElement<JsonReferenceExpression>().withText(StandardPatterns.string().matchPropertyName())
      )
    )
    .withConstraintSetsParentAtLevel(CONSTRAINT_BLOCK_PROPERTY_DEPTH + 1) // JsonArray adds one level
// endregion

// region Kotlin Syntax Helpers
internal inline fun <reified T : PsiElement> psiElement(): PsiElementPattern<T, PsiElementPattern.Capture<T>> =
  PlatformPatterns.psiElement(T::class.java)

internal inline fun <reified T : PsiElement> PsiElementPattern<*, *>.withParent() = this.withParent(T::class.java)

/**
 * Pattern such that when traversing up the tree from the current element, the element at [level] is a [JsonProperty]. And its name matches
 * the given [name].
 */
internal fun PsiElementPattern<*, *>.withPropertyParentAtLevel(level: Int, name: String) =
  withPropertyParentAtLevel(level, listOf(name))

/**
 * Pattern such that when traversing up the tree from the current element, the element at [level] is a [JsonProperty]. Which name matches
 * one of the given [names].
 */
internal fun PsiElementPattern<*, *>.withPropertyParentAtLevel(level: Int, names: Collection<String>) =
  this.withSuperParent(level, psiElement<JsonProperty>().withChild(
    psiElement<JsonReferenceExpression>().withText(StandardPatterns.string().oneOf(names)))
  )

/**
 * Verifies that the current element is at the given [index] of the elements contained by its [JsonArray] parent.
 */
internal fun <T : JsonValue> PsiElementPattern<T, PsiElementPattern.Capture<T>>.atIndexOfJsonArray(index: Int) =
  with(object : PatternCondition<T>("atIndexOfJsonArray") {
    override fun accepts(element: T, context: ProcessingContext?): Boolean {
      val parent = element.context as? JsonArray ?: return false
      val children = parent.valueList
      val indexOfSelf = children.indexOf(element)
      return index == indexOfSelf
    }
  })
// endregion