/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.ui.createSynchronizedObject
import androidx.compose.ui.synchronized

/**
 * Allows postponing execution of some code (command), adding it to the list via [add],
 * and performing all added commands in some time in the future via [perform]
 */
internal class CommandList(
    private var onNewCommand: () -> Unit
) {
    private val sync = createSynchronizedObject()
    private val list = mutableListOf<() -> Unit>()
    private val listCopy = mutableListOf<() -> Unit>()

    /**
     * true if there are any commands added.
     *
     * Can be called concurrently from multiple threads.
     */
    val hasCommands: Boolean get() = synchronized(sync) {
        list.isNotEmpty()
    }

    /**
     * Add command to the list, and notify observer via [onNewCommand].
     *
     * Can be called concurrently from multiple threads.
     */
    fun add(command: () -> Unit) {
        synchronized(sync) {
            list.add(command)
        }
        onNewCommand()
    }

    /**
     * Clear added commands and perform them.
     *
     * Doesn't support multiple [perform]'s from different threads. But does support concurrent [perform]
     * and concurrent [add].
     */
    fun perform() {
        synchronized(sync) {
            listCopy.addAll(list)
            list.clear()
        }
        listCopy.forEach { it.invoke() }
        listCopy.clear()
    }
}