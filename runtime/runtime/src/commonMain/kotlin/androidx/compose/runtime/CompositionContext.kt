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

package androidx.compose.runtime

import kotlinx.collections.immutable.persistentHashMapOf
import kotlin.coroutines.CoroutineContext

private val EmptyCompositionLocalMap: CompositionLocalMap = persistentHashMapOf()

/**
 * An Effect to construct a [CompositionContext] at the current point of composition. This can be
 * used to run a separate composition in the context of the current one, preserving
 * [CompositionLocal]s and propagating invalidations. When this call leaves the composition, the
 * context is invalidated.
 */
@OptIn(InternalComposeApi::class)
@Composable fun rememberCompositionContext(): CompositionContext {
    return currentComposer.buildContext()
}

@Deprecated(
    "Renamed to rememberCompositionContext",
    ReplaceWith(
        "rememberCompositionContext()",
        "androidx.compose.runtime.rememberCompositionContext"
    )
)
@Composable fun rememberCompositionReference() = rememberCompositionContext()

@Deprecated("Renamed to CompositionContext")
typealias CompositionReference = CompositionContext

/**
 * A [CompositionContext] is an opaque type that is used to logically "link" two compositions
 * together. The [CompositionContext] instance represents a reference to the "parent" composition
 * in a specific position of that composition's tree, and the instance can then be given to a new
 * "child" composition. This reference ensures that invalidations and [CompositionLocal]s flow
 * logically through the two compositions as if they were not separate.
 *
 * The "parent" of a root composition is a [Recomposer].
 *
 * @see rememberCompositionContext
 */
@OptIn(InternalComposeApi::class)
abstract class CompositionContext internal constructor() {
    internal abstract val compoundHashKey: Int
    internal abstract val collectingKeySources: Boolean
    internal abstract val collectingParameterInformation: Boolean
    internal abstract val effectCoroutineContext: CoroutineContext
    internal abstract fun composeInitial(
        composition: ControlledComposition,
        content: @Composable () -> Unit
    )
    internal abstract fun invalidate(composition: ControlledComposition)

    internal open fun recordInspectionTable(table: MutableSet<CompositionData>) {}
    internal open fun registerComposer(composer: Composer) { }
    internal open fun unregisterComposer(composer: Composer) { }
    internal abstract fun registerComposition(composition: ControlledComposition)
    internal abstract fun unregisterComposition(composition: ControlledComposition)

    internal open fun <T> getCompositionLocal(key: CompositionLocal<T>): T =
        key.defaultValueHolder.value
    internal open fun getCompositionLocalScope(): CompositionLocalMap = EmptyCompositionLocalMap
    internal open fun startComposing() {}
    internal open fun doneComposing() {}
}
