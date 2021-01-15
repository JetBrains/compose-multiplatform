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

import androidx.compose.ui.unit.Duration
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private class MockedTimeProvider : ExpireAfterAccessCache.TimeProvider {
    var currentTime: Long = 0
    override fun getTime() = currentTime
}

@RunWith(JUnit4::class)
class CacheTest {
    private val time = MockedTimeProvider()
    private val cache = ExpireAfterAccessCache<String, String>(
        expireAfter = Duration(seconds = 1),
        timeProvider = time
    )
    @Test
    fun single_key() {
        cache.get("k1") { "v1" }
        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k1")
        Truth.assertThat(cache.accessQueue.tail!!.key)
            .isEqualTo("k1")

        var valueFromCache = cache.get("k1") { "v1_2" }
        Truth.assertThat(valueFromCache).isEqualTo("v1")

        time.currentTime += Duration(seconds = 2).nanoseconds

        valueFromCache = cache.get("k1") { "v1_3" }
        Truth.assertThat(valueFromCache).isEqualTo("v1")

        Truth.assertThat(cache.accessQueue.head!!.accessTime)
            .isEqualTo(Duration(seconds = 2).nanoseconds)
    }

    @Test
    fun two_keys() {
        cache.get("k1") { "v1" }

        time.currentTime += Duration(milliseconds = 10).nanoseconds

        cache.get("k2") { "v2" }

        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k2")
        Truth.assertThat(cache.accessQueue.tail!!.key)
            .isEqualTo("k1")

        time.currentTime += Duration(milliseconds = 10).nanoseconds

        var valueFromCache = cache.get("k1") { "v1_2" }

        Truth.assertThat(valueFromCache).isEqualTo("v1")

        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k1")
        Truth.assertThat(cache.accessQueue.tail!!.key)
            .isEqualTo("k2")

        // expiration
        time.currentTime += Duration(seconds = 2).nanoseconds
        cache.get("k1") { "v1_3" }

        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k1")
        Truth.assertThat(cache.accessQueue.tail!!.key)
            .isEqualTo("k1")

        Truth.assertThat(cache.map.size)
            .isEqualTo(1)
    }

    @Test
    fun three_keys() {
        cache.get("k1") { "v1" }
        cache.get("k2") { "v2" }
        cache.get("k3") { "v3" }

        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k3")
        Truth.assertThat(cache.accessQueue.tail!!.key)
            .isEqualTo("k1")

        cache.get("k2") { "v2_2" }

        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k2")
        Truth.assertThat(cache.accessQueue.tail!!.key)
            .isEqualTo("k1")
        Truth.assertThat(cache.accessQueue.tail!!.nextInAccess!!.key)
            .isEqualTo("k3")

        time.currentTime += Duration(seconds = 2).nanoseconds
        cache.get("k3") { "v3_3" }

        Truth.assertThat(cache.accessQueue.head!!.key)
            .isEqualTo("k3")
        Truth.assertThat(cache.accessQueue.head!!)
            .isEqualTo(cache.accessQueue.tail!!)
        Truth.assertThat(cache.accessQueue.tail!!.prevInAccess).isNull()
        Truth.assertThat(cache.accessQueue.tail!!.nextInAccess).isNull()

        Truth.assertThat(cache.map.size)
            .isEqualTo(1)
    }
}