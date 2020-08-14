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

@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")

package androidx.compose.runtime.frames

@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "SnapshotApplyConflictException",
        "androidx.compose.runtime.snapshots.SnapshotApplyConflictException"
    ),
    DeprecationLevel.ERROR
)
class FrameAborted(val frame: Frame) : RuntimeException("Frame aborted")

/**
 * Base implementation of a frame record
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "StateRecord",
        "androidx.compose.runtime.snapshots.StateRecord"
    )
)
abstract class AbstractRecord : Record {
    override var frameId: Int = deprecated()
    override var next: Record? = null
}

/**
 * Frame local values of a framed object.
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "StateRecord",
        "androidx.compose.runtime.snapshots.StateRecord"
    )
)
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
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "StateObject",
        "androidx.compose.runtime.snapshots.StateObject"
    )
)
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
typealias FrameWriteObserver = (write: Any, isNew: Boolean) -> Unit
typealias FrameCommitObserver = (committed: Set<Any>, frame: Frame) -> Unit

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
    internal val writeObserver: FrameWriteObserver?
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
 * [FrameReadObserver] will be called for every frame read happened on the current
 * thread during execution of the [block].
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "Snapshot.observe(readObserver, null, block)",
        "androidx.compose.runtime.snapshots.Snapshot"
    )
)
fun observeAllReads(readObserver: FrameReadObserver, block: () -> Unit): Unit = deprecated()

/**
 * Return the thread's active frame. This will throw if no frame is active for the thread.
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "Snapshot.current",
        "androidx.compose.runtime.snapshots.Snapshot"
    ),
    DeprecationLevel.ERROR
)
fun currentFrame(): Frame = deprecated()

@Deprecated("Frames has been replaced by snapshots. There is no equivalent with snapshots, it is" +
        " always valid to read and write to a state object.",
    level = DeprecationLevel.ERROR
)
val inFrame: Boolean get(): Boolean = deprecated()

/**
 * Open a frame
 *
 * @param readOnly true if the frame can only be read from
 * @return the newly created frame's data
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "takeMutableSnapshot()",
        "androidx.compose.runtime.snapshots.takeMutableSnapshot"
    ),
    DeprecationLevel.ERROR
)
fun open(readOnly: Boolean = false): Unit = deprecated()

/**
 * Open a frame with observers
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "takeMutableSnapshot(readObserver, writeObserver)",
        "androidx.compose.runtime.snapshots.takeMutableSnapshot"
    ),
    DeprecationLevel.ERROR
)
fun open(readObserver: FrameReadObserver? = null, writeObserver: FrameWriteObserver? = null) {
    deprecated()
}

/*
 * Commits the pending frame if there one is open. Intended to be used in a `finally` clause
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("snapshot.apply()"),
    DeprecationLevel.ERROR
)
fun commitHandler(): Unit = deprecated()

/**
 * Commit the current pending frame. Throws FrameAborted if changes in the frame collides with the
 * current committed frame. Throws IllegalStateException no frame is open (use `commitHandler()` to
 * commit a frame if one is open).
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("snapshot.apply()"),
    DeprecationLevel.ERROR
)
fun commit(): Unit = deprecated()

/**
 * Returns true if the given object framed object mutated in the the frame
 */
@Deprecated("Frames has been replaced by snapshots",
    level = DeprecationLevel.ERROR
)
fun wasModified(value: Any): Boolean = error("deprecated")

@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith(
        "Snapshot.registerApplyObserver",
        "androidx.compose.runtime.snapshots.Snapshot"
    ),
    DeprecationLevel.ERROR
)
fun registerCommitObserver(observer: FrameCommitObserver): () -> Unit = deprecated()

/**
 * Commit the given frame. Throws FrameAborted if changes in the frame collides with the current
 * committed frame.
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("frame.apply().check()"),
    DeprecationLevel.ERROR
)
fun commit(frame: Frame): Unit = error("deprecated")

/**
 * Abort the current frame. Throws FrameAborted if a frame is open. Throws IllegalStateException if
 * no frame is open (use `abortHandler` to abort a frame without throwing an exception or to abort a
 * frame if one is open).
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("snapshot.dispose()"),
    DeprecationLevel.ERROR
)
fun abort(): Unit = deprecated()

/**
 * Abort the given frame and throw a FrameAborted exception.
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("frame.dispose()"),
    DeprecationLevel.ERROR
)
fun abort(frame: Frame): Unit = deprecated()

/**
 * Abort the current frame if one is open. This is intended to be used in a catch handler to abort
 * the frame and then rethrow the exception.
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("snapshot.dispose()"),
    DeprecationLevel.ERROR
)
fun abortHandler(): Unit = deprecated()

/**
 * Abort the given frame.
 */
@Deprecated("Frames has been replaced by snapshots",
    ReplaceWith("frame.dispose()"),
    DeprecationLevel.ERROR
)
fun abortHandler(frame: Frame): Unit = deprecated()

/**
 * Suspend the given frame. After calling suspend() the thread's no longer has an open frame. Call
 * `restore()` to restore a suspended thread.
 */
@Deprecated(
    "Frames has been replaced by snapshots.",
    ReplaceWith(
        "Snapshot.global",
        "androidx.compose.runtime.snapshots.Snapshot"
    ),
    level = DeprecationLevel.ERROR
)
fun suspend(): Frame = error("deprecated")

/**
 * Restore the given frame to the thread.
 */
@Deprecated(
    "Frames has been replaced by snapshots.",
    ReplaceWith(
        "Snapshot.global",
        "androidx.compose.runtime.snapshots.Snapshot"
    ),
    level = DeprecationLevel.ERROR
)
fun restore(frame: Frame): Unit = deprecated()

@Deprecated(
    "Frames has been replaced by snapshots.",
    ReplaceWith("readable"),
    level = DeprecationLevel.ERROR
)
fun <T : Record> T.readable(framed: Framed): T = error("deprecated")

@Deprecated(
    "Frames has been replaced by snapshots.",
    level = DeprecationLevel.ERROR
)
fun _readable(r: Record, framed: Framed): Record = error("deprecated")

@Deprecated("Frames has been replaced by snapshots.",
    level = DeprecationLevel.ERROR
)
fun _writable(r: Record, framed: Framed): Record = error("deprecated")

@Deprecated("Frames has been replaced by snapshots.",
    level = DeprecationLevel.ERROR
)
fun _created(framed: Framed): Unit = error("deprecated")

@Deprecated(
    "Frames has been replaced by snapshots.",
    ReplaceWith("writable"),
    level = DeprecationLevel.ERROR
)
fun <T : Record> T.writable(framed: Framed): T = deprecated()

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
@Deprecated(
    "Frames has been replaced by snapshots.",
    ReplaceWith("writable"),
    level = DeprecationLevel.ERROR
)
fun <T : Record> T.writable(framed: Framed, frame: Frame): T = deprecated()

/**
 * Returns the current record without notifying any [Frame.readObserver]s.
 */
@PublishedApi
internal fun <T : Record> current(r: T, frame: Frame): T = deprecated()

/**
 * Provides a [block] with the current record, without notifying any [Frame.readObserver]s.
 *
 * @see [Record.readable]
 */
@Deprecated(
    "Frames has been replaced by snapshots.",
    ReplaceWith("withCurrent"),
    level = DeprecationLevel.ERROR
)
inline fun <T : Record> T.withCurrent(block: (r: T) -> Unit): T = error("deprecated")

internal fun deprecated(): Nothing = error("deprecated")