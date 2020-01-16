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

package androidx.compose.mock

import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composer
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.cache
import androidx.compose.invalidate
import androidx.compose.runWithCurrent

interface MockViewComposition {
    val cc: Composer<View>
}

abstract class ViewComponent : MockViewComposition {
    private var recomposer: (() -> Unit)? = null
    private lateinit var composer: Composer<View>
    @PublishedApi
    internal fun setComposer(value: Composer<View>) {
        composer = value
    }

    override val cc: Composer<View> get() = composer

    fun recompose() {
        recomposer?.invoke()
    }

    operator fun invoke() {
        val cc = cc as MockViewComposer
        cc.startRestartGroup(0)
        recomposer = invalidate
        compose()
        cc.endRestartGroup()?.updateScope { invoke() }
    }

    abstract fun compose()
}

typealias Compose = MockViewComposition.() -> Unit

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
object ViewApplierAdapter :
    ApplyAdapter<View> {
    override fun View.start(instance: View) {}
    override fun View.insertAt(index: Int, instance: View) = addAt(index, instance)
    override fun View.removeAt(index: Int, count: Int) = removeAt(index, count)
    override fun View.move(from: Int, to: Int, count: Int) = moveAt(from, to, count)
    override fun View.end(instance: View, parent: View) {}
}

class MockViewComposer(
    val root: View
) : Composer<View>(
    SlotTable(),
    Applier(root, ViewApplierAdapter), object : Recomposer() {
        override fun recomposeSync() {}

        override fun scheduleChangesDispatch() {}

        override fun hasPendingChanges(): Boolean = false
    }) {
    private val rootComposer: MockViewComposition by lazy {
        object : MockViewComposition {
            override val cc: Composer<View> get() = this@MockViewComposer
        }
    }

    fun compose(composition: MockViewComposition.() -> Unit) {
        composeRoot {
            rootComposer.composition()
        }
    }
    fun recomposeWithCurrent() {
        runWithCurrent { recompose() }
    }
}

/* inline */ fun <N, /* reified */ V> Composer<N>.applyNeeded(value: V): Boolean =
    changed(value) && !inserting

inline fun <reified P1> MockViewComposition.memoize(
    key: Any,
    p1: P1,
    block: MockViewComposition.(p1: P1) -> Unit
) {
    cc.startGroup(key)
    if (!cc.changed(p1)) {
        cc.skipCurrentGroup()
    } else {
        cc.startGroup(key)
        block(p1)
        cc.endGroup()
    }
    cc.endGroup()
}

inline fun <V : View> MockViewComposition.emit(
    key: Any,
    noinline factory: () -> V,
    block: MockViewComposition.() -> Unit
) {
    cc.startNode(key)
    cc.emitNode(factory)
    block()
    cc.endNode()
}

inline fun <V : View, reified A1> MockViewComposition.emit(
    key: Any,
    noinline factory: () -> V,
    a1: A1,
    noinline set1: V.(A1) -> Unit
) {
    cc.startNode(key)
    cc.emitNode(factory)
    if (cc.changed(a1)) {
        cc.apply(a1, set1)
    }
    cc.endNode()
}

val invocation = Any()

inline fun MockViewComposition.call(
    key: Any,
    invalid: Composer<View>.() -> Boolean,
    block: () -> Unit
) = with(cc) {
    startGroup(key)
    if (invalid() || inserting) {
        startGroup(invocation)
        block()
        endGroup()
    } else {
        (cc as Composer<*>).skipCurrentGroup()
    }
    endGroup()
}

inline fun <T : ViewComponent> MockViewComposition.call(
    key: Any,
    ctor: () -> T,
    invalid: ComponentUpdater<T>.() -> Unit,
    block: (f: T) -> Unit
) = with(cc) {
    startGroup(key)
    val f = cache(true, ctor).apply { setComposer(this@with) }
    val updater = object : ComponentUpdater<T> {
        override val cc: Composer<View> = this@with
        override var changed: Boolean = false
        override val component = f
    }
    updater.invalid()
    if (updater.changed || inserting) {
        startGroup(invocation)
        block(f)
        endGroup()
    } else {
        skipCurrentGroup()
    }
    endGroup()
}

fun MockViewComposition.join(
    key: Any,
    block: (invalidate: () -> Unit) -> Unit
) {
    val myCC = cc as MockViewComposer
    myCC.startRestartGroup(key)
    block(invalidate)
    myCC.endRestartGroup()?.updateScope { join(key, block) }
}

interface ComponentUpdater<C> : MockViewComposition {
    val component: C
    var changed: Boolean
}

inline fun <C, reified V> ComponentUpdater<C>.set(value: V, noinline block: C.(V) -> Unit) {
    if (cc.changed(value)) {
        cc.apply<V, C>(value) { component.block(it) }
        component.block(value)
        changed = true
    }
}

inline fun <C, reified V> ComponentUpdater<C>.update(value: V, noinline block: C.(V) -> Unit) {
    if (cc.applyNeeded(value)) {
        cc.apply<V, C>(value) { component.block(it) }
        component.block(value)
        changed = true
    }
}
