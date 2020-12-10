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

import org.junit.Assert.assertTrue
import org.junit.Test

internal class MutableResourceTest {
    private val allResources = ArrayList<TestResource>()

    private inner class TestResource : AutoCloseable {
        var isClosed = false
            private set

        init {
            allResources.add(this)
        }

        fun checkIsAvailable() {
            for (i in 0 until 10) {
                check(!isClosed)
                Thread.sleep(0)
            }
        }

        override fun close() {
            isClosed = true
        }
    }

    @Test(timeout = 5000)
    fun `set and use in parallel threads`() {
        val resource = MutableResource<TestResource>()

        val usingThread = TestThread {
            for (i in 0 until 10000) {
                resource.useWithoutClosing {
                    it?.checkIsAvailable()
                }
            }
        }

        val swappingTread = TestThread {
            while (true) {
                resource.set(TestResource())
                Thread.sleep(0)
                resource.set(TestResource())
                Thread.sleep(0)
                resource.close()
                Thread.sleep(0)
            }
        }

        usingThread.start()
        swappingTread.start()

        usingThread.joinAndThrow()
        swappingTread.interrupt()
        swappingTread.joinAndThrow()

        resource.close()
        assertTrue(allResources.all(TestResource::isClosed))
    }
}