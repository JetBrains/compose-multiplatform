package com.google.r4a.frames

import java.util.TreeSet
import java.util.BitSet
import java.util.HashSet

class FrameAborted(val frame: Frame) : RuntimeException("Frame aborted") {}

interface Record {
    var frameId: Int
    var next: Record?
    fun assign(value: Record)
    fun create(): Record
}

abstract class AbstractRecord: Record {
    override var frameId: Int = currentFrame().id
    override var next: Record? = null
}

internal val threadFrame = ThreadLocal<Frame>()

class Frame(val id: Int, internal val invalid: BitSet, readOnly: Boolean) {
    internal val modified = if (readOnly) null else HashSet<Framed>()

    val readonly: Boolean
        get() = modified == null
}

private fun validateNotInFrame() {
    if (threadFrame.get() != null) throw IllegalStateException("In an existing frame")
}

fun currentFrame(): Frame {
    return threadFrame.get() ?: throw IllegalStateException("Not in a frame")
}

// A global synchronization object
private val sync = Object()

// The following variables should only be written when sync is taken
private val openFrames = BitSet()
private val abortedFrames = BitSet()
private var maxFrameId = 0

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

private fun open(readOnly: Boolean, speculative: Boolean): Frame {
    validateNotInFrame()
    synchronized(sync) {
        maxFrameId += 2
        val id = if (speculative) maxFrameId or 1 else maxFrameId
        val invalid = currentInvalid()
        val frame = Frame(id, invalid, readOnly)
        openFrames.set(id)
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
fun open(readOnly: Boolean = false) = open(readOnly, false)

/**
 * Open a speculative frame. A speculative frame can only be aborted and can be used to
 * speculate on how a set of framed objects might react to changes. This allows, for example,
 * expensive calculations to be pre-calculated on a separate thread and later replayed on
 * the primary thread without affecting the primary thread.
 */
fun speculate() = open(false, true)

/*
 * Commits the pending frame if there one is open. Intended to be used in a `finally` clause
 */
fun commitHandler() = threadFrame.get()?.let { commit(it) }

/**
 * Commit the current pending frame. Throws FrameAborted if changes in the frame collides with the current
 * committed frame. Throws IllegalStateException no frame is open (use `commitHandler()` to commit a frame
 * if one is open).
 */
fun commit() = commit(currentFrame())

/**
 * Commit the given frame. Throws FrameAborted if changes in the frame collides with the current
 * committed frame.
 */
fun commit(frame: Frame) {
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
        if (frame.id and 1 != 0) throw IllegalStateException("Speculative frames cannot be committed")
        if (modified == null || modified.size == 0) {
            closeFrame(frame)
        } else {
            // If there are modifications we need to ensure none of the modifications have collisions.

            // A record is guaranteed not collide if no other write was performed to the record by a committed
            // frame since this frame was opened. No writes to a framed object occurred if, ignoring this frame, the
            // readable records for the framed object are the same. If they are different, and the records could be
            // =merged, (such as considering writes to different fields as not colliding) could be allowed here but,
            // for now, the all writes to a record are considered atomic. Additionally, if the field values can be
            // merged this could also be allowed here.

            val current = currentInvalid()
            val nextFrame = maxFrameId + 1
            val start = frame.invalid.copy().apply { set(frame.id) }
            val id = frame.id
            for (framed in frame.modified) {
                val first = framed.first
                if (readable(first, nextFrame, current) != readable(first, id, start)) {
                    abort(frame)
                }
            }
            closeFrame(frame)
        }

    }
}

/**
 * Throw an exception if a frame is not open
 */
private fun validateOpen(frame: Frame) {
    if (!openFrames[frame.id]) throw IllegalStateException("Frame not open")
}

/**
 * Abort the current frame. Throws FrameAborted if a frame is open. Throws IllegalStateException if no frame
 * is open (use `abortHandler` to abort a frame without throwing an exception or to abort a frame if one is open).
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
 * Abort the current frame if one is open. This is intended to be used in a catch handler to abort the frame and then
 * rethrow the exception.
 */
fun abortHandler() {
    threadFrame.get()?.let { abortHandler(it) }
}

/**
 * Abort the given frame.
 */
fun abortHandler(frame: Frame) {
    synchronized(sync) {
        validateOpen(frame)
        if (frame.id and 1 == 0)
            abortedFrames.set(frame.id)
        closeFrame(frame)
    }
}

/**
 * Suspend the given frame. After calling suspend() the thread's no longer has an open frame. Call `restore()` to
 * restore a suspended thread.
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
    synchronized(sync) {
        validateOpen(frame)
        threadFrame.set(frame)
    }
}

private fun closeFrame(frame: Frame) {
    openFrames.clear(frame.id)
    threadFrame.set(null)
}

private fun speculationFrame(candidateFrame: Int, currentFrame: Int) = candidateFrame != currentFrame && (candidateFrame and 1 == 1)

private fun valid(currentFrame: Int, candidateFrame: Int, invalid: BitSet): Boolean {
    // A candidate frame is valid if the it is less than or equal to the current frame
    // and it wasn't specifically marked as invalid when the frame started.
    //
    // All frames open or aborted at the start of the current frame are considered invalid
    // for a frame (they have not been committed and therefore are considered invalid).
    //
    // All frames born after the current frame are considered invalid since they occur after the
    // current frame was open.
    return candidateFrame != 0 && candidateFrame <= currentFrame && !speculationFrame(candidateFrame, currentFrame) && !invalid.get(candidateFrame)
}

// Determine if the given data is valid for the frame.
private fun valid(data: Record, frame: Int, invalid: BitSet): Boolean {
    return valid(frame, data.frameId, invalid)
}

private fun <T: Record> readable(r: T, id: Int, invalid: BitSet): T {
    // The readable record valid record with the highest frameId
    var current: Record? = r
    var candidate: Record? = null
    while(current != null) {
        if (valid(current, id, invalid)) {
            candidate = if (candidate == null) current else if (candidate.frameId < current.frameId) current else candidate
        }
        current = current.next
    }
    if (candidate != null) {
        return candidate as T
    }
    throw IllegalStateException("Could not find a current")
}

fun <T: Record> T.readable(): T {
    return this.readable(currentFrame())
}

fun <T: Record> T.readable(frame: Frame): T {
    return readable(this, frame.id, frame.invalid)
}

fun _readable(r: Record): Record = r.readable()
fun _writable(r: Record, framed: Framed): Record = r.writable(framed)

interface Framed {
    val first: Record
    fun prepend(value: Record)
}

fun <T: Record> T.writable(framed: Framed): T {
    return this.writable(framed, currentFrame())
}

/**
 * A record can be reused if no other frame will see it as valid. This is always true for a record
 * created in an aborted frame. It is also true if the record is valid in the previous frame and is
 * obscured by another record also valid in the previous frame record.
 */
private fun used(framed: Framed, id: Int, invalid: BitSet): Record? {
    var current: Record? = framed.first
    var validRecord: Record? = null
    while (current != null) {
        val currentId = current.frameId
        if (speculationFrame(currentId, id) || abortedFrames[currentId])
            return current
        if (valid(current, id-1, invalid)) {
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

fun <T: Record> T.writable(framed: Framed, frame: Frame): T {
    if (frame.readonly) throw IllegalStateException("In a readonly frame")
    val id = frame.id
    val readData = readable<T>(this, id, frame.invalid)

    // If the readable data was born in this frame, it is writable.
    if (readData.frameId == frame.id) return readData

    // Otherwise, make a copy of the readable data and mark it as born in this frame, making it writable.
    val used = used(framed, id, frame.invalid) as T?
    val newData = used ?: readData.create().apply { frameId = Int.MAX_VALUE; framed.prepend(this as T) } as T
    newData.assign(readData)
    newData.frameId = id

    frame.modified?.add(framed)

    return newData
}
