package com.google.r4a

import com.google.r4a.frames.Framed
import com.google.r4a.frames.Record

@Composable
abstract class Component : Framed {
    abstract fun compose()
    protected fun recompose() {
        CompositionContext.recompose(this)
    }

    protected lateinit var next: Record
    override val first: Record get() = next
    override fun prepend(value: Record) {
        value.next = next
        next = value
    }
}
