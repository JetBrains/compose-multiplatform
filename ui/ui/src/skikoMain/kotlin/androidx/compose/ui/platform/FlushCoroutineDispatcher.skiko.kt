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

package androidx.compose.ui.platform

import androidx.compose.ui.synchronized
import androidx.compose.ui.createSynchronizedObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Volatile

/**
 * Dispatcher with the ability to immediately perform (flush) all pending tasks.
 * Without a flush all tasks are dispatched in the dispatcher provided by [scope]
 */
internal class FlushCoroutineDispatcher(
    scope: CoroutineScope
) : CoroutineDispatcher() {
    // Dispatcher should always be alive, even if Job is cancelled. Otherwise coroutines which
    // use this dispatcher won't be properly cancelled.
    // TODO replace it by scope.coroutineContext[Dispatcher] when it will be no longer experimental
    private val scope = CoroutineScope(scope.coroutineContext.minusKey(Job))
    private val tasks = mutableSetOf<Runnable>()
    private val tasksLock = createSynchronizedObject()
    private val tasksCopy = mutableSetOf<Runnable>()
    @Volatile
    private var isPerformingRun = false
    private val runLock = createSynchronizedObject()
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasksLock) {
            tasks.add(block)
        }
        scope.launch {
            performRun {
                val isTaskAlive = synchronized(tasksLock) {
                    tasks.remove(block)
                }
                if (isTaskAlive) {
                    block.run()
                }
            }
        }
    }
    /**
     * Does the dispatcher have any tasks scheduled or currently in progress
     */
    fun hasTasks() = synchronized(tasksLock) {
        tasks.isNotEmpty()
    } && !isPerformingRun

    /**
     * Perform all scheduled tasks and wait for the tasks which are already
     * performing in the [scope]
     */
    fun flush() = performRun {
        synchronized(tasksLock) {
            tasksCopy.addAll(tasks)
            tasks.clear()
        }
        tasksCopy.forEach(Runnable::run)
        tasksCopy.clear()
    }

    // the lock is needed to be certain that all tasks will be completed after `flush` method
    private fun performRun(body: () -> Unit) = synchronized(runLock) {
        try {
            isPerformingRun = true
            body()
        } finally {
            isPerformingRun = false
        }
    }
}
