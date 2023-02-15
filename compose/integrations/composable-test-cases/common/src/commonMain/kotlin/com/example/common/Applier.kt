/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.common

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.Clock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit

open class StringsNodeWrapper(
    var text: String? = null,
    val list: MutableList<StringsNodeWrapper> = mutableListOf()
) {
    override fun toString(): String {
        return list.joinToString(prefix = "${text ?: "_"}:{", postfix = "}")
    }

    fun dump() = toString()
}

private class PlainTextNode(text: String? = null) : StringsNodeWrapper(text) {
    override fun toString(): String {
        return text ?: ""
    }
}
private class StringsListApplier(root: StringsNodeWrapper) : AbstractApplier<StringsNodeWrapper>(root) {
    override fun insertBottomUp(index: Int, instance: StringsNodeWrapper) {
        // ignored. Building tree bottom-up
    }

    override fun insertTopDown(index: Int, instance: StringsNodeWrapper) {
        current.list.add(index, instance)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.list.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        current.list.remove(index, count)
    }

    override fun onClear() {
        current.list.clear()
    }
}

private object GlobalSnapshotManager {
    private var started = false
    private var commitPending = false
    private var removeWriteObserver: (ObserverHandle)? = null

    private val scheduleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun ensureStarted() {
        if (!started) {
            started = true
            removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
        }
    }

    private val globalWriteObserver: (Any) -> Unit = {
        // Race, but we don't care too much if we end up with multiple calls scheduled.
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                Snapshot.sendApplyNotifications()
            }
        }
    }

    /**
     * List of deferred callbacks to run serially. Guarded by its own monitor lock.
     */
    private val scheduledCallbacks = mutableListOf<() -> Unit>()

    /**
     * Guarded by [scheduledCallbacks]'s monitor lock.
     */
    private var isSynchronizeScheduled = false

    /**
     * Synchronously executes any outstanding callbacks and brings snapshots into a
     * consistent, updated state.
     */
    private fun synchronize() {
        scheduledCallbacks.forEach { it.invoke() }
        scheduledCallbacks.clear()
        isSynchronizeScheduled = false
    }

    private fun schedule(block: () -> Unit) {
        scheduledCallbacks.add(block)
        if (!isSynchronizeScheduled) {
            isSynchronizeScheduled = true
            scheduleScope.launch { synchronize() }
        }
    }
}

private class MonotonicClockImpl(
    private val afterFrameNotification: Channel<Int>
) : MonotonicFrameClock {

    private val NANOS_PER_MILLI = 1_000_000
    private var frameCounter = 0
    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R = suspendCoroutine { continuation ->
        val now = Clock.System.now()
        val currentNanos = now.toEpochMilliseconds() * NANOS_PER_MILLI + now.nanosecondsOfSecond
        val result = onFrame(currentNanos)
        continuation.resume(result)
        afterFrameNotification.trySend(++frameCounter)
    }
}

fun composeText(
    afterFrameNotification: Channel<Int> = Channel(onBufferOverflow = BufferOverflow.DROP_OLDEST),
    content: @Composable () -> Unit,
): StringsNodeWrapper {
    GlobalSnapshotManager.ensureStarted()

    val context = MonotonicClockImpl(afterFrameNotification)
    val recomposer = Recomposer(context)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val root = StringsNodeWrapper("root")
    val composition = Composition(
        applier = StringsListApplier(root),
        parent = recomposer
    )

    composition.setContent(content)
    return root
}

@Composable
fun TextLeafNode(text: String) {
    ComposeNode<StringsNodeWrapper, StringsListApplier>(
        factory = { PlainTextNode(text) },
        update = {
            set(text) { value -> this.text = value }
        },
    )
}

@Composable
fun TextContainerNode(name: String, content: @Composable () -> Unit = {}) {
    ComposeNode<StringsNodeWrapper, StringsListApplier>(
        factory = { StringsNodeWrapper(name) },
        update = {
            set(name) { value -> this.text = value }
        },
        content = content
    )
}
