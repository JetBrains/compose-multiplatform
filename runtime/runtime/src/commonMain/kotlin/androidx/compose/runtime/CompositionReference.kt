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

private val EmptyAmbientMap: AmbientMap = persistentHashMapOf()

/**
 * A [CompositionReference] is an opaque type that is used to logically "link" two compositions
 * together. The [CompositionReference] instance represents a reference to the "parent" composition
 * in a specific position of that composition's tree, and the instance can then be given to a new
 * "child" composition. This reference ensures that invalidations and ambients flow logically
 * through the two compositions as if they were not separate.
 *
 * The "parent" of a root composition is a [Recomposer].
 *
 * @see compositionReference
 */
@OptIn(InternalComposeApi::class)
abstract class CompositionReference internal constructor() {
    internal abstract val compoundHashKey: Int
    internal abstract val collectingKeySources: Boolean
    internal abstract val collectingParameterInformation: Boolean
    internal abstract val effectCoroutineContext: CoroutineContext
    internal abstract fun composeInitial(composer: Composer<*>, composable: @Composable () -> Unit)
    internal abstract fun invalidate(composer: Composer<*>)

    internal open fun recordInspectionTable(table: MutableSet<CompositionData>) {}
    internal open fun registerComposer(composer: Composer<*>) {
        registerComposerWithRoot(composer)
    }
    internal open fun unregisterComposer(composer: Composer<*>) {
        unregisterComposerWithRoot(composer)
    }
    internal abstract fun registerComposerWithRoot(composer: Composer<*>)
    internal abstract fun unregisterComposerWithRoot(composer: Composer<*>)

    internal open fun <T> getAmbient(key: Ambient<T>): T = key.defaultValueHolder.value
    internal open fun getAmbientScope(): AmbientMap = EmptyAmbientMap
    internal open fun startComposing() {}
    internal open fun doneComposing() {}
}
