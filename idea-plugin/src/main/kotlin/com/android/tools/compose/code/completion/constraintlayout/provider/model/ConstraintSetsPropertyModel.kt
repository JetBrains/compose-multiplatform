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
package com.android.tools.compose.code.completion.constraintlayout.provider.model

import com.android.tools.compose.code.completion.constraintlayout.KeyWords
import com.intellij.json.psi.JsonProperty

/**
 * Model for the `ConstraintSets` Json block.
 *
 * The `ConstraintSets` Json block, is a collection of different ConstraintSets, each of which describes a state of the layout by defining
 * properties of each of its widgets such as width, height or their layout constraints.
 *
 * @param constraintSetsElement The PSI element of the `ConstraintSets` Json property
 */
internal class ConstraintSetsPropertyModel(
  constraintSetsElement: JsonProperty
) : JsonPropertyModel(constraintSetsElement) {
  // TODO(b/209839226): Explore how we could use these models to validate the syntax or structure of the JSON as well as to check logic
  //  correctness through Inspections/Lint
  /**
   * List of all ConstraintSet elements in the Json block.
   */
  val constraintSets: List<ConstraintSetModel> = innerProperties.map { ConstraintSetModel(it) }

  /**
   * The names of all ConstraintSets in this block.
   */
  fun getConstraintSetNames(): Collection<String> {
    return declaredFieldNamesSet
  }

  /**
   * Returns the remaining possible fields for the given [constraintSetName], this is done by reading all fields in all ConstraintSets and
   * subtracting the fields already present in [constraintSetName]. Most of these should be the IDs that represent constrained widgets.
   */
  fun getRemainingFieldsForConstraintSet(constraintSetName: String): List<String> {
    val availableNames = mutableSetOf(KeyWords.Extends)
    val usedNames = mutableSetOf<String>()
    constraintSets.forEach { constraintSet ->
      constraintSet.declaredFieldNamesSet.forEach { propertyName ->
        if (constraintSet.name == constraintSetName) {
          usedNames.add(propertyName)
        }
        else {
          availableNames.add(propertyName)
        }
      }
    }
    availableNames.removeAll(usedNames)
    return availableNames.toList()
  }
}
