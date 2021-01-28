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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Dispatcher with the ability to immediately perform (flush) all pending tasks.
 * Without a flush all tasks are dispatched in the dispatcher provided by [scope]
 */
internal class FlushCoroutineDispatcher(
    private val scope: CoroutineScope
) : CoroutineDispatcher() {
    private val tasks = ArrayList<Runnable>()
    private val tasksCopy = ArrayList<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasks) {
            val isFlushScheduled = tasks.isNotEmpty()
            tasks.add(block)
            if (!isFlushScheduled) {
                scope.launch { flush() }
            }
        }
    }

    fun hasTasks() = synchronized(tasks) {
        tasks.isNotEmpty()
    }

    fun flush() {
        synchronized(tasks) {
            tasksCopy.clear()
            tasksCopy.addAll(tasks)
            tasks.clear()
        }

        tasksCopy.forEach(Runnable::run)
    }
}