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

@file:OptIn(InternalComposeApi::class)
package androidx.compose.runtime

/**
 * A composition object is usually constructed for you, and returned from an API that
 * is used to initially compose a UI. For instance, [setContent] returns a Composition.
 *
 * The [dispose] method should be used when you would like to dispose of the UI and
 * the Composition.
 */
interface Composition {
    /**
     * Update the composition with the content described by the [content] composable
     *
     * @param content A composable function that describes the UI
     */
    fun setContent(content: @Composable () -> Unit)

    /**
     * Clear the hierarchy that was created from the composition.
     */
    fun dispose()

    /**
     * Returns true if any pending invalidations have been scheduled.
     */
    fun hasInvalidations(): Boolean
}

/**
 * This method is the way to initiate a composition. Optionally, a [parent]
 * [CompositionReference] can be provided to make the composition behave as a sub-composition of
 * the parent.
 *
 * It is important to call [Composition.dispose] whenever this [key] is no longer needed in
 * order to release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 *
 * @param key The object this composition will be tied to. Only one [Composition] will be created
 * for a given [key]. If the same [key] is passed in subsequent calls, the same [Composition]
 * instance will be returned.
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent composition reference, if applicable. Default is null.
 * @param onCreated A function which will be executed only when the Composition is created.
 *
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
@ExperimentalComposeApi
fun compositionFor(
    key: Any,
    applier: Applier<*>,
    parent: CompositionReference,
    onCreated: () -> Unit = {}
): Composition = Compositions.findOrCreate(key) {
    CompositionImpl(
        parent,
        composerFactory = { parent -> Composer(applier, parent) },
        onDispose = { Compositions.onDisposed(key) }
    ).also {
        onCreated()
    }
}

/**
 * @param parent An optional reference to the parent composition.
 * @param composerFactory A function to create a composer object, for use during composition
 * @param onDispose A callback to be triggered when [dispose] is called.
 */
private class CompositionImpl(
    private val parent: CompositionReference,
    composerFactory: (CompositionReference) -> Composer<*>,
    private val onDispose: () -> Unit
) : Composition {
    private val composer: Composer<*> = composerFactory(parent).also {
        parent.registerComposer(it)
    }

    /**
     * Return true if this is a root (non-sub-) composition.
     */
    val isRoot: Boolean = parent is Recomposer

    private var disposed = false

    var composable: @Composable () -> Unit = emptyContent()

    override fun setContent(content: @Composable () -> Unit) {
        check(!disposed) { "The composition is disposed" }
        this.composable = content
        parent.composeInitial(composer, composable)
    }

    @OptIn(ExperimentalComposeApi::class)
    override fun dispose() {
        if (!disposed) {
            disposed = true
            composable = emptyContent()
            composer.dispose()
            onDispose()
        }
    }

    override fun hasInvalidations() = composer.hasInvalidations()
}

/**
 * Keeps all the active compositions.
 * This object is thread-safe.
 */
private object Compositions {
    private val holdersMap = WeakHashMap<Any, CompositionImpl>()

    fun findOrCreate(root: Any, create: () -> CompositionImpl): CompositionImpl =
        synchronized(holdersMap) {
            holdersMap[root] ?: create().also { holdersMap[root] = it }
        }

    fun onDisposed(root: Any) {
        synchronized(holdersMap) {
            holdersMap.remove(root)
        }
    }

    fun clear() {
        synchronized(holdersMap) {
            holdersMap.clear()
        }
    }

    fun collectAll(): List<CompositionImpl> = synchronized(holdersMap) {
        holdersMap.values.toList()
    }
}

/**
 * Apply Code Changes will invoke the two functions before and after a code swap.
 *
 * This forces the whole view hierarchy to be redrawn to invoke any code change that was
 * introduce in the code swap.
 *
 * All these are private as within JVMTI / JNI accessibility is mostly a formality.
 */
private class HotReloader {
    companion object {
        private var state = mutableListOf<Pair<CompositionImpl, @Composable () -> Unit>>()

        @TestOnly
        fun clearRoots() {
            Compositions.clear()
        }

        // Called before Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun saveStateAndDispose(context: Any) {
            state.clear()
            val holders = Compositions.collectAll()
            holders.mapTo(state) { it to it.composable }
            holders.filter { it.isRoot }.forEach { it.setContent(emptyContent()) }
        }

        // Called after Dex Code Swap
        @Suppress("UNUSED_PARAMETER")
        private fun loadStateAndCompose(context: Any) {
            val roots = mutableListOf<CompositionImpl>()
            state.forEach { (composition, composable) ->
                composition.composable = composable
                if (composition.isRoot) {
                    roots.add(composition)
                }
            }
            roots.forEach { it.setContent(it.composable) }
            state.clear()
        }

        @TestOnly
        internal fun simulateHotReload(context: Any) {
            saveStateAndDispose(context)
            loadStateAndCompose(context)
        }
    }
}

/**
 * @suppress
 */
@TestOnly
fun simulateHotReload(context: Any) = HotReloader.simulateHotReload(context)

/**
 * @suppress
 */
@TestOnly
fun clearRoots() = HotReloader.clearRoots()
