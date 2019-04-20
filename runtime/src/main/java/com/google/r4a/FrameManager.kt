package com.google.r4a

import android.os.Handler
import android.os.Looper
import com.google.r4a.frames.open
import com.google.r4a.frames.commit
import com.google.r4a.frames.suspend
import com.google.r4a.frames.restore
import com.google.r4a.frames.registerCommitObserver
import com.google.r4a.frames.inFrame
import java.lang.ref.WeakReference

fun <T> isolated(block: () -> T) = FrameManager.isolated(block)
fun <T> unframed(block: () -> T) = FrameManager.unframed(block)
fun <T> framed(block: () -> T) = FrameManager.framed(block)

fun nextFrame() = FrameManager.nextFrame()

/**
 * Ignore the object's implementation of hashCode and equals as they will change for data classes
 * that are mutated. The read observer needs to track the object identity, not the object value.
 */
private class WeakIdentity<T>(value: T) {
    // Save the hash code of value as it might be reclaimed making value.hashCode inaccessable
    private val myHc = System.identityHashCode(value)

    // Preserve a weak reference to the value to prevent read observers from leaking observed values
    private val weakValue = WeakReference(value)

    // Ignore the equality of value and use object identity instead
    override fun equals(other: Any?): Boolean =
        this === other || (other is WeakIdentity<*>) && other.value === value && value !== null
    override fun hashCode(): Int = myHc

    val value: T? get() = weakValue.get()
}

/**
 * The frame manager manages the priority frame in the main thread.
 *
 * Once the FrameManager has started there is always an open frame in the main thread. If a model object is committed in any
 * frame then the frame manager schedules the current frame to commit with the Choreographer and a new frame is open. Any
 * model objects read during composition are recorded in an invalidations map. If they are mutated during a frame the recompose
 * scope that was active during the read is invalidated.
 */
internal object FrameManager {
    private var started = false
    private var commitPending = false
    private var reclaimPending = false
    private var invalidations = HashMap<WeakIdentity<Any>, MutableSet<RecomposeScope>>()
    private var removeCommitObserver: (() -> Unit)? = null

    private val handler by lazy { Handler(Looper.getMainLooper()) }

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
        invalidations = HashMap()
    }

    fun <T> isolated(block: () -> T): T {
        ensureStarted()
        try {
            return block()
        } finally {
            close()
        }
    }

    fun <T> unframed(block: () -> T): T {
        if (inFrame) {
            val frame = suspend()
            try {
                val result = block()
                if (inFrame) error("An unframed block left a frame uncommitted or aborted")
                return result
            } finally {
                restore(frame)
            }
        } else return block()
    }

    fun <T> framed(block: () -> T): T {
        if (inFrame) {
            return block()
        } else {
            open()
            try {
                return block()
            } finally {
                commit()
            }
        }
    }

    fun nextFrame() {
        if (inFrame) {
            commit()
            open()
        }
    }

    fun scheduleCleanup() {
        if (started && !reclaimPending && synchronized(this) {
                if (!reclaimPending) {
                    reclaimPending = true
                    true
                } else false
            }) {
            schedule(reclaimInvalid)
        }
    }

    private val readObserver: (read: Any) -> Unit = { read ->
        currentComposer?.currentInvalidate?.let {
            synchronized(this) {
                invalidations.getOrPut(WeakIdentity(read)) { mutableSetOf() }.add(it)
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
                invalidations[WeakIdentity(it)] as Set<RecomposeScope>?
            }.reduceSet()
        }
        currentInvalidations.forEach { scope -> scope.invalidate?.let { it(false) } }
    }

    /**
     * Remove all invalidation scopes not currently part of a composition
     */
    private val reclaimInvalid: () -> Unit = {
        synchronized(this) {
            if (reclaimPending) {
                reclaimPending = false
                val removes = invalidations.mapNotNull loop@{ entry ->
                    val identity = entry.key
                    if (identity.value == null) return@loop identity
                    val invalidations = entry.value
                    invalidations.removeIfTrue { !it.valid }
                    (if (invalidations.isEmpty()) identity else null)
                }
                removes.forEach { identity -> invalidations.remove(identity) }
            }
        }
    }

    private fun open() {
        open(readObserver = readObserver, writeObserver = writeObserver)
    }

    private inline fun schedule(crossinline block: () -> Unit) {
        handler.post({ block() })
    }
}

private fun <T> Iterable<Set<T>>.reduceSet(): Set<T> {
    val iterator = iterator()
    if (!iterator.hasNext()) return emptySet<T>()
    var acc = mutableSetOf<T>() + iterator.next()
    while (iterator.hasNext()) {
        acc += iterator.next()
    }
    return acc
}

private fun <T> MutableSet<T>.removeIfTrue(predicate: (value: T) -> Boolean) {
    val items = this.iterator()
    while (items.hasNext()) {
        if (predicate(items.next())) {
            items.remove()
        }
    }
}