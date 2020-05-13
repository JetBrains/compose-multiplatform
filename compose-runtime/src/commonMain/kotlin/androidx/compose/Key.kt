/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

/**
 * [key] is a utility composable that is used to "group" or "key" a block of execution inside of a
 * composition. This is sometimes needed for correctness inside of control-flow that may cause a
 * given composable invocation to execute more than once during composition.
 *
 * The value for a key *does not need to be globally unique*, and needs only be unique amongst the
 * invocations of [key] *at that point* in composition.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.samples.locallyUniqueKeys
 *
 * Even though there are users with the same id composed in both the top and the bottom loop,
 * because they are different calls to [key], there is no need to create compound keys.
 *
 * The key must be unique for each element in the collection, however, or children and local state
 * might be reused in unintended ways.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.samples.notAlwaysUniqueKeys
 *
 * This example assumes that `parent.id` is a unique key for each item in the collection,
 * but this is only true if it is fair to assume that a parent will only ever have a single child,
 * which may not be the case.  Instead, it may be more correct to do the following:
 *
 * @sample androidx.compose.samples.moreCorrectUniqueKeys
 *
 * @param v1 The value used to identify this group. The value will be compared for equality
 *  using [equals] and hashed using [hashCode].
 * @param block The composable children for this group.
 *
 * @see [Pivotal]
 */
@Composable
inline fun <T, V1> key(
    @Suppress("UNUSED_PARAMETER")
    @Pivotal
    v1: V1,
    block: @Composable() () -> T
): T = block()

/**
 * [key] is a utility composable that is used to "group" or "key" a block of execution inside of a
 * composition. This is sometimes needed for correctness inside of control-flow that may cause a
 * given composable invocation to execute more than once during composition.
 *
 * The value for a key *does not need to be globally unique*, and needs only be unique amongst the
 * invocations of [key] *at that point* in composition.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.samples.locallyUniqueKeys
 *
 * Even though there are users with the same id composed in both the top and the bottom loop,
 * because they are different calls to [key], there is no need to create compound keys.
 *
 * The key must be unique for each element in the collection, however, or children and local state
 * might be reused in unintended ways.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.samples.notAlwaysUniqueKeys
 *
 * This example assumes that `parent.id` is a unique key for each item in the collection,
 * but this is only true if it is fair to assume that a parent will only ever have a single child,
 * which may not be the case.  Instead, it may be more correct to do the following:
 *
 * @sample androidx.compose.samples.moreCorrectUniqueKeys
 *
 * A compound key can be created by passing in multiple arguments:
 *
 * @sample androidx.compose.samples.twoInputsKeySample
 *
 * @param v1 The first value to use as a key. This will be compared to its previous value using
 * [equals]
 * @param v2 The second value to use as a key. This will be compared to its previous value using
 * [equals]
 * @param block The composable children for this group.
 */
@Composable
inline fun <T, V1, V2> key(
    @Suppress("UNUSED_PARAMETER")
    @Pivotal
    v1: V1,
    @Suppress("UNUSED_PARAMETER")
    @Pivotal
    v2: V2,
    block: @Composable() () -> T
) = block()

/**
 * [key] is a utility composable that is used to "group" or "key" a block of execution inside of a
 * composition. This is sometimes needed for correctness inside of control-flow that may cause a
 * given composable invocation to execute more than once during composition.
 *
 * The value for a key *does not need to be globally unique*, and needs only be unique amongst the
 * invocations of [key] *at that point* in composition.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.samples.locallyUniqueKeys
 *
 * Even though there are users with the same id composed in both the top and the bottom loop,
 * because they are different calls to [key], there is no need to create compound keys.
 *
 * The key must be unique for each element in the collection, however, or children and local state
 * might be reused in unintended ways.
 *
 * For instance, consider the following example:
 *
 * @sample androidx.compose.samples.notAlwaysUniqueKeys
 *
 * This example assumes that `parent.id` is a unique key for each item in the collection,
 * but this is only true if it is fair to assume that a parent will only ever have a single child,
 * which may not be the case.  Instead, it may be more correct to do the following:
 *
 * @sample androidx.compose.samples.moreCorrectUniqueKeys
 *
 * @param inputs The set of values to be used to create a compound key. These will be compared to
 * their previous values using [equals]
 * @param block The composable children for this group.
 */
@Composable
inline fun <T> key(
    @Suppress("UNUSED_PARAMETER")
    @Pivotal
    vararg inputs: Any?,
    block: @Composable() () -> T
) = block()
