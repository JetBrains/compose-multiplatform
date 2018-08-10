package com.google.r4a

import java.util.*

/**
 * An adapter that performs tree based operations on some tree startNode N without requiring a specific base type for N
 */
interface ApplyAdapter<N> {
    fun N.insertAt(index: Int, instance: N)
    fun N.removeAt(index: Int, count: Int)
    fun N.move(from: Int, to: Int, count: Int)
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
    }

    fun up() {
        _current = stack.pop()
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
