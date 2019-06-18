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

// TODO(lmr): this should be named Composer. also we can probably remove
abstract class Composition<N> {
    abstract val inserting: Boolean

    abstract fun startGroup(key: Any)
    abstract fun endGroup()
    abstract fun skipGroup()

    abstract fun startNode(key: Any)
    // NOTE(lmr): When we move to a model where composition defers creations/mutations to nodes
    // to the applyChanges phase, we will want to move all usages of emitNode(N) to createNode(() -> T)
    abstract fun <T : N> emitNode(factory: () -> T) // Deprecated // TODO(lmr): only used in mock
    abstract fun <T : N> createNode(factory: () -> T) // TODO(lmr): never used. replace usages of emitNode?
    abstract fun emitNode(node: N) // Deprecated - single threaded
    abstract fun useNode(): N
    abstract fun endNode()

    abstract fun joinKey(left: Any?, right: Any?): Any

    abstract fun nextSlot(): Any?

    abstract fun skipValue()
    abstract fun updateValue(value: Any?)
    abstract fun <V, T> apply(value: V, block: T.(V) -> Unit)
}

fun <N> Composition<N>.nextValue(): Any? = nextSlot().let {
    if (it is CompositionLifecycleObserverHolder) it.instance else it
}

inline fun <N, T> Composition<N>.cache(valid: Boolean = true, block: () -> T): T {
    var result = nextValue()
    if (result === SlotTable.EMPTY || !valid) {
        val value = block()
        updateValue(value)
        result = value
    } else skipValue()

    @Suppress("UNCHECKED_CAST")
    return result as T
}

/* inline */ fun <N, /* reified */ V> Composition<N>.changed(value: V): Boolean {
    return if (nextSlot() != value) {
        updateValue(value)
        true
    } else {
        skipValue()
        false
    }
}

/* inline */ fun <N, V> Composition<N>.remember(block: () -> V): V = cache(true, block)

/* inline */ fun <N, V, /* reified */ P1> Composition<N>.remember(p1: P1, block: () -> V) =
    cache(!changed(p1), block)

/* inline */ fun <
        N,
        V,
        /* reified */
        P1,
        /* reified */
        P2
        > Composition<N>.remember(p1: P1, p2: P2, block: () -> V): V {
    var valid = !changed(p1)
    valid = !changed(p2) && valid
    return cache(valid, block)
}

/* inline */ fun <
        N,
        V,
        /* reified */
        P1,
        /* reified */
        P2,
        /* reified */
        P3
        > Composition<N>.remember(p1: P1, p2: P2, p3: P3, block: () -> V): V {
    var valid = !changed(p1)
    valid = !changed(p2) && valid
    valid = !changed(p3) && valid
    return cache(valid, block)
}

/* inline */ fun <
        N,
        V,
        /* reified */
        P1,
        /* reified */
        P2,
        /* reified */
        P3,
        /* reified */
        P4
        > Composition<N>.remember(
            p1: P1,
            p2: P2,
            p3: P3,
            p4: P4,
            block: () -> V
        ): V {
    var valid = !changed(p1)
    valid = !changed(p2) && valid
    valid = !changed(p3) && valid
    valid = !changed(p4) && valid
    return cache(valid, block)
}

/* inline */ fun <N, V> Composition<N>.remember(vararg args: Any?, block: () -> V): V {
    var valid = true
    for (arg in args) valid = !changed(arg) && valid
    return cache(valid, block)
}
