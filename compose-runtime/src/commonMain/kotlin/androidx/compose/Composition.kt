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
package androidx.compose

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
}

/**
 * This method is the way to initiate a composition. Optionally, a [parent]
 * [CompositionReference] can be provided to make the composition behave as a sub-composition of
 * the parent.  The children of [container] will be updated and maintained by the time this
 * method returns.
 *
 * It is important to call [Composition.dispose] whenever this [container] is no longer needed in
 * order to release resources.
 *
 * @param container The container whose content is being composed.
 * @param recomposer The [Recomposer] to coordinate scheduling of composition updates.
 * @param parent The parent composition reference, if applicable. Default is null.
 * @param composerFactory The factory used to created a [Composer] to be used by the composition.
 */
@Deprecated("Use the compositionFor(...) overload that accepts an Applier<N>")
fun compositionFor(
    container: Any,
    recomposer: Recomposer,
    parent: CompositionReference? = null,
    composerFactory: (SlotTable, Recomposer) -> Composer<*>
): Composition = Compositions.findOrCreate(container) {
    CompositionImpl(recomposer, parent, composerFactory) {
        Compositions.onDisposed(container)
    }
}

/**
 * This method is the way to initiate a composition. Optionally, a [parent]
 * [CompositionReference] can be provided to make the composition behave as a sub-composition of
 * the parent.  The children of [container] will be updated and maintained by the time this
 * method returns.
 *
 * It is important to call [Composition.dispose] whenever this [container] is no longer needed in
 * order to release resources.
 *
 * @param container The container whose content is being composed.
 * @param parent The parent composition reference, if applicable. Default is null.
 * @param composerFactory The factory used to created a [Composer] to be used by the composition.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Specify the Recomposer explicitly",
    ReplaceWith(
        "compositionFor(container, Recomposer.current(), parent, composerFactory)",
        "androidx.compose.Recomposer"
    )
)
fun compositionFor(
    container: Any,
    parent: CompositionReference? = null,
    composerFactory: (SlotTable, Recomposer) -> Composer<*>
): Composition = compositionFor(container, Recomposer.current(), parent, composerFactory)

/**
 * This method is the way to initiate a composition. Optionally, a [parent]
 * [CompositionReference] can be provided to make the composition behave as a sub-composition of
 * the parent.
 *
 * It is important to call [Composition.dispose] whenever this [key] is no longer needed in
 * order to release resources.
 *
 * @sample androidx.compose.samples.CustomTreeComposition
 *
 * @param key The object this composition will be tied to. Only one [Composition] will be created
 * for a given [key]. If the same [key] is passed in subsequent calls, the same [Composition]
 * instance will be returned.
 * @param applier The [Applier] instance to be used in the composition.
 * @param recomposer The [Recomposer] instance to be used for composition.
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
    recomposer: Recomposer,
    parent: CompositionReference? = null,
    onCreated: () -> Unit = {}
): Composition = Compositions.findOrCreate(key) {
    CompositionImpl(
        recomposer,
        parent,
        composerFactory = { slots, rcmpsr -> Composer(slots, applier, rcmpsr) },
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
    private val recomposer: Recomposer,
    parent: CompositionReference?,
    composerFactory: (SlotTable, Recomposer) -> Composer<*>,
    private val onDispose: () -> Unit
) : Composition {
    private val slotTable: SlotTable = SlotTable()
    private val composer: Composer<*> = composerFactory(slotTable, recomposer).also {
        it.parentReference = parent
        parent?.registerComposer(it)
    }

    /**
     * Return true if this is a root (non-sub-) composition.
     */
    val isRoot: Boolean = parent == null

    private var disposed = false

    var composable: @Composable () -> Unit = emptyContent()

    override fun setContent(content: @Composable () -> Unit) {
        check(!disposed) { "The composition is disposed" }
        this.composable = content
        recomposer.composeInitial(composable, composer)
    }

    override fun dispose() {
        if (!disposed) {
            setContent(emptyContent())
            slotTable.read { reader ->
                for (index in 0 until slotTable.size) {
                    val value = reader.get(index)
                    if (value is RecomposeScope) {
                        value.inTable = false
                    }
                }
            }
            onDispose()
            disposed = true
        }
    }
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
