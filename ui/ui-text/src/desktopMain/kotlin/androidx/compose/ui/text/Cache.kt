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

package androidx.compose.ui.text

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// Extremely simple Cache interface which is enough for ui.text needs
internal interface Cache<K, V> {
    // get a value for [key] or load it by [loader] if doesn't exist
    fun get(key: K, loader: (K) -> V): V
}

// expire cache entries after `expireAfter` after last access
internal class ExpireAfterAccessCache<K, V>(
    val expireAfterNanos: Long,
    val timeProvider: TimeProvider = SystemTimeProvider()
) : Cache<K, V> {
    internal interface TimeProvider {
        fun getTime(): Long
    }

    internal class SystemTimeProvider : TimeProvider {
        override fun getTime() = System.nanoTime()
    }

    inner class Entry(
        val key: K,
        val value: V,
        var accessTime: Long,
        var nextInAccess: Entry? = null,
        var prevInAccess: Entry? = null
    )

    inner class LinkedQueue {
        var head: Entry? = null
        var tail: Entry? = null

        fun moveToHead(entry: Entry) {
            if (head == entry) {
                return
            }

            if (tail == entry) {
                tail = entry.nextInAccess
            }

            entry.nextInAccess?.prevInAccess = entry.prevInAccess
            entry.prevInAccess?.nextInAccess = entry.nextInAccess

            head?.nextInAccess = entry
            entry.prevInAccess = head
            entry.nextInAccess = null
            head = entry
        }

        fun putToHead(entry: Entry) {
            if (tail == null) {
                tail = entry
            }
            head?.nextInAccess = entry
            entry.prevInAccess = head
            head = entry
        }

        fun removeFromTail() {
            if (tail == head) {
                head == null
            }
            tail?.nextInAccess?.prevInAccess = null
            tail = tail?.nextInAccess
        }
    }

    internal val map = HashMap<K, Entry>()
    internal val accessQueue = LinkedQueue()
    private val lock = ReentrantLock()

    override fun get(key: K, loader: (K) -> V): V {
        lock.withLock {
            val now = timeProvider.getTime()
            val v = map[key]
            if (v != null) {
                v.accessTime = now
                accessQueue.moveToHead(v)
                checkEvicted(now)
                return v.value
            } else {
                checkEvicted(now)
                val newVal = loader(key)
                val entry = Entry(
                    key = key,
                    value = newVal,
                    accessTime = now
                )
                map[key] = entry
                accessQueue.putToHead(entry)
                return newVal
            }
        }
    }

    private fun checkEvicted(now: Long) {
        val expireTime = now - expireAfterNanos
        var next = accessQueue.tail
        while (next != null && next.accessTime < expireTime) {
            map.remove(next.key)
            accessQueue.removeFromTail()
            next = next.nextInAccess
        }
    }
}
