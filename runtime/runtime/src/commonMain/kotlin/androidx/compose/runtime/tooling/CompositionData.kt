/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.tooling

import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A [CompositionData] is the data tracked by the composer during composition.
 *
 * This interface is not intended to be used directly and is provided to allow the tools API to
 * have access to data tracked during composition. The tools API should be used instead which
 * provides a more usable interpretation of the slot table.
 */
interface CompositionData {
    /**
     * Iterate the composition data in the group.  The composition data is structured as a tree of
     * values that corresponds to the call graph of the functions that produced the tree.
     * Interspersed are groups that represents the nodes themselves.
     */
    val compositionGroups: Iterable<CompositionGroup>

    /**
     * Returns true if no composition data has been collected. This occurs when the first
     * composition into this composition data has not completed yet or, if it is a group, it
     * doesn't contain any child groups.
     */
    val isEmpty: Boolean
}

/**
 * [CompositionGroup] is a group of data slots tracked independently by composition. These groups
 * correspond to flow control branches (such as if statements and function calls) as well as
 * emitting of a node to the tree.
 *
 * This interface is not intended to be used directly and is provided to allow the tools API to
 * have access to data tracked during composition. The tools API should be used instead which
 * provides a more usable interpretation of the slot table.
 */
@JvmDefaultWithCompatibility
interface CompositionGroup : CompositionData {
    /**
     * A value used to identify the group within its siblings and is typically a compiler
     * generated integer but can be an object if the [key] composable is used.
     */
    val key: Any

    /**
     * Information recorded by the compiler to help tooling identify the source that generated
     * the group. The format of this string is internal and is interpreted by the tools API which
     * translates this information into source file name and offsets.
     */
    val sourceInfo: String?

    /**
     * If the group represents a node this returns a non-null value which is the node that was
     * emitted for the group.
     */
    val node: Any?

    /**
     * The data stored in the slot table for this group. This information includes the values
     * stored for parameters that are checked for change, any value passed as a parameter for
     * [androidx.compose.runtime.remember] and the last value returned by
     * [androidx.compose.runtime.remember], etc.
     */
    val data: Iterable<Any?>

    /**
     * A value that identifies a Group independently of movement caused by recompositions.
     */
    val identity: Any?
      get() = null
}