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

import com.intellij.json.psi.JsonProperty

/**
 * Model for the JSON block that corresponds to the constraints applied on a widget (defined by an ID).
 *
 * Constraints are a set of instructions that define the widget's dimensions, position with respect to other widgets and render-time
 * transforms.
 */
internal class ConstraintsModel(jsonProperty: JsonProperty): JsonPropertyModel(jsonProperty) {
  // TODO(b/207030860): Fill the contents of this model as is necessary, keeping in mind that it would be useful to have fields like
  //   'verticalConstraints', 'hasBaseline', 'dimensionBehavior', etc...
}