package com.google.r4a

import android.view.Choreographer
import com.google.r4a.frames.open
import com.google.r4a.frames.commit
import com.google.r4a.frames.registerCommitObserver
import com.google.r4a.frames.inFrame

import java.util.*
import kotlin.collections.ArrayList

fun <T> isolated(block: () -> T) = FrameManager.isolated(block)

internal object FrameManager {
    private var started = false
    private var commitPending = false
    private var reclaimPending = false
    private var invalidations = WeakHashMap<Any, MutableSet<RecomposeScope>>()
    private var removeCommitObserver: (() -> Unit)? = null
    private var compositions = WeakHashMap<CompositionContext, Boolean>()

    fun ensureStarted() {
        if (!started) {
            started = true
            removeCommitObserver = registerCommitObserver(commitObserver)
            open()
        }
    }

    fun close() {
        synchronized(this) {
            invalidations.clear()
        }
        if (inFrame) commit()
        removeCommitObserver?.let { it() }
        started = false
        invalidations = WeakHashMap()
        compositions = WeakHashMap()
    }

    fun <T> isolated(block: () -> T): T {
        ensureStarted()
        try {
            return block()
        } finally {
            close()
        }
    }

    fun nextFrame() {
        if (inFrame) {
            commit()
            open()
        }
    }

    fun scheduleCleanup() {
        if (!reclaimPending && synchronized(this) {
                if (!reclaimPending) {
                    reclaimPending = true
                    true
                } else false
            }) {
            schedule(reclaimInvalid)
        }
    }

    fun registerComposition(composition: CompositionContext) {
        synchronized(this) {
            compositions[composition] = true
        }
    }

    private val readObserver: (read: Any) -> Unit = { read ->
        (CompositionContext.current as? ComposerCompositionContext)?.composer?.currentInvalidate?.let {
            synchronized(this) {
                invalidations.getOrPut(read) { mutableSetOf() }.add(it)
            }
        }
    }

    private val writeObserver: (write: Any) -> Unit = {
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                nextFrame()
            }
        }
    }

    private val commitObserver: (committed: Set<Any>) -> Unit = { committed ->
        val currentInvalidations = synchronized(this) {
            committed.mapNotNull {
                invalidations[it] as Set<RecomposeScope>?
            }.reduceSet()
        }
        currentInvalidations.forEach { scope -> scope.invalidate?.let { it() } }
        val currentRecomposes = synchronized(this) { ArrayList(compositions.keys) }
        currentRecomposes.forEach { it.recomposeAll() }
    }

    /***
     * Remove all invalidation scopes not currently part of a composition
     */
    private val reclaimInvalid: () -> Unit = {
        synchronized(this) {
            if (reclaimPending) {
                reclaimPending = false
                val removes = invalidations.mapNotNull { entry ->
                    val framed = entry.key
                    val invalidations = entry.value
                    invalidations.removeIf { !it.valid }
                    (if (invalidations.isEmpty()) framed else null)
                }
                removes.forEach { framed -> invalidations.remove(framed) }
            }
        }
    }

    private fun open() {
        open(readObserver = readObserver, writeObserver = writeObserver)
    }

    private fun schedule(block: () -> Unit) {
        Choreographer.getInstance().postFrameCallbackDelayed({ block() }, 0)
    }
}

private fun <T> Iterable<Set<T>>.reduceSet(): Set<T> {
    val iterator = iterator()
    if (!iterator.hasNext()) return emptySet<T>()
    var acc = iterator.next()
    while (iterator.hasNext()) {
        acc += iterator.next()
    }
    return acc
}