/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.desktop

/**
 * Resource that can be used multiple times in parallel threads.
 */
internal class MutableResource<T : AutoCloseable> : AutoCloseable {
    @Volatile
    private var resource: T? = null
    @Volatile
    private var resourceToClose: T? = null
    @Volatile
    private var usingResource: T? = null

    /**
     * Close internal resource.
     *
     * If we close resource when it is using in [useWithoutClosing], we defer it's closing.
     *
     * If resource isn't using we close it immediately.
     *
     * After close we can use [set] (for set new internal resource)
     * and [useWithoutClosing] (it will be called with null resource)
     */
    override fun close() = set(null)

    /**
     * Change internal resource and close previous.
     *
     * If we set resource when previous is using in [useWithoutClosing], we defer it's closing.
     *
     * If previous isn't using we close it immediately.
     */
    fun set(resource: T?): Unit = synchronized(this) {
        val oldResource = this.resource
        this.resource = resource
        if (oldResource === usingResource) {
            resourceToClose = oldResource
        } else {
            oldResource?.close()
        }
    }

    /**
     * Can be called from the other thread.
     *
     * If we [set] new resource when we using previous, we close previous after using.
     */
    fun useWithoutClosing(use: (T?) -> Unit) {
        synchronized(this) {
            usingResource = resource
        }
        try {
            use(usingResource)
        } finally {
            synchronized(this) {
                usingResource = null
                resourceToClose?.close()
                resourceToClose = null
            }
        }
    }
}