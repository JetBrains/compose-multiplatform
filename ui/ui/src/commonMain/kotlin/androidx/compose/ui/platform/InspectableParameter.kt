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

package androidx.compose.ui.platform

/**
 * A compose parameter that is inspectable by tools. It gives access to private parts of a
 * parameter value.
 */
interface InspectableParameter {

    /**
     * The elements of a compose parameter instance.
     */
    val inspectableElements: Sequence<ParameterElement>

    /**
     * Use this name as the parameter name shown in tools if this is a sub element of another
     * [InspectableParameter] specified with an empty name. Example: a modifier with multiple
     * elements.
     */
    val nameFallback: String?
        get() = null

    /**
     * Use this value as a representation of the overall value of this parameter.
     */
    val valueOverride: Any?
        get() = null
}

/**
 * A parameter element describes the elements of a compose parameter instance.
 * The [name] typically refers to a (possibly private) property name with its corresponding [value].
 */
data class ParameterElement(val name: String, val value: Any?)
