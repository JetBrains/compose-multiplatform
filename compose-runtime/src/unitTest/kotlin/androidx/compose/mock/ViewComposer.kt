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
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.Stable
import androidx.compose.currentComposer
import androidx.compose.invokeComposable

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
object ViewApplierAdapter :
    ApplyAdapter<View> {
    override fun View.start(instance: View) {}
    override fun View.insertAt(index: Int, instance: View) = addAt(index, instance)
    override fun View.removeAt(index: Int, count: Int) = removeAt(index, count)
    override fun View.move(from: Int, to: Int, count: Int) = moveAt(from, to, count)
    override fun View.end(instance: View, parent: View) {}
}

typealias Updater<T> = ComposerUpdater<View, T>

@Stable
interface MockComposeScope {
    val composer: MockViewComposer
}

class MockViewComposer(
    val root: View
) : Composer<View>(
    SlotTable(),
    Applier(root, ViewApplierAdapter), object : Recomposer() {
        override fun recomposeSync() {}

        override fun scheduleChangesDispatch() {}

        override fun hasPendingChanges(): Boolean = false
    }), MockComposeScope {
    override val composer: MockViewComposer get() = this

    fun compose(composable: @Composable MockComposeScope.() -> Unit) {
        composeRoot {
            invokeComposable(this) {
                val c = currentComposer as MockViewComposer
                c.composable()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <V : View> emit(
        key: Any,
        ctor: () -> V,
        update: Updater<V>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as V
        Updater(this, node).update()
        endNode()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <V : View> emit(
        key: Any,
        ctor: () -> V,
        update: Updater<V>.() -> Unit,
        children: () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as V
        Updater(this, node).update()
        children()
        endNode()
    }
}

@Composable
fun <P1> MockComposeScope.memoize(
    key: Int,
    p1: P1,
    block: @Composable (p1: P1) -> Unit
) {
    with(currentComposer as MockViewComposer) {
        startGroup(key)
        if (!changed(p1)) {
            skipToGroupEnd()
        } else {
            block(p1)
        }
        endGroup()
    }
}
