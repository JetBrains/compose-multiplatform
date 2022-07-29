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

import com.android.tools.compose.code.completion.constraintlayout.getJsonPropertyParent
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.json.psi.JsonElement
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

/**
 * Base model for [JsonElement], sets a pointer to avoid holding to the element itself.
 */
internal abstract class BaseJsonElementModel<E: JsonElement>(element: E) {
  protected val elementPointer = SmartPointerManager.createPointer(element)
}

/**
 * Base model for a [JsonProperty].
 *
 * Populates some common fields and provides useful function while avoiding holding to PsiElement instances.
 */
internal open class JsonPropertyModel(element: JsonProperty): BaseJsonElementModel<JsonProperty>(element) {
  /**
   * The [JsonObject] that describes this [JsonProperty].
   */
  private val innerJsonObject: JsonObject? = elementPointer.element?.getChildOfType<JsonObject>()

  /**
   * A mapping of the containing [JsonProperty]s by their declare name.
   */
  private val propertiesByName: Map<String, JsonProperty> =
    innerJsonObject?.propertyList?.associateBy { it.name } ?: emptyMap()

  /**
   * [List] of all the children of this element that are [JsonProperty].
   */
  protected val innerProperties: Collection<JsonProperty> = propertiesByName.values

  /**
   * Name of the [JsonProperty].
   */
  val name: String?
    get() = elementPointer.element?.name

  /**
   * A set of names for all declared properties in this [JsonProperty].
   */
  val declaredFieldNamesSet: Set<String> = propertiesByName.keys

  /**
   * For the children of the current element, returns the [JsonProperty] which name matches the given [name]. Null if none of them does.
   */
  protected fun findProperty(name: String): JsonProperty? = propertiesByName[name]

  /**
   * Returns true if this [JsonProperty] contains another [JsonProperty] declared by the given [name].
   */
  fun containsPropertyOfName(name: String): Boolean = propertiesByName.containsKey(name)

  /**
   * Returns the containing [JsonProperty].
   *
   * May return null if this model is for a top level [JsonProperty].
   */
  fun getParentProperty(): JsonProperty? = elementPointer.element?.parentOfType<JsonProperty>(withSelf = false)

  companion object {
    /**
     * Returns the [JsonPropertyModel] where the completion is performed on an inner [JsonProperty], including if the completion is on the
     * value side of the inner [JsonProperty].
     *
     * In other words, the model of the second [JsonProperty] parent if the element on [CompletionParameters.getPosition] is NOT a
     * [JsonProperty].
     *
     * Or the model of the first [JsonProperty] parent if the element on [CompletionParameters.getPosition] is a [JsonProperty].
     */
    fun getModelForCompletionOnInnerJsonProperty(parameters: CompletionParameters): JsonPropertyModel? {
      val parentJsonProperty = getJsonPropertyParent(parameters) ?: return null
      ProgressManager.checkCanceled()
      return JsonPropertyModel(parentJsonProperty)
    }
  }
}