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

package androidx.compose.ui.input.nestedscroll

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope

internal val ModifierLocalNestedScroll = modifierLocalOf<NestedScrollModifierLocal?> { null }

/**
 * NestedScroll using ModifierLocal as implementation.
 */
internal class NestedScrollModifierLocal(
    val dispatcher: NestedScrollDispatcher,
    val connection: NestedScrollConnection
) : ModifierLocalConsumer, ModifierLocalProvider<NestedScrollModifierLocal?>,
    NestedScrollConnection {
    init {
        dispatcher.calculateNestedScrollScope = { nestedCoroutineScope }
    }

    private var parent: NestedScrollModifierLocal? by mutableStateOf(null)

    private val nestedCoroutineScope: CoroutineScope
        get() = parent?.nestedCoroutineScope
            ?: dispatcher.originNestedScrollScope
            ?: throw IllegalStateException(
                "in order to access nested coroutine scope you need to attach dispatcher to the " +
                    "`Modifier.nestedScroll` first."
            )

    override val key: ProvidableModifierLocal<NestedScrollModifierLocal?>
        get() = ModifierLocalNestedScroll

    override val value: NestedScrollModifierLocal
        get() = this

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        parent = ModifierLocalNestedScroll.current
        dispatcher.parent = parent
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val parentPreConsumed = parent?.onPreScroll(available, source) ?: Offset.Zero
        val selfPreConsumed = connection.onPreScroll(available - parentPreConsumed, source)
        return parentPreConsumed + selfPreConsumed
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val selfConsumed = connection.onPostScroll(consumed, available, source)
        val parentConsumed =
            parent?.onPostScroll(consumed + selfConsumed, available - selfConsumed, source)
                ?: Offset.Zero
        return selfConsumed + parentConsumed
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val parentPreConsumed = parent?.onPreFling(available) ?: Velocity.Zero
        val selfPreConsumed = connection.onPreFling(available - parentPreConsumed)
        return parentPreConsumed + selfPreConsumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val selfConsumed = connection.onPostFling(consumed, available)
        val parentConsumed =
            parent?.onPostFling(consumed + selfConsumed, available - selfConsumed) ?: Velocity.Zero
        return selfConsumed + parentConsumed
    }
}
