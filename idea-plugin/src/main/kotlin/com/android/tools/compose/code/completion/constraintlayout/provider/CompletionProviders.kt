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
package com.android.tools.compose.code.completion.constraintlayout.provider

import com.android.tools.compose.code.completion.constraintlayout.ClearAllTemplate
import com.android.tools.compose.code.completion.constraintlayout.ClearOption
import com.android.tools.compose.code.completion.constraintlayout.ConstrainAnchorTemplate
import com.android.tools.compose.code.completion.constraintlayout.ConstraintLayoutKeyWord
import com.android.tools.compose.code.completion.constraintlayout.Dimension
import com.android.tools.compose.code.completion.constraintlayout.JsonNewObjectTemplate
import com.android.tools.compose.code.completion.constraintlayout.JsonNumericValueTemplate
import com.android.tools.compose.code.completion.constraintlayout.JsonObjectArrayTemplate
import com.android.tools.compose.code.completion.constraintlayout.JsonStringArrayTemplate
import com.android.tools.compose.code.completion.constraintlayout.JsonStringValueTemplate
import com.android.tools.compose.code.completion.constraintlayout.KeyCycleField
import com.android.tools.compose.code.completion.constraintlayout.KeyFrameChildCommonField
import com.android.tools.compose.code.completion.constraintlayout.KeyFrameField
import com.android.tools.compose.code.completion.constraintlayout.KeyPositionField
import com.android.tools.compose.code.completion.constraintlayout.KeyWords
import com.android.tools.compose.code.completion.constraintlayout.OnSwipeField
import com.android.tools.compose.code.completion.constraintlayout.RenderTransform
import com.android.tools.compose.code.completion.constraintlayout.SpecialAnchor
import com.android.tools.compose.code.completion.constraintlayout.StandardAnchor
import com.android.tools.compose.code.completion.constraintlayout.TransitionField
import com.android.tools.compose.code.completion.constraintlayout.buildJsonNumberArrayTemplate
import com.android.tools.compose.code.completion.constraintlayout.getJsonPropertyParent
import com.android.tools.compose.code.completion.constraintlayout.provider.model.ConstraintSetModel
import com.android.tools.compose.code.completion.constraintlayout.provider.model.ConstraintSetsPropertyModel
import com.android.tools.compose.code.completion.constraintlayout.provider.model.JsonPropertyModel
import com.android.tools.compose.completion.addLookupElement
import com.android.tools.compose.completion.inserthandler.InsertionFormat
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import kotlin.reflect.KClass

/**
 * Completion provider that looks for the 'ConstraintSets' declaration and passes a model that provides useful functions for inheritors that
 * want to provide completions based on the contents of the 'ConstraintSets' [JsonProperty].
 */
internal abstract class BaseConstraintSetsCompletionProvider : CompletionProvider<CompletionParameters>() {
  final override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val constraintSetsModel = createConstraintSetsModel(initialElement = parameters.position)
    if (constraintSetsModel != null) {
      ProgressManager.checkCanceled()
      addCompletions(constraintSetsModel, parameters, result)
    }
  }

  /**
   * Inheritors should implement this function that may pass a reference to the ConstraintSets property.
   */
  abstract fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  )

  /**
   * Finds the [JsonProperty] for the 'ConstraintSets' declaration and returns its model.
   *
   * The `ConstraintSets` property is expected to be a property of the root [JsonObject].
   */
  private fun createConstraintSetsModel(initialElement: PsiElement): ConstraintSetsPropertyModel? {
    // Start with the closest JsonObject towards the root
    var currentJsonObject: JsonObject? = initialElement.parentOfType<JsonObject>(withSelf = true) ?: return null
    lateinit var topLevelJsonObject: JsonObject

    // Then find the top most JsonObject while checking for cancellation
    while (currentJsonObject != null) {
      topLevelJsonObject = currentJsonObject
      currentJsonObject = currentJsonObject.parentOfType<JsonObject>(withSelf = false)

      ProgressManager.checkCanceled()
    }

    // The last non-null JsonObject is the topmost, the ConstraintSets property is expected within this element
    val constraintSetsProperty = topLevelJsonObject.findProperty(KeyWords.ConstraintSets) ?: return null
    // TODO(b/207030860): Consider creating the model even if there's no property that is explicitly called 'ConstraintSets'
    //    ie: imply that the root JsonObject is the ConstraintSets object, with the downside that figuring out the correct context would
    //    be much more difficult
    return ConstraintSetsPropertyModel(constraintSetsProperty)
  }
}

/**
 * Provides options to autocomplete constraint IDs for constraint set declarations, based on the IDs already defined by the user in other
 * constraint sets.
 */
internal object ConstraintSetFieldsProvider : BaseConstraintSetsCompletionProvider() {
  override fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val currentConstraintSet = ConstraintSetModel.getModelForCompletionOnConstraintSetProperty(parameters) ?: return
    val currentSetName = currentConstraintSet.name ?: return
    constraintSetsPropertyModel.getRemainingFieldsForConstraintSet(currentSetName).forEach { fieldName ->
      val template = if (fieldName == KeyWords.Extends) JsonStringValueTemplate else JsonNewObjectTemplate
      result.addLookupElement(lookupString = fieldName, tailText = null, template)
    }
  }
}

/**
 * Autocomplete options with the names of all available ConstraintSets, except from the one the autocomplete was invoked from.
 */
internal object ConstraintSetNamesProvider : BaseConstraintSetsCompletionProvider() {
  override fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val currentConstraintSet = ConstraintSetModel.getModelForCompletionOnConstraintSetProperty(parameters)
    val currentSetName = currentConstraintSet?.name
    val names = constraintSetsPropertyModel.getConstraintSetNames().toMutableSet()
    if (currentSetName != null) {
      names.remove(currentSetName)
    }
    names.forEach(result::addLookupElement)
  }
}

/**
 * Autocomplete options used to define the constraints of a widget (defined by the ID) within a ConstraintSet
 */
internal object ConstraintsProvider : BaseConstraintSetsCompletionProvider() {
  override fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val parentPropertyModel = JsonPropertyModel.getModelForCompletionOnInnerJsonProperty(parameters) ?: return
    val existingFieldsSet = parentPropertyModel.declaredFieldNamesSet
    StandardAnchor.values().forEach {
      if (!existingFieldsSet.contains(it.keyWord)) {
        result.addLookupElement(lookupString = it.keyWord, tailText = " [...]", format = ConstrainAnchorTemplate)
      }
    }
    if (!existingFieldsSet.contains(KeyWords.Visibility)) {
      result.addLookupElement(lookupString = KeyWords.Visibility, format = JsonStringValueTemplate)
    }
    result.addEnumKeyWordsWithStringValueTemplate<SpecialAnchor>(existingFieldsSet)
    result.addEnumKeyWordsWithNumericValueTemplate<Dimension>(existingFieldsSet)
    result.addEnumKeyWordsWithNumericValueTemplate<RenderTransform>(existingFieldsSet)

    // Complete 'clear' if the containing ConstraintSet has `extendsFrom`
    val containingConstraintSetModel = parentPropertyModel.getParentProperty()?.let {
      ConstraintSetModel(it)
    }
    if (containingConstraintSetModel?.extendsFrom != null) {
      // Add an option with an empty string array and another one with all clear options
      result.addLookupElement(lookupString = KeyWords.Clear, format = JsonStringArrayTemplate)
      result.addLookupElement(lookupString = KeyWords.Clear, format = ClearAllTemplate, tailText = " [<all>]")
    }
  }
}

/**
 * Provides IDs when autocompleting a constraint array.
 *
 * The ID may be either 'parent' or any of the declared IDs in all ConstraintSets, except the ID of the constraints block from which this
 * provider was invoked.
 */
internal object ConstraintIdsProvider : BaseConstraintSetsCompletionProvider() {
  override fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val possibleIds = constraintSetsPropertyModel.constraintSets.flatMap { it.declaredIds }.toCollection(HashSet())
    // Parent ID should always be present
    possibleIds.add(KeyWords.ParentId)
    // Remove the current ID
    getJsonPropertyParent(parameters)?.name?.let(possibleIds::remove)

    possibleIds.forEach { id ->
      result.addLookupElement(lookupString = id)
    }
  }
}

/**
 * Provides the appropriate anchors when completing a constraint array.
 *
 * [StandardAnchor.verticalAnchors] can only be constrained to other vertical anchors. Same logic for [StandardAnchor.horizontalAnchors].
 */
internal object AnchorablesProvider : BaseConstraintSetsCompletionProvider() {
  override fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val currentAnchorKeyWord = parameters.position.parentOfType<JsonProperty>(withSelf = true)?.name ?: return

    val possibleAnchors = when {
      StandardAnchor.isVertical(currentAnchorKeyWord) -> StandardAnchor.verticalAnchors
      StandardAnchor.isHorizontal(currentAnchorKeyWord) -> StandardAnchor.horizontalAnchors
      else -> emptyList()
    }
    possibleAnchors.forEach { result.addLookupElement(lookupString = it.keyWord) }
  }
}

/**
 * Provides the appropriate options when completing string literals within a `clear` array.
 *
 * @see ClearOption
 */
internal object ClearOptionsProvider : BaseConstraintSetsCompletionProvider() {
  override fun addCompletions(
    constraintSetsPropertyModel: ConstraintSetsPropertyModel,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    val existing = parameters.position.parentOfType<JsonArray>(withSelf = false)?.valueList
                     ?.filterIsInstance<JsonStringLiteral>()
                     ?.map { it.value }
                     ?.toSet() ?: emptySet()
    addEnumKeywords<ClearOption>(result, existing)
  }
}

/**
 * Provides completion for the fields of a `Transition`.
 *
 * @see TransitionField
 */
internal object TransitionFieldsProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val parentPropertyModel = JsonPropertyModel.getModelForCompletionOnInnerJsonProperty(parameters) ?: return
    TransitionField.values().forEach {
      if (parentPropertyModel.containsPropertyOfName(it.keyWord)) {
        // skip
        return@forEach
      }
      when (it) {
        TransitionField.OnSwipe,
        TransitionField.KeyFrames -> {
          result.addLookupElement(lookupString = it.keyWord, format = JsonNewObjectTemplate)
        }
        else -> {
          result.addLookupElement(lookupString = it.keyWord, format = JsonStringValueTemplate)
        }
      }
    }
  }
}

/**
 * Provides completion for the fields of an `OnSwipe` block.
 *
 * @see OnSwipeField
 */
internal object OnSwipeFieldsProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val parentPropertyModel = JsonPropertyModel.getModelForCompletionOnInnerJsonProperty(parameters) ?: return
    result.addEnumKeyWordsWithStringValueTemplate<OnSwipeField>(parentPropertyModel.declaredFieldNamesSet)
  }
}

/**
 * Provides completion for the fields of a `KeyFrames` block.
 *
 * @see KeyFrameField
 */
internal object KeyFramesFieldsProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val parentPropertyModel = JsonPropertyModel.getModelForCompletionOnInnerJsonProperty(parameters) ?: return
    addEnumKeywords<KeyFrameField>(
      result = result,
      format = JsonObjectArrayTemplate,
      existing = parentPropertyModel.declaredFieldNamesSet
    )
  }
}

/**
 * Provides completion for the fields of KeyFrame children. A KeyFrame child can be any of [KeyFrameField].
 */
internal object KeyFrameChildFieldsCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    // TODO(b/207030860): For consistency, make it so that JsonPropertyModel may be used here. It currently won't work because the model
    //  doesn't consider a property defined by an array of objects.

    // Obtain existing list of existing properties
    val parentObject = parameters.position.parentOfType<JsonObject>(withSelf = false) ?: return
    val existingFieldsSet = parentObject.propertyList.map { it.name }.toSet()

    // We have to know the type of KeyFrame we are autocompleting for (KeyPositions, KeyAttributes, etc)
    val keyFrameTypeName = parentObject.parentOfType<JsonProperty>(withSelf = false)?.name ?: return

    // Look for the `frames` property, we want to know the size of its array (if present), since all other numeric properties should have an
    // array of the same size
    val framesProperty = parentObject.findProperty(KeyFrameChildCommonField.Frames.keyWord)
    val arrayCountInFramesProperty = (framesProperty?.value as? JsonArray)?.valueList?.size ?: 1

    // Create the template that will be used by any numeric property we autocomplete
    val jsonNumberArrayTemplate = buildJsonNumberArrayTemplate(count = arrayCountInFramesProperty)

    // We've done some read operations, check for cancellation
    ProgressManager.checkCanceled()

    // Common fields for any type of KeyFrame
    KeyFrameChildCommonField.values().forEach {
      if (existingFieldsSet.contains(it.keyWord)) {
        return@forEach
      }
      when (it) {
        KeyFrameChildCommonField.Frames -> result.addLookupElement(lookupString = it.keyWord, format = jsonNumberArrayTemplate)
        else -> result.addLookupElement(lookupString = it.keyWord, format = JsonStringValueTemplate)
      }
    }

    // Figure out which type of KeyFrame the completion is being called on, and offer completion for their respective fields
    when (keyFrameTypeName) {
      KeyFrameField.Positions.keyWord -> {
        addKeyPositionFields(result, existingFieldsSet) {
          // Some KeyPosition fields take either a Number Array value or a String value
          if (isNumberArrayType(it)) jsonNumberArrayTemplate else JsonStringValueTemplate
        }
      }
      KeyFrameField.Attributes.keyWord -> {
        // KeyAttributes properties are the same as the RenderTransform fields
        addEnumKeywords<RenderTransform>(result = result, format = jsonNumberArrayTemplate, existing = existingFieldsSet)
      }
      KeyFrameField.Cycles.keyWord -> {
        // KeyCycles properties are a mix of RenderTransform fields and KeyCycles specific fields
        addEnumKeywords<RenderTransform>(result = result, format = jsonNumberArrayTemplate, existing = existingFieldsSet)
        addEnumKeywords<KeyCycleField>(result = result, format = jsonNumberArrayTemplate, existing = existingFieldsSet)
      }
      else -> {
        thisLogger().warn("Completion on unknown KeyFrame type: $keyFrameTypeName")
      }
    }
  }

  /**
   * Add LookupElements to the [result] for each non-repeated [KeyPositionField] using the [InsertionFormat] returned by [templateProvider].
   */
  private fun addKeyPositionFields(
    result: CompletionResultSet,
    existing: Set<String>,
    templateProvider: (KeyPositionField) -> InsertionFormat
  ) {
    KeyPositionField.values().forEach { keyPositionField ->
      if (existing.contains(keyPositionField.keyWord)) {
        // Skip repeated fields
        return@forEach
      }
      result.addLookupElement(lookupString = keyPositionField.keyWord, format = templateProvider(keyPositionField))
    }
  }

  private fun isNumberArrayType(keyPositionField: KeyPositionField) =
    when (keyPositionField) {
      // Only some KeyPosition fields receive a Number value
      KeyPositionField.PercentX,
      KeyPositionField.PercentY,
      KeyPositionField.PercentWidth,
      KeyPositionField.PercentHeight -> true
      else -> false
    }
}

/**
 * Provides plaint-text completion for each of the elements in the Enum.
 *
 * The provided values come from [ConstraintLayoutKeyWord.keyWord].
 */
internal class EnumValuesCompletionProvider<E>(private val enumClass: KClass<E>)
  : CompletionProvider<CompletionParameters>() where E : Enum<E>, E : ConstraintLayoutKeyWord {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    enumClass.java.enumConstants.forEach {
      result.addLookupElement(lookupString = it.keyWord)
    }
  }
}

/**
 * Add the [ConstraintLayoutKeyWord.keyWord] of the enum constants as a completion result that takes a string for its value.
 */
private inline fun <reified E> CompletionResultSet.addEnumKeyWordsWithStringValueTemplate(
  existing: Set<String>
) where E : Enum<E>, E : ConstraintLayoutKeyWord {
  addEnumKeywords<E>(result = this, existing = existing, format = JsonStringValueTemplate)
}

/**
 * Add the [ConstraintLayoutKeyWord.keyWord] of the enum constants as a completion result that takes a number for its value.
 */
private inline fun <reified E> CompletionResultSet.addEnumKeyWordsWithNumericValueTemplate(
  existing: Set<String>
) where E : Enum<E>, E : ConstraintLayoutKeyWord {
  addEnumKeywords<E>(result = this, existing = existing, format = JsonNumericValueTemplate)
}

/**
 * Helper function to simplify adding enum constant members to the completion result.
 */
private inline fun <reified E> addEnumKeywords(
  result: CompletionResultSet,
  existing: Set<String> = emptySet(),
  format: InsertionFormat? = null
) where E : Enum<E>, E : ConstraintLayoutKeyWord {
  E::class.java.enumConstants.forEach { constant ->
    if (!existing.contains(constant.keyWord)) {
      result.addLookupElement(lookupString = constant.keyWord, format = format)
    }
  }
}