/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue

/**
 * Provides [SaveableStateHolder] to be used with the lazy layout items.
 * This allows to save the state of the items like inner LazyRow scroll position which such
 * items are scrolled away in order to restore it when this item is visible again.
 * However on top of the default logic provided via [rememberSaveableStateHolder] this instance
 * will only save the items which are currently visible when the parent [SaveableStateRegistry]
 * triggers [SaveableStateRegistry.performSave] in order to save on the space we use in the
 * Android`s Bundle to not cause crash with TransactionTooLargeException.
 */
@Composable
internal fun LazySaveableStateHolderProvider(content: @Composable (SaveableStateHolder) -> Unit) {
    val currentRegistry = LocalSaveableStateRegistry.current
    val holder = rememberSaveable(
        currentRegistry, saver = LazySaveableStateHolder.saver(currentRegistry)
    ) {
        LazySaveableStateHolder(currentRegistry, emptyMap())
    }
    CompositionLocalProvider(LocalSaveableStateRegistry provides holder) {
        holder.wrappedHolder = rememberSaveableStateHolder()
        content(holder)
    }
}

private class LazySaveableStateHolder(
    private val wrappedRegistry: SaveableStateRegistry
) : SaveableStateRegistry by wrappedRegistry, SaveableStateHolder {

    constructor(
        parentRegistry: SaveableStateRegistry?,
        restoredValues: Map<String, List<Any?>>?
    ) : this(
        SaveableStateRegistry(restoredValues) {
            parentRegistry?.canBeSaved(it) ?: true
        }
    )

    var wrappedHolder by mutableStateOf<SaveableStateHolder?>(null)

    private val previouslyComposedKeys = mutableSetOf<Any>()

    override fun performSave(): Map<String, List<Any?>> {
        val holder = wrappedHolder
        if (holder != null) {
            previouslyComposedKeys.forEach {
                holder.removeState(it)
            }
        }
        return wrappedRegistry.performSave()
    }

    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        requireNotNull(wrappedHolder).SaveableStateProvider(key, content)
        DisposableEffect(key) {
            previouslyComposedKeys -= key
            onDispose {
                previouslyComposedKeys += key
            }
        }
    }

    override fun removeState(key: Any) {
        requireNotNull(wrappedHolder).removeState(key)
    }

    companion object {
        fun saver(parentRegistry: SaveableStateRegistry?) =
            Saver<LazySaveableStateHolder, Map<String, List<Any?>>>(save = {
                it.performSave().ifEmpty { null }
            }, restore = { restored ->
                LazySaveableStateHolder(parentRegistry, restored)
            })
    }
}
