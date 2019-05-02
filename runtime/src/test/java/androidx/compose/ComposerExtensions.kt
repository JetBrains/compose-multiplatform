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

import android.content.Context
import android.view.View
import android.view.ViewGroup

inline fun ViewComposition.group(key: Int, block: () -> Unit) {
    try {
        composer.startGroup(key)
        block()
    } finally {
        composer.endGroup()
    }
}

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    ctor: () -> T,
    noinline block: ViewValidator.(f: T) -> Boolean
): Unit = emitComponent(loc, null, ctor, block)

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    ctor: () -> T
): Unit = emitComponent(loc, null, ctor, { true })

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    key: Int?,
    ctor: () -> T
): Unit = emitComponent(loc, key, ctor, { true })

inline fun <reified T : Component> ViewComposition.emitComponent(
    loc: Int,
    key: Int?,
    ctor: () -> T,
    noinline block: ViewValidator.(f: T) -> Boolean
): Unit = call(
    joinKey(loc, key),
    ctor,
    block,
    { f -> f() }
)

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit
): Unit = emitView(loc, null, ctor, updater)

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    ctor: (context: Context) -> T
): Unit = emitView(loc, null, ctor, {})

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T
): Unit = emitView(loc, key, ctor, {})

inline fun <reified T : View> ViewComposition.emitView(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit
): Unit = emit(
    joinKey(loc, key),
    ctor,
    updater
)

inline fun <reified T : ViewGroup> ViewComposition.emitViewGroup(
    loc: Int,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: () -> Unit
) = emitViewGroup(loc, null, ctor, updater, block)

inline fun <reified T : ViewGroup> ViewComposition.emitViewGroup(
    loc: Int,
    key: Int?,
    ctor: (context: Context) -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: @Composable() () -> Unit
) = emit(
    joinKey(loc, key),
    ctor,
    updater,
    block
)

inline fun <reified T : Emittable> ViewComposition.emitEmittable(
    loc: Int,
    ctor: () -> T,
    noinline updater: ViewUpdater<T>.() -> Unit
) = emitEmittable(loc, null, ctor, updater, {})

inline fun <reified T : Emittable> ViewComposition.emitEmittable(
    loc: Int,
    ctor: () -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: () -> Unit
) = emitEmittable(loc, null, ctor, updater, block)

inline fun <reified T : Emittable> ViewComposition.emitEmittable(
    loc: Int,
    key: Int?,
    ctor: () -> T,
    noinline updater: ViewUpdater<T>.() -> Unit,
    block: () -> Unit
) = emit(
    joinKey(loc, key),
    ctor,
    updater,
    block
)

inline fun <reified T> ViewComposition.provideAmbient(
    key: Ambient<T>,
    value: T,
    noinline children: @Composable() () -> Unit
) = call(
    0,
    { changed(key) + changed(value) + changed(children) },
    { @Suppress("PLUGIN_ERROR") key.Provider(value, children) }
)