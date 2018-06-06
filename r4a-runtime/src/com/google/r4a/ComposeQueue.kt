package com.google.r4a

import java.util.*

internal class ComposeQueue<T>(
    comparator: (T, T) -> Int
) {
    private val queue = PriorityQueue<T>(comparator)
    private val elements = HashSet<T>()

    fun isNotEmpty() = queue.size > 0
    fun pop() = queue.poll()?.also { elements.remove(it) }
    fun add(el: T) {
        if (elements.contains(el)) return
        queue.add(el)
        elements.add(el)
    }
    fun remove(el: T) {
        if (!elements.contains(el)) return
        elements.remove(el)
        queue.remove(el)
    }
}