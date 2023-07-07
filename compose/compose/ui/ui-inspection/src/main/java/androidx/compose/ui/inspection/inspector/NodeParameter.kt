/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.inspection.inspector

/**
 * Holds data representing a Composable parameter for the Layout Inspector.
 */
class NodeParameter internal constructor(
    /**
     * The name of the parameter.
     */
    val name: String,

    /**
     * The type of the parameter.
     */
    val type: ParameterType,

    /**
     * The value of the parameter.
     */
    val value: Any?
) {
    /**
     * Sub elements of the parameter.
     */
    val elements = mutableListOf<NodeParameter>()

    /**
     * Reference to value parameter.
     */
    var reference: NodeParameterReference? = null

    /**
     * The index into the composite parent parameter value.
     */
    var index = 0
}

/**
 * The type of a parameter.
 */
enum class ParameterType {
    String,
    Boolean,
    Double,
    Float,
    Int32,
    Int64,
    Color,
    Resource,
    DimensionDp,
    DimensionSp,
    DimensionEm,
    Lambda,
    FunctionReference,
    Iterable,
}
