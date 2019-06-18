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

import java.util.Stack

/**
 * An adapter that performs tree based operations on some tree startNode N without requiring a specific base type for N
 */
interface ApplyAdapter<N> {
    fun N.start(instance: N)
    fun N.insertAt(index: Int, instance: N)
    fun N.removeAt(index: Int, count: Int)
    fun N.move(from: Int, to: Int, count: Int)
    fun N.end(instance: N, parent: N)
}

/**
 * A helper class to apply changes to a tree with startNode types N given an apply adapter for type N
 */
class Applier<N>(root: N, private val adapter: ApplyAdapter<N>) {
    private val stack = Stack<N>()
    private var _current: N = root

    val current: N get() = _current

    fun down(node: N) {
        stack.push(current)
        _current = node
        with(adapter) {
            current.start(node)
        }
    }

    fun up() {
        val node = _current
        _current = stack.pop()
        with(adapter) {
            current.end(node, current)
        }
    }

    fun insert(index: Int, instance: N) {
        with(adapter) {
            current.insertAt(index, instance)
        }
    }

    fun remove(index: Int, count: Int) {
        with(adapter) {
            current.removeAt(index, count)
        }
    }

    fun move(from: Int, to: Int, count: Int) {
        with(adapter) {
            current.move(from, to, count)
        }
    }

    fun reset() {
        stack.clear()
    }
}
