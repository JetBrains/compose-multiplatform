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
 * A compose value that is inspectable by tools. It gives access to private parts of a value.
 */
interface InspectableValue {

    /**
     * The elements of a compose value.
     */
    val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf()

    /**
     * Use this name as the reference name shown in tools of this value if there is no explicit
     * reference name given to the value.
     * Example: a modifier in a modifier list.
     */
    val nameFallback: String?
        get() = null

    /**
     * Use this value as a readable representation of the value.
     */
    val valueOverride: Any?
        get() = null
}

/**
 * A [ValueElement] describes an element of a compose value instance.
 * The [name] typically refers to a (possibly private) property name with its corresponding [value].
 */
data class ValueElement(val name: String, val value: Any?)
