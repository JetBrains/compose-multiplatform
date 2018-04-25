package com.google.r4a.frames

import java.util.TreeSet
import java.util.BitSet

class FrameAborted(val frame: FrameData) : RuntimeException("Frame aborted") {}

interface Record<T> where T : Record<T> {
    var minFrame: Int
    var maxFrame: Int
    var next: T?
    fun assign(value: T)
    fun create(): T
}

abstract class AbstractRecord<T>: Record<T>  where T: Record<T> {
    override var minFrame: Int = 0
    override var maxFrame: Int = Int.MAX_VALUE
    override var next: T? = null
}

internal val threadFrame = ThreadLocal<FrameData>()

class FrameData(val id: Int, internal val invalid: BitSet, readOnly: Boolean, internal val implicit: Boolean) {
    internal val modified = if (readOnly) null else HashSet<Holder<*>>()

    val readonly: Boolean
        get() = modified == null
}

private fun validateInFrame(data: FrameData?) {
    if (data == null) throw IllegalStateException("Not in a frame")
}

private fun validateNotInFrame() {
    if (threadFrame.get() != null) throw IllegalStateException("In an existing frame")
}

fun currentFrameData(): FrameData {
    val data = threadFrame.get()
    validateInFrame(data)
    return data
}

val frameId: Int get() = currentFrameData().id

// A global synchronization object
private val sync = Object()

// The following variables should only be written when sync is taken
private val openFrames = BitSet()
private val openFrameIds = TreeSet<Int>()
private val abortedFrames = BitSet()
private var maxFrameId = 0
private var minOpenId = 0

// TODO(chuckj): PERF - the sets of open frames grows indefinitly. Consider periodically shrinking the set

/**
 * Return the frames that are currently open or aborted which should be considered invalid for any new frames
 */
private fun currentInvalid(): BitSet {
    return BitSet().apply {
        or(openFrames)
        or(abortedFrames)
    }
}

private fun BitSet.copy(): BitSet {
    return BitSet().apply { or(this@copy) }
}

/**
 * Open a frame
 *
 * @param readOnly true if the frame can only be read from
 * @param implicit true if committing the frame implicitly causes another one to be created
 * @return the newly created frame's data
 */
fun open(readOnly: Boolean, implicit: Boolean): FrameData {
    validateNotInFrame()
    synchronized(sync) {
        val id = ++maxFrameId
        val invalid = currentInvalid()
        val frame = FrameData(id, invalid, readOnly, implicit)
        openFrames.set(id)
        openFrameIds.add(id)
        threadFrame.set(frame)
        return frame
    }
}

/**
 * Commits the pending frame if there one is open. Intended to be used in a `finally` clause
 */
fun commitHandler() = threadFrame.get()?.let { commit(it) }

/**
 * Commit the current pending frame. Throws FrameAborted if changes in the frame collides with the current
 * committed frame. Throws IllegalStateException no frame is open (use `commitHandler()` to commit a frame
 * if one is open).
 */
fun commit() = commit(currentFrameData())

/**
 * Commit the given frame. Throws FrameAborted if changes in the frame collides with the current
 * committed frame.
 */
fun commit(frame: FrameData) {
    // NOTE: the this algorithm is currently does not guarantee a serializable frame operation as it doesn't prevent
    // crossing writes as described here https://arxiv.org/pdf/1412.2324.pdf

    // Just removing the frame from the open frame set is enough to make it visible, however, this should
    // only be done after first determining that there are no colliding writes in the commit.

    // A write is considered colliding if any write occurred on the object in a frame committed since the
    // frame was last opened. There are two trivial cases that can be dismissed immediately, first, if the frame
    // is read-only, no writes occurred. Second, if no other frame was opened while the current frame was open.
    val modified = frame.modified
    synchronized(sync) {
        if (!openFrames[frame.id]) throw IllegalStateException("Frame not open")
        if (modified == null || modified.size == 0) {
            closeFrame(frame)
        } else {
            // If there are modifications we need to ensure none of the modifications have collisions.

            // A record is guaranteed not collide if no other write was performed to the record by a committed
            // frame since this frame was opened. No writes to a holder occurred if, ignoring this frame, the readable
            // records for the holder is the same. If they are different, and the records could be merged, (such
            // as considering writes to different fields as not colliding) could be allowed here but, for now, the
            // all writes to a record are considered atomic. Additionally, if the field values can be merged this
            // could also be allowed here.

            val current = currentInvalid()
            val nextFrame = maxFrameId + 1
            val start = frame.invalid.copy().apply { set(frame.id) }
            val id = frame.id
            for (holder in frame.modified) {
                if (!holder.canCommit(nextFrame, current, id, start)) {
                    abort(frame)
                }
            }
            closeFrame(frame)
        }

    }

    if (frame.implicit) {
        open(frame.readonly, frame.implicit)
    }
}

/**
 * Throw an exception if a frame is not open
 */
private fun validateOpen(frame: FrameData) {
    if (!openFrames[frame.id]) throw IllegalStateException("Frame not open")
}

/**
 * Abort the current frame. Throws FrameAborted if a frame is open. Throws IllegalStateException if no frame
 * is open (use `abortHandler` to abort a frame without throwing an exception or to abort a frame if one is open).
 */
fun abort() {
    abort(currentFrameData())
}

/**
 * Abort the given frame and throw a FrameAborted exception.
 */
fun abort(frame: FrameData) {
    abortHandler(frame)
    throw FrameAborted(frame)
}

/**
 * Abort the current frame if one is open. This is intended to be used in a catch handler to abort the frame and then
 * rethrow the exception.
 */
fun abortHandler() {
    threadFrame.get()?.let { abortHandler(it) }
}

/**
 * Abort the given frame.
 */
fun abortHandler(frame: FrameData) {
    synchronized(sync) {
        validateOpen(frame)
        abortedFrames.set(frame.id)
        closeFrame(frame)
    }
    if (frame.implicit) {
        open(frame.readonly, frame.implicit)
    }
}

/**
 * Suspend the given frame. After calling suspend() the thread's no longer has an open frame. Call `restore()` to
 * restore a suspended thread.
 */
fun suspend(): FrameData {
    val frame = currentFrameData()
    threadFrame.set(null)
    return frame
}

/**
 * Restore the given frame to the thread.
 */
fun restore(frame: FrameData) {
    validateNotInFrame()
    synchronized(sync) {
        validateOpen(frame)
        threadFrame.set(frame)
    }
}

private fun closeFrame(frame: FrameData) {
    openFrames.clear(frame.id)
    openFrameIds.remove(frame.id)
    minOpenId = if (openFrameIds.size > 0) openFrameIds.first() else maxFrameId + 1
    threadFrame.set(null)
}

private fun valid(currentFrame: Int, candidateFrame: Int, invalid: BitSet): Boolean {
    // A candidate frame is valid if the it is less than or equal to the current frame
    // and it wasn't specifically marked as invalid when the frame started.
    //
    // All frames open or aborted at the start of the current frame are considered invalid
    // for a frame (they have not been committed and therefore are considered invalid).
    //
    // All frames born after the current frame are considered invalid since they occur after the
    // current frame was open.
    return candidateFrame != 0 && candidateFrame <= currentFrame && !invalid.get(candidateFrame)
}

// Determine if the given data is valid for the frame.
private fun <T : Record<T>> valid(data: T, frame: Int, invalid: BitSet): Boolean {
    // A frame is valid if the birth frame (i.e. min frame) is valid and the death frame (i.e. the max frame)
    // is invalid.
    return valid(frame, data.minFrame, invalid) && !valid(frame, data.maxFrame, invalid)
}

private fun <T: Record<T>> readable(r: T, id: Int, invalid: BitSet): T {
    // The readable data is the first valid data with respect to the current frame.
    // This assumes the frames are in order.
    var current: T? = r
    var candidate: T? = null
    while(current != null) {
        if (valid(current, id, invalid)) {
            candidate = if (candidate == null) current else if (candidate.minFrame < current.minFrame) current else candidate
        }
        current = current.next
    }
    if (candidate != null) {
        return candidate
    }
    throw IllegalStateException("Could not find a current")
}

fun <T> T.readable(): T where T: Record<T> {
    return this.readable(currentFrameData())
}

fun <T> T.readable(frame: FrameData): T where T: Record<T> {
    return readable(this, frame.id, frame.invalid)
}

interface Holder<T> where T: Record<T> {
    val first: T
    fun prepend(r: T)

    fun canCommit(current: Int, invalid: BitSet, start: Int, startSet: BitSet): Boolean {
        return readable(first, current, invalid) == readable(first, start, startSet)
    }
}

fun <T> T.writable(holder: Holder<T>): T where T: Record<T> {
    return this.writable(holder, currentFrameData())
}

/**
 * A record can be reused no other frame will see it as valid. This is true if the minimum open frame is greater than
 * the frame that obscured this record(and that frame wasn't aborted) or the frame it was born in was aborted. A
 * maxFrame of 0 is indicates that the record is in the process of being created (possibly on another thread) and
 * should not be reused.
 */
private fun <T: Record<T>> used(holder: Holder<T>): T? {
    var current: T? = holder.first
    val min = minOpenId
    while (current != null) {
        if (current.maxFrame != 0 && ((current.maxFrame < min && !abortedFrames[current.maxFrame]) || abortedFrames[current.minFrame]))
            return current
        current = current.next
    }
    return null
}


fun <T> T.writable(holder: Holder<T>, frame: FrameData): T where T: Record<T> {
    if (frame.readonly) throw IllegalStateException("In a readonly frame")
    val id = frame.id
    val readData = readable<T>(this, id, frame.invalid)

    // If the readable data was born in this frame, it is writable.
    if (readData.minFrame == frame.id) return readData

    // Otherwise, make a copy of the readable data and mark it as born in this frame, making it writable.
    val used = used(holder)
    val newData = used ?: readData.create().apply { maxFrame = 0; minFrame = 0; holder.prepend(this) }
    newData.assign(readData)
    newData.minFrame = id
    newData.maxFrame = Int.MAX_VALUE

    // Mark the readData as obscured by this write
    readData.maxFrame = id

    frame.modified?.add(holder)

    return newData
}
