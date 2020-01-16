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

private fun makeComposer(
    factory: (SlotTable, Recomposer) -> Composer<*>,
    parent: CompositionReference?,
    slotTable: SlotTable
): Composer<*> = factory(slotTable, Recomposer.current()).also {
    it.parentReference = parent
    parent?.registerComposer(it)
}

private val EmptyComposable: @Composable() () -> Unit = {}

/**
 * A Composition is an object that is used to manage the UI created from a Composable from the
 * top level. A composition object is usually constructed for you, and returned from an API that
 * is used to initially compose a UI. For instance, [Compose.composeInto] returns a Composition.
 *
 * The Composition object can be used to update the composition by calling the [compose] methods.
 * Similarly, the [dispose] method should be used when you would like to dispose of the UI and
 * the Composition.
 *
 * @param composerFactory A function to create a composer object, for use during composition
 * @param parent An optional reference to the parent composition.
 *
 * @see Compose.composeInto
 */
open class Composition(
    private val composerFactory: (SlotTable, Recomposer) -> Composer<*>,
    private val parent: CompositionReference? = null
) {
    private val slotTable: SlotTable = SlotTable()
    internal val composer: Composer<*> = makeComposer(composerFactory, parent, slotTable)
    internal var composable: @Composable() () -> Unit = EmptyComposable

    /**
     * Update the composition with the content described by the [content] composable
     *
     * @param content A composable function that describes the UI
     */
    fun compose(content: @Composable() () -> Unit) {
        composable = content
        compose()
    }

    /**
     * Recompose the composition with the same composable that the Composition was last composed
     * with
     */
    fun compose() {
        Recomposer.recompose(composable, composer)
    }

    /**
     * Clear the hierarchy that was created from the composition.
     */
    fun dispose() {
        composable = EmptyComposable
        compose()
    }

    /**
     * Recomposes any changes without forcing the [composable] to compose and blocks until
     * composition completes.
     *
     * @return true if there were pending changes, false otherwise.
     */
    @TestOnly
    fun recomposeSync(): Boolean {
        return Recomposer.current().recomposeSync(composer)
    }
}