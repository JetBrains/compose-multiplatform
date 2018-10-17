package com.google.r4a

interface Emittable {
    fun emitInsertAt(index: Int, instance: Emittable)
    fun emitRemoveAt(index: Int, count: Int)
    fun emitMove(from: Int, to: Int, count: Int)
}