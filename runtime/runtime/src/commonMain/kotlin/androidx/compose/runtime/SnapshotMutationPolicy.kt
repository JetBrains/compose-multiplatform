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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass
package androidx.compose.runtime

import androidx.compose.runtime.snapshots.MutableSnapshot
// Explicit imports for these needed in common source sets.
import kotlin.jvm.JvmName
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A policy to control how the result of [mutableStateOf] report and merge changes to
 * the state object.
 *
 * A mutation policy can be passed as an parameter to [mutableStateOf], and [compositionLocalOf].
 *
 * Typically, one of the stock policies should be used such as [referentialEqualityPolicy],
 * [structuralEqualityPolicy], or [neverEqualPolicy]. However, a custom mutation policy can be
 * created by implementing this interface, such as a counter policy,
 *
 * @sample androidx.compose.runtime.samples.counterSample
 */
@JvmDefaultWithCompatibility
interface SnapshotMutationPolicy<T> {
    /**
     * Determine if setting a state value's are equivalent and should be treated as equal. If
     * [equivalent] returns `true` the new value is not considered a change.
     */
    fun equivalent(a: T, b: T): Boolean

    /**
     * Merge conflicting changes in snapshots. This is only called if [current] and [applied] are
     * not [equivalent]. If a valid merged value can be calculated then it should be returned.
     *
     * For example, if the state object holds an immutable data class with multiple fields,
     * and [applied] has changed fields that are unmodified by [current] it might be valid to return
     * a new copy of the data class that combines that changes from both [current] and [applied]
     * allowing a snapshot to apply that would have otherwise failed.
     *
     * @sample androidx.compose.runtime.samples.counterSample
     */
    fun merge(previous: T, current: T, applied: T): T? = null
}

/**
 * A policy to treat values of a [MutableState] as equivalent if they are referentially (===) equal.
 *
 * Setting [MutableState.value] to its current referentially (===) equal value is not considered
 * a change. When applying a [MutableSnapshot], if the snapshot changes the value to the
 * equivalent value the parent snapshot has is not considered a conflict.
 */
@Suppress("UNCHECKED_CAST")
fun <T> referentialEqualityPolicy(): SnapshotMutationPolicy<T> =
    ReferentialEqualityPolicy as SnapshotMutationPolicy<T>

private object ReferentialEqualityPolicy : SnapshotMutationPolicy<Any?> {
    override fun equivalent(a: Any?, b: Any?) = a === b

    override fun toString() = "ReferentialEqualityPolicy"
}

/**
 * A policy to treat values of a [MutableState] as equivalent if they are structurally (==) equal.
 *
 * Setting [MutableState.value] to its current structurally (==) equal value is not considered
 * a change. When applying a [MutableSnapshot], if the snapshot changes the value to the
 * equivalent value the parent snapshot has is not considered a conflict.
 */
@Suppress("UNCHECKED_CAST")
fun <T> structuralEqualityPolicy(): SnapshotMutationPolicy<T> =
    StructuralEqualityPolicy as SnapshotMutationPolicy<T>

private object StructuralEqualityPolicy : SnapshotMutationPolicy<Any?> {
    override fun equivalent(a: Any?, b: Any?) = a == b

    override fun toString() = "StructuralEqualityPolicy"
}

/**
 * A policy never treat values of a [MutableState] as equivalent.
 *
 * Setting [MutableState.value] will always be considered a change. When applying a
 * [MutableSnapshot] that changes the state will always conflict with other snapshots that change
 * the same state.
 */
@Suppress("UNCHECKED_CAST")
fun <T> neverEqualPolicy(): SnapshotMutationPolicy<T> =
    NeverEqualPolicy as SnapshotMutationPolicy<T>

private object NeverEqualPolicy : SnapshotMutationPolicy<Any?> {
    override fun equivalent(a: Any?, b: Any?) = false

    override fun toString() = "NeverEqualPolicy"
}
