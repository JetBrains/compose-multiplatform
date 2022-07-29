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

import com.android.tools.compose.code.completion.constraintlayout.provider.AnchorablesProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.ClearOptionsProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.ConstraintIdsProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.ConstraintSetFieldsProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.ConstraintSetNamesProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.ConstraintsProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.EnumValuesCompletionProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.KeyFrameChildFieldsCompletionProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.KeyFramesFieldsProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.OnSwipeFieldsProvider
import com.android.tools.compose.code.completion.constraintlayout.provider.TransitionFieldsProvider
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.modules.inComposeModule
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonStringLiteral

internal const val BASE_DEPTH_FOR_LITERAL_IN_PROPERTY = 2

internal const val BASE_DEPTH_FOR_NAME_IN_PROPERTY_OBJECT = BASE_DEPTH_FOR_LITERAL_IN_PROPERTY + BASE_DEPTH_FOR_LITERAL_IN_PROPERTY

/** Depth for a literal of a property of the list of ConstraintSets. With respect to the ConstraintSets root element. */
private const val CONSTRAINT_SET_LIST_PROPERTY_DEPTH = BASE_DEPTH_FOR_LITERAL_IN_PROPERTY + BASE_DEPTH_FOR_LITERAL_IN_PROPERTY

/** Depth for a literal of a property of a ConstraintSet. With respect to the ConstraintSets root element. */
private const val CONSTRAINT_SET_PROPERTY_DEPTH = CONSTRAINT_SET_LIST_PROPERTY_DEPTH + BASE_DEPTH_FOR_LITERAL_IN_PROPERTY

/** Depth for a literal of a property of a Transition. With respect to the Transitions root element. */
private const val TRANSITION_PROPERTY_DEPTH = CONSTRAINT_SET_PROPERTY_DEPTH

/** Depth for a literal of a property of a Constraints block. With respect to the ConstraintSets root element. */
internal const val CONSTRAINT_BLOCK_PROPERTY_DEPTH = CONSTRAINT_SET_PROPERTY_DEPTH + BASE_DEPTH_FOR_LITERAL_IN_PROPERTY

/** Depth for a literal of a property of an OnSwipe block. With respect to the Transitions root element. */
internal const val ONSWIPE_PROPERTY_DEPTH = TRANSITION_PROPERTY_DEPTH + BASE_DEPTH_FOR_LITERAL_IN_PROPERTY

/** Depth for a literal of a property of a KeyFrames block. With respect to the Transitions root element. */
internal const val KEYFRAMES_PROPERTY_DEPTH = ONSWIPE_PROPERTY_DEPTH

/**
 * [CompletionContributor] for the JSON5 format supported in ConstraintLayout-Compose (and MotionLayout).
 *
 * See the official wiki in [GitHub](https://github.com/androidx/constraintlayout/wiki/ConstraintSet-JSON5-syntax) to learn more about the
 * supported JSON5 syntax.
 */
class ConstraintLayoutJsonCompletionContributor : CompletionContributor() {
  init {
    // region ConstraintSets
    extend(
      CompletionType.BASIC,
      // Complete field names in ConstraintSets
      jsonPropertyName().withConstraintSetsParentAtLevel(CONSTRAINT_SET_PROPERTY_DEPTH),
      ConstraintSetFieldsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete constraints field names (width, height, start, end, etc.)
      jsonPropertyName().withConstraintSetsParentAtLevel(CONSTRAINT_BLOCK_PROPERTY_DEPTH),
      ConstraintsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete ConstraintSet names in Extends keyword
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, KeyWords.Extends),
      ConstraintSetNamesProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete IDs on special anchors, they take a single string value
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, SpecialAnchor.values().map { it.keyWord }),
      ConstraintIdsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete IDs in the constraint array (first position)
      jsonStringValue()
        // First element in the array, ie: there is no PsiElement preceding the desired one at this level
        .withParent(psiElement<JsonStringLiteral>().atIndexOfJsonArray(0))
        .insideConstraintArray(),
      ConstraintIdsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete anchors in the constraint array (second position)
      jsonStringValue()
        // Second element in the array, ie: there is one PsiElement preceding the desired one at this level
        .withParent(psiElement<JsonStringLiteral>().atIndexOfJsonArray(1))
        .insideConstraintArray(),
      AnchorablesProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete a clear option within the 'clear' array
      jsonStringValue()
        .insideClearArray(),
      ClearOptionsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete non-numeric dimension values for width & height
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, Dimension.values().map { it.keyWord }),
      EnumValuesCompletionProvider(DimBehavior::class)
    )
    extend(
      CompletionType.BASIC,
      // Complete Visibility mode values
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, KeyWords.Visibility),
      EnumValuesCompletionProvider(VisibilityMode::class)
    )
    //endregion

    //region Transitions
    extend(
      CompletionType.BASIC,
      // Complete fields of a Transition block
      jsonPropertyName()
        .withTransitionsParentAtLevel(TRANSITION_PROPERTY_DEPTH),
      TransitionFieldsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete existing ConstraintSet names for `from` and `to` Transition properties
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, listOf(TransitionField.From.keyWord, TransitionField.To.keyWord))
        .withTransitionsParentAtLevel(TRANSITION_PROPERTY_DEPTH),
      // TODO(b/207030860): Guarantee that provided names for 'from' or 'to' are distinct from each other,
      //  ie: both shouldn't reference the same ConstraintSet
      ConstraintSetNamesProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete fields of a KeyFrames block
      jsonPropertyName()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_NAME_IN_PROPERTY_OBJECT, TransitionField.KeyFrames.keyWord)
        .withTransitionsParentAtLevel(KEYFRAMES_PROPERTY_DEPTH),
      KeyFramesFieldsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete fields of an OnSwipe block
      jsonPropertyName()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_NAME_IN_PROPERTY_OBJECT, TransitionField.OnSwipe.keyWord)
        .withTransitionsParentAtLevel(ONSWIPE_PROPERTY_DEPTH),
      OnSwipeFieldsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete the possible IDs for the OnSwipe `anchor` property
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, OnSwipeField.AnchorId.keyWord),
      ConstraintIdsProvider
    )
    extend(
      CompletionType.BASIC,
      // Complete the known values for the OnSwipe `side` property
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, OnSwipeField.Side.keyWord),
      EnumValuesCompletionProvider(OnSwipeSide::class)
    )
    extend(
      CompletionType.BASIC,
      // Complete the known values for the OnSwipe `direction` property
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, OnSwipeField.Direction.keyWord),
      EnumValuesCompletionProvider(OnSwipeDirection::class)
    )
    extend(
      CompletionType.BASIC,
      // Complete the known values for the OnSwipe `mode` property
      jsonStringValue()
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_LITERAL_IN_PROPERTY, OnSwipeField.Mode.keyWord),
      EnumValuesCompletionProvider(OnSwipeMode::class)
    )
    extend(
      CompletionType.BASIC,
      // Complete the fields for any of the possible KeyFrames children
      jsonPropertyName()
        // A level deeper considering the array surrounding the object
        .withPropertyParentAtLevel(BASE_DEPTH_FOR_NAME_IN_PROPERTY_OBJECT + 1, KeyFrameField.values().map { it.keyWord }),
      KeyFrameChildFieldsCompletionProvider
    )
    //endregion
  }

  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    if (!StudioFlags.COMPOSE_CONSTRAINTLAYOUT_COMPLETION.get() ||
        parameters.position.inComposeModule() != true ||
        parameters.position.language != JsonLanguage.INSTANCE) {
      // TODO(b/207030860): Allow in other contexts once the syntax is supported outside Compose
      return
    }
    super.fillCompletionVariants(parameters, result)
  }
}