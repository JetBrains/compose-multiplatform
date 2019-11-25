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

package androidx.compose.frames

import androidx.compose.ThreadLocal
import androidx.compose.synchronized

class FrameAborted(val frame: Frame) : RuntimeException("Frame aborted")

/**
 * Frame id of 0 is reserved as invalid and no state record with frame 0 is considered valid.
 *
 * The value 0 was chosen as it is the default value of the Int frame id type and records initially
 * created will naturally have a frame id of 0. If this wasn't considered invalid adding such a
 * record to a framed object will make the state record immediately visible to all frames instead of
 * being born invalid. Using 0 ensures all state records are created invalid and must be explicitly
 * marked as valid in for a frame.
 */
private const val INVALID_FRAME = 0

/**
 * The frame records are created with frame ID CREATION_FRAME when not in a frame.
 * This allows framed object to be created in the in static initializers when a
 * frame could not have been created yet.
 *
 * The value 1 was chosen because it must be greater than 0, as 0 is reserved to
 * indicated an invalid frame therefore 1 is the lowest valid frame.
 */
private const val CREATION_FRAME = 1

/**
 * Base implementation of a frame record
 */
abstract class AbstractRecord : Record {
    override var frameId: Int = threadFrame.get()?.id ?: CREATION_FRAME
    override var next: Record? = null
}

/**
 * Frame local values of a framed object.
 */
interface Record {
    /**
     * The frame id of the frame in which the record was created.
     */
    var frameId: Int

    /**
     * Reference of the next frame record. Frame records are stored in a linked list.
     */
    var next: Record?

    /**
     * Copy the value into this frame record from another for the same framed object.
     */
    fun assign(value: Record)

    /**
     * Create a new frame record for the same framed object.
     */
    fun create(): Record
}

/**
 * Interface implemented by all model objects. Used by this module to maintain the state records
 * of a model object.
 */
interface Framed {
    /**
     * The first state record in a linked list of state records.
     */
    val firstFrameRecord: Record

    /**
     * Add a new state record to the beginning of a list. After this call [firstFrameRecord] should
     * be [value].
     */
    fun prependFrameRecord(value: Record)
}

typealias FrameReadObserver = (read: Any) -> Unit
typealias FrameWriteObserver = (write: Any) -> Unit
typealias FrameCommitObserver = (committed: Set<Any>, frame: Frame) -> Unit

private val threadFrame = ThreadLocal<Frame>()

/**
 * Information about a frame including the frame id and whether or not it is read only.
 */
class Frame internal constructor(
    /**
     * The id of the frame. This value is monotonically increasing for each frame created.
     */
    val id: Int,

    /**
     * A set of all the frames that should be treated as invalid. That is the set of all frames open
     * or aborted.
     */
    internal val invalid: FrameIdSet,

    /**
     * True if the frame is read only
     */
    readOnly: Boolean,

    /**
     * Observe a frame read
     */
    internal val readObserver: FrameReadObserver?,

    /**
     * Observe a frame write
     */
    internal val writeObserver: FrameWriteObserver?,

    /**
     * The reference to the thread local list of observers from [threadReadObservers].
     * We store it here to save on an additional ThreadLocal.get() call during
     * the every model read.
     */
    internal var threadReadObservers: MutableList<FrameReadObserver>
) {
    internal val modified = if (readOnly) null else HashSet<Framed>()

    /**
     * True if any change to a frame object will throw.
     */
    val readonly: Boolean
        get() = modified == null

    /**
     * Whether there are any pending changes in this frame.
     */
    fun hasPendingChanges(): Boolean = (modified?.size ?: 0) > 0
}

/**
 * Holds the thread local list of [FrameReadObserver]s not associated with any specific [Frame].
 * They survives [Frame]s switch.
 */
private val threadReadObservers = ThreadLocal { mutableListOf<FrameReadObserver>() }

/**
 * [FrameReadObserver] will be called for every frame read happened on the current
 * thread during execution of the [block].
 */
fun observeAllReads(readObserver: FrameReadObserver, block: () -> Unit) {
    val observers = threadReadObservers.get()
    try {
        observers.add(readObserver)
        block()
    } finally {
        observers.remove(readObserver)
    }
}

private fun validateNotInFrame() {
    if (threadFrame.get() != null) throw IllegalStateException("In an existing frame")
}

/**
 * Return the thread's active frame. This will throw if no frame is active for the thread.
 */
fun currentFrame(): Frame {
    return threadFrame.get() ?: throw IllegalStateException("Not in a frame")
}

val inFrame: Boolean get() = threadFrame.get() != null

// A global synchronization object. This synchronization object should be taken before modifying any
// of the fields below.
private val sync = Any()

// The following variables should only be written when sync is taken
private var openFrames = FrameIdSet.EMPTY

// The first frame created must be at least on more than the CREATION_FRAME so objects
// created ouside a frame (that use the CREATION_FRAME as there id) and modified in the first
// frame will be seen as modified.
private var maxFrameId = CREATION_FRAME + 1

private fun open(
    readOnly: Boolean,
    readObserver: FrameReadObserver?,
    writeObserver: FrameWriteObserver?
): Frame {
    validateNotInFrame()
    val threadReadObservers = threadReadObservers.get()
    synchronized(sync) {
        val id = maxFrameId++
        val invalid = openFrames
        val frame = Frame(
            id = id,
            invalid = invalid,
            readOnly = readOnly,
            readObserver = readObserver,
            writeObserver = writeObserver,
            threadReadObservers = threadReadObservers
        )
        openFrames = openFrames.set(id)
        threadFrame.set(frame)
        return frame
    }
}

/**
 * Open a frame
 *
 * @param readOnly true if the frame can only be read from
 * @return the newly created frame's data
 */
fun open(readOnly: Boolean = false) =
    open(readOnly, null, null)

/**
 * Open a frame with observers
 */
fun open(readObserver: FrameReadObserver? = null, writeObserver: FrameWriteObserver? = null) =
    open(false, readObserver, writeObserver)

/*
 * Commits the pending frame if there one is open. Intended to be used in a `finally` clause
 */
fun commitHandler() = threadFrame.get()?.let {
    commit(it)
}

/**
 * Commit the current pending frame. Throws FrameAborted if changes in the frame collides with the
 * current committed frame. Throws IllegalStateException no frame is open (use `commitHandler()` to
 * commit a frame if one is open).
 */
fun commit() = commit(currentFrame())

/**
 * Returns true if the given object framed object mutated in the the frame
 */
fun wasModified(value: Any) = currentFrame().modified?.contains(value) ?: false

private var commitListeners = mutableListOf<FrameCommitObserver>()

fun registerCommitObserver(observer: FrameCommitObserver): () -> Unit {
    synchronized(sync) {
        commitListeners.add(observer)
    }
    return {
        synchronized(sync) {
            commitListeners.remove(observer)
        }
    }
}

/**
 * Commit the given frame. Throws FrameAborted if changes in the frame collides with the current
 * committed frame.
 */
fun commit(frame: Frame) {
    // NOTE: the this algorithm is currently does not guarantee a serializable frame operation as it
    // doesn't prevent crossing writes as described here https://arxiv.org/pdf/1412.2324.pdf

    // Just removing the frame from the open frame set is enough to make it visible, however, this
    // should only be done after first determining that there are no colliding writes in the commit.

    // A write is considered colliding if any write occurred on the object in a frame committed
    // since the frame was last opened. There is a trivial cases that can be dismissed immediately,
    // no writes occurred.
    val modified = frame.modified
    val id = frame.id
    val listeners = synchronized(sync) {
        if (!openFrames.get(id)) throw IllegalStateException("Frame not open")
        if (modified == null || modified.size == 0) {
            closeFrame(frame)
            emptyList()
        } else {
            // If there are modifications we need to ensure none of the modifications have
            // collisions.

            // A record is guaranteed not collide if no other write was performed to the record by a
            // committed frame since this frame was opened. No writes to a framed object occurred
            // if, ignoring this frame, the readable records for the framed object are the same. If
            // they are different, and the records could be merged, (such as considering writes to
            // different fields as not colliding) could be allowed here but, for now, the all writes
            // to a record are considered atomic. Additionally, if the field values can be merged
            // (e.g. using a conflict-free data type) this could also be allowed here.

            val current = openFrames
            val nextFrame = maxFrameId
            val start = frame.invalid.set(id)
            for (framed in frame.modified) {
                val first = framed.firstFrameRecord
                if (readable(
                        first,
                        nextFrame,
                        current
                    ) != readable(first, id, start)
                ) {
                    abort(frame)
                }
            }
            closeFrame(frame)
            commitListeners.toList()
        }
    }
    if (modified != null)
        for (commitListener in listeners) {
            commitListener(modified, frame)
        }
}

/**
 * Throw an exception if a frame is not open
 */
private fun validateOpen(frame: Frame) {
    if (!openFrames.get(frame.id)) throw IllegalStateException("Frame not open")
}

/**
 * Abort the current frame. Throws FrameAborted if a frame is open. Throws IllegalStateException if
 * no frame is open (use `abortHandler` to abort a frame without throwing an exception or to abort a
 * frame if one is open).
 */
fun abort() {
    abort(currentFrame())
}

/**
 * Abort the given frame and throw a FrameAborted exception.
 */
fun abort(frame: Frame) {
    abortHandler(frame)
    throw FrameAborted(frame)
}

/**
 * Abort the current frame if one is open. This is intended to be used in a catch handler to abort
 * the frame and then rethrow the exception.
 */
fun abortHandler() {
    threadFrame.get()?.let { abortHandler(it) }
}

/**
 * Abort the given frame.
 */
fun abortHandler(frame: Frame) {
    validateOpen(frame)

    // Mark all state records created in this frame as invalid
    frame.modified?.let { modified ->
        val id = frame.id
        for (framed in modified) {
            var current: Record? = framed.firstFrameRecord
            while (current != null) {
                if (current.frameId == id) {
                    current.frameId = INVALID_FRAME
                    break
                }
                current = current.next
            }
        }
    }

    // The frame can now be closed.
    closeFrame(frame)
}

/**
 * Suspend the given frame. After calling suspend() the thread's no longer has an open frame. Call
 * `restore()` to restore a suspended thread.
 */
fun suspend(): Frame {
    val frame = currentFrame()
    threadFrame.set(null)
    return frame
}

/**
 * Restore the given frame to the thread.
 */
fun restore(frame: Frame) {
    validateNotInFrame()
    validateOpen(frame)
    frame.threadReadObservers = threadReadObservers.get()
    threadFrame.set(frame)
}

private fun closeFrame(frame: Frame) {
    synchronized(sync) {
        openFrames = openFrames.clear(frame.id)
    }
    threadFrame.set(null)
}

private fun valid(currentFrame: Int, candidateFrame: Int, invalid: FrameIdSet): Boolean {
    // A candidate frame is valid if the it is less than or equal to the current frame
    // and it wasn't specifically marked as invalid when the frame started.
    //
    // All frames open at the start of the current frame are considered invalid for a frame (they
    // have not been committed and therefore are considered invalid).
    //
    // All frames born after the current frame are considered invalid since they occur after the
    // current frame was open.
    //
    // INVALID_FRAME is reserved as an invalid frame.
    return candidateFrame != INVALID_FRAME && candidateFrame <= currentFrame &&
            !invalid.get(candidateFrame)
}

// Determine if the given data is valid for the frame.
private fun valid(data: Record, frame: Int, invalid: FrameIdSet): Boolean {
    return valid(frame, data.frameId, invalid)
}

private fun <T : Record> readable(r: T, id: Int, invalid: FrameIdSet): T {
    // The readable record is the valid record with the highest frameId
    var current: Record? = r
    var candidate: Record? = null
    while (current != null) {
        if (valid(current, id, invalid)) {
            candidate = if (candidate == null) current
            else if (candidate.frameId < current.frameId) current else candidate
        }
        current = current.next
    }
    if (candidate != null) {
        @Suppress("UNCHECKED_CAST")
        return candidate as T
    }
    throw IllegalStateException("Could not find a current")
}

fun <T : Record> T.readable(framed: Framed): T {
    val frame = currentFrame()
    // invoke the observer associated with the current frame.
    frame.readObserver?.invoke(framed)
    // invoke the thread local observers.
    frame.threadReadObservers.forEach { it(framed) }
    return readable(this, frame.id, frame.invalid)
}

fun _readable(r: Record, framed: Framed): Record = r.readable(framed)
fun _writable(r: Record, framed: Framed): Record = r.writable(framed)
fun _created(framed: Framed) = threadFrame.get()?.writeObserver?.let { it(framed) }

fun <T : Record> T.writable(framed: Framed): T {
    return this.writable(framed, currentFrame())
}

/**
 * A record can be reused if no other frame will see it as valid. This is always true for a record
 * created in an aborted frame. It is also true if the record is valid in the previous frame and is
 * obscured by another record also valid in the previous frame record.
 */
private fun used(framed: Framed, id: Int, invalid: FrameIdSet): Record? {
    var current: Record? = framed.firstFrameRecord
    var validRecord: Record? = null
    while (current != null) {
        val currentId = current.frameId
        if (currentId == INVALID_FRAME) {
            // Any frames that were marked invalid by an aborted frame can be used immediately.
            return current
        }
        if (valid(current, id - 1, invalid)) {
            if (validRecord == null) {
                validRecord = current
            } else {
                // If we have two valid records one must obscure the other. Return the
                // record with the lowest id
                return if (current.frameId < validRecord.frameId) current else validRecord
            }
        }
        current = current.next
    }
    return null
}

/**
 * Return a writable frame record for the given record. It is assumed that this is called for the
 * first framed record in a frame object. If the frame is read-only calling this will throw. A
 * record is writable if it was created in the current writable frame. A writable record will always
 * be the readable record (as all newer records are invalid it must be the newest valid record).
 * This means that if the readable record is not from the current frame, a new record must be
 * created. To create a new writable record, a record can be reused, if possible, and the readable
 * record is applied to it. If a record cannot be reused, a new record is created and the readable
 * record is applied to it. Once the values are correct the record is made live by giving it the
 * current frame id.
 */
fun <T : Record> T.writable(framed: Framed, frame: Frame): T {
    if (frame.readonly) throw IllegalStateException("In a readonly frame")
    val id = frame.id
    val readData = readable<T>(this, id, frame.invalid)

    // If the readable data was born in this frame, it is writable.
    if (readData.frameId == frame.id) return readData

    // The first write to an framed in frame
    frame.writeObserver?.let { it(framed) }

    // Otherwise, make a copy of the readable data and mark it as born in this frame, making it
    // writable.
    val newData = synchronized(framed) {
        // Calling used() on a framed object might return the same record for each thread calling
        // used() therefore selecting the record to reuse should be guarded.

        // Note: setting the frameId to Int.MAX_VALUE will make it invalid for all frames. This
        // means we can release the lock on the object as used() will no longer select it. Using id
        // could also be used but it puts the object into a state where the reused value appears to
        // be the current valid value for the the frame. This is not an issue if the frame is only
        // being read from a single thread but using Int.MAX_VALUE allows multiple readers, single
        // writer, of a frame. Note that threads reading a mutating frame should not cache the
        // result of readable() as the mutating thread calls to writable() can change the result of
        // readable().
        @Suppress("UNCHECKED_CAST")
        (used(framed, id, frame.invalid) as T?)?.apply { frameId = Int.MAX_VALUE }
            ?: readData.create().apply {
                frameId = Int.MAX_VALUE; framed.prependFrameRecord(this as T)
            } as T
    }
    newData.assign(readData)
    newData.frameId = id

    frame.modified?.add(framed)

    return newData
}

/**
 * Returns the current record without notifying any [Frame.readObserver]s.
 */
@PublishedApi
internal fun <T : Record> current(r: T, frame: Frame) = readable(r, frame.id, frame.invalid)

/**
 * Provides a [block] with the current record, without notifying any [Frame.readObserver]s.
 *
 * @see [Record.readable]
 */
inline fun <T : Record> T.withCurrent(block: (r: T) -> Unit) = block(current(this, currentFrame()))
