package com.google.r4a

internal class IntStack {
    private var slots = IntArray(10)
    private var tos = 0

    val size: Int get() = tos

    fun push(value: Int) {
        if (tos >= slots.size) {
            slots = slots.copyOf(slots.size * 2)
        }
        slots[tos++] = value
    }

    fun pop(): Int = slots[--tos]
    fun peek() = slots[tos - 1]
    fun peek(index: Int) = slots[index]
    fun isEmpty() = tos == 0
    fun isNotEmpty() = tos != 0
    fun clear() { tos = 0 }
}