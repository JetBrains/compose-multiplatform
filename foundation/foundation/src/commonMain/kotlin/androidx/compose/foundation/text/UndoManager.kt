/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.text.input.TextFieldValue

internal val SNAPSHOTS_INTERVAL_MILLIS = 5000

internal expect fun timeNowMillis(): Long

/**
 * It keeps last snapshots of [TextFieldValue]. The total number of kept snapshots is limited but
 * total number of characters in them and should not be more than [maxStoredCharacters]
 * We add a new [TextFieldValue] to the chain in one of three conditions:
 * 1. Keyboard command was executed (something was pasted, word was deleted etc.)
 * 2. Before undo
 * 3. If the last "snapshot" is older than [SNAPSHOTS_INTERVAL_MILLIS]
 *
 * In any case, we are not adding [TextFieldValue] if the content is the same. If text is the same
 * but selection is changed we are not adding a new entry to the chain but update the selection for
 * the last one.
 */
internal class UndoManager(
    val maxStoredCharacters: Int = 100_000
) {
    private class Entry(
        var next: Entry? = null,
        var value: TextFieldValue
    )

    private var undoStack: Entry? = null
    private var redoStack: Entry? = null
    private var storedCharacters: Int = 0
    private var lastSnapshot: Long? = null
    private var forceNextSnapshot = false

    /**
     * It gives an undo manager a chance to save a snapshot if needed because either it's time
     * for periodic snapshotting or snapshot was previously forced via [forceNextSnapshot]. It
     * can be called during every TextField recomposition.
     */
    fun snapshotIfNeeded(value: TextFieldValue, now: Long = timeNowMillis()) {
        if (forceNextSnapshot || now > (lastSnapshot ?: 0) + SNAPSHOTS_INTERVAL_MILLIS) {
            lastSnapshot = now
            makeSnapshot(value)
        }
    }

    /**
     * It forces making a snapshot during the next [snapshotIfNeeded] call
     */
    fun forceNextSnapshot() {
        forceNextSnapshot = true
    }

    /**
     * Unconditionally makes a new snapshot (if a value differs from the last one)
     */
    fun makeSnapshot(value: TextFieldValue) {
        forceNextSnapshot = false
        if (value == undoStack?.value) {
            return
        } else if (value.text == undoStack?.value?.text) {
            // if text is the same, but selection / composition is different we a not making a
            // new record, but update the last one
            undoStack?.value = value
            return
        }
        undoStack = Entry(
            value = value,
            next = undoStack
        )
        redoStack = null
        storedCharacters += value.text.length

        if (storedCharacters > maxStoredCharacters) {
            removeLastUndo()
        }
    }

    private fun removeLastUndo() {
        var entry = undoStack
        if (entry?.next == null) return
        while (entry?.next?.next != null) {
            entry = entry.next
        }
        entry?.next = null
    }

    fun undo(): TextFieldValue? {
        return undoStack?.let { undoEntry ->
            undoEntry.next?.let { nextEntry ->
                undoStack = nextEntry
                storedCharacters -= undoEntry.value.text.length
                redoStack = Entry(
                    value = undoEntry.value,
                    next = redoStack
                )
                nextEntry.value
            }
        }
    }

    fun redo(): TextFieldValue? {
        return redoStack?.let { redoEntry ->
            redoStack = redoEntry.next
            undoStack = Entry(
                value = redoEntry.value,
                next = undoStack
            )
            storedCharacters += redoEntry.value.text.length
            redoEntry.value
        }
    }
}