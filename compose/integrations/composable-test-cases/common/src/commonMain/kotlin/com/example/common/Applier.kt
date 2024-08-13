/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.common

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private var removeWriteObserver: (ObserverHandle)? = null

    fun ensureStarted() {
        if (removeWriteObserver != null) {
            removeWriteObserver!!.dispose()
        }
        removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
    }

    private val globalWriteObserver: (Any) -> Unit = {
        // Race, but we don't care too much if we end up with multiple calls scheduled.
        Snapshot.sendApplyNotifications()
    }
}

/**
 * A clock that runs onFrame immediately when withFrameNanos is requested.
 * Since it doesn't sync with any actual/real frame dispatching,
 * this Clock impl can be used only for tests.
 */
private class MonotonicClockImpl : MonotonicFrameClock {

    private val NANOS_PER_MILLI = 1_000_000

    private var i = 0L

    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R = suspendCoroutine { continuation ->
        val now = ++i
        val currentNanos = now * NANOS_PER_MILLI
        val result = onFrame(currentNanos)
        continuation.resume(result)
    }
}

fun composeText(
    recomposerCoroutineContext: CoroutineContext? = null,
    content: @Composable () -> Unit
): StringsNodeWrapper {
    GlobalSnapshotManager.ensureStarted()

    val clock = MonotonicClockImpl()
    val context = if (recomposerCoroutineContext != null) recomposerCoroutineContext + clock else clock
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
