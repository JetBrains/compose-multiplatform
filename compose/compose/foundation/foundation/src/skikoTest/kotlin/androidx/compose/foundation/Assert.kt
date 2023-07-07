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

package androidx.compose.foundation

import kotlin.math.abs
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class AssertThat<T>(val t: T?, val message: String? = null)

internal class AssertMessage(private val message: String) {
    fun <T> that(t: T) = AssertThat(t, message)
}

internal fun <T> AssertThat<T>.isEqualTo(a: Any?) {
    assertEquals(a, t)
}

internal fun AssertThat<Float>.isEqualTo(f: Float, eps: Float = 0f) {
    if (eps != 0f) {
        assertTrue(message = message ?: "|$t - $f| exceeds $eps") { abs(t!! - f) <= eps }
    } else {
        assertEquals(f, t!!, message = message)
    }
}

internal fun AssertThat<Int>.isNotEqualTo(i: Int) {
    assertNotEquals(i, t)
}

internal fun <T> AssertThat<T>.isNotEqualTo(i: T) {
    assertNotEquals(i, t)
}

internal fun AssertThat<Int>.isEqualTo(i: Int, d: Int = 0) {
    if (d != 0) {
        assertTrue(message = message ?: "|$t - $i| exceeds $d") { abs(t!! - i) <= d }
    } else {
        assertEquals(i, t!!, message = message)
    }
}

internal fun AssertThat<Int>.isWithin1PixelFrom(from: Int) {
    isEqualTo(from, 1)
}

internal fun AssertThat<Boolean>.isTrue() = assertTrue(t == true, message)

internal fun AssertThat<Boolean>.isFalse() = assertTrue(t == false, message)

internal fun <K, T : Iterable<K>> AssertThat<T>.containsExactly(vararg any: K) {
    require(t != null)
    assertContentEquals(t, any.toList(), message)
}

internal fun <K, T : Iterable<K>> AssertThat<T>.containsAtLeast(vararg any: K) {
    require(t != null)
    any.toList().forEach {
        assertContains(t, it, message)
    }
}

internal fun AssertThat<Float>.isGreaterThan(n: Int) {
    assertTrue(t!! > n, message ?: "$t is not greater than $n")
}

internal fun AssertThat<Float>.isAtLeast(n: Float) {
    assertTrue(t!! >= n, message ?: "$t is less than $n")
}

internal fun AssertThat<Float>.isGreaterThan(n: Float) {
    assertTrue(t!! > n, message ?: "$t is not greater than $n")
}
internal fun AssertThat<Float>.isLessThan(n: Float) {
    assertTrue(t!! < n, message ?: "$t is not less than $n")
}
internal fun AssertThat<Int>.isLessThan(n: Int, d: Int = 0) {
    assertTrue(t!! < n, message ?: "$t is not less than $n")
}
internal fun AssertThat<Float>.isAtMost(n: Float) {
    assertTrue(t!! <= n, message ?: "$t is greater than $n")
}

internal fun AssertThat<Int>.isAtMost(n: Int, d: Int = 0) {
    assertTrue(t!! <= n, message ?: "$t is greater than $n")
}

internal fun AssertThat<Float>.isLessThan(n: Int) {
    assertTrue(t!! < n, message ?: "$t is not less than $n")
}

internal fun <T : Collection<*>> AssertThat<T>.isEmpty() {
    assertTrue(t!!.isEmpty(), message ?: "$t is not empty")
}

internal fun <T : Collection<*>> AssertThat<T>.hasSize(size: Int) {
    assertEquals(t!!.size, size, message ?: "$t has ${t.size} items, but $size expected")
}

internal fun <K, T : Collection<*>> AssertThat<T>.contains(vararg items: K) {
    assertTrue(message) {
        t!!.containsAll(items.toList())
    }
}


internal fun AssertThat<*>.isNull() {
    assertEquals(null, t, message ?: "$t expected to be null")
}

internal fun AssertThat<*>.isNotNull() {
    assertNotEquals(null, t, message ?: "$t expected to be not null")
}

internal fun <T> assertThat(t: T?, message: String? = null): AssertThat<T> {
    return AssertThat(t, message)
}

internal fun assertWithMessage(message: String) = AssertMessage(message)

internal fun AssertThat<Int>.isGreaterThan(other: Int, d: Int = 0) {
    assertTrue(message ?: "The was expected to be greater than $other. But value = $t") { t!! > other }
}