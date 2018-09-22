package com.google.r4a

import com.google.r4a.frames.Framed
import com.google.r4a.frames.Record

@Composable
abstract class Component : Framed, Recomposable {
    internal var recomposeCallback: (() -> Unit)? = null

    protected fun recompose() {
        CompositionContext.recompose(this)
    }

    protected fun recomposeSync() {
        CompositionContext.recomposeSync(this)
    }

    protected lateinit var _firstFrameRecord: Record
    override val firstFrameRecord: Record get() = _firstFrameRecord

    override fun prependFrameRecord(value: Record) {
        value.next = _firstFrameRecord
        _firstFrameRecord = value
    }

    override fun setRecompose(recompose: () -> Unit) {
        this.recomposeCallback = recompose
    }
}
