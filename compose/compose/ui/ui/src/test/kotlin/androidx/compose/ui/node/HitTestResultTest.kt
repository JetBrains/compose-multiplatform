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

package androidx.compose.ui.node

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.google.common.truth.Truth.assertThat

@RunWith(JUnit4::class)
class HitTestResultTest {
    @Test
    fun testHit() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hit("Hello", true) {
            hitTestResult.hit("World", true) {
                assertThat(hitTestResult.hasHit()).isFalse()
            }
            assertThat(hitTestResult.hasHit()).isTrue()
        }
        assertThat(hitTestResult.hasHit()).isTrue()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0f, true)).isFalse()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0f, false)).isFalse()

        assertThat(hitTestResult).hasSize(2)
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")

        hitTestResult.hit("Baz", true) {}
        assertThat(hitTestResult.hasHit()).isTrue()
        assertThat(hitTestResult).hasSize(1)
        assertThat(hitTestResult[0]).isEqualTo("Baz")
    }

    @Test
    fun testHitClipped() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hit("Hello", false) {
            hitTestResult.hit("World", false) {
                assertThat(hitTestResult.hasHit()).isFalse()
            }
            assertThat(hitTestResult.hasHit()).isFalse()
        }
        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0f, true)).isTrue()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0f, false)).isFalse()

        assertThat(hitTestResult).hasSize(2)
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")

        hitTestResult.hit("Baz", false) {}
        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult).hasSize(1)
        assertThat(hitTestResult[0]).isEqualTo("Baz")
    }

    @Test
    fun testHitInMinimumTouchTarget() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hitInMinimumTouchTarget("Hello", 1f, true) {
            hitTestResult.hitInMinimumTouchTarget("World", 2f, false) { }
            assertThat(hitTestResult.hasHit()).isFalse()
            assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(1.5f, false)).isTrue()
            assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(2.5f, false)).isFalse()
            assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(2.5f, true)).isTrue()
        }
        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0.5f, true)).isTrue()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(1.5f, true)).isFalse()

        assertThat(hitTestResult).hasSize(2)
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")

        hitTestResult.hitInMinimumTouchTarget("Baz", 0.5f, false) { }
        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult).hasSize(1)
        assertThat(hitTestResult[0]).isEqualTo("Baz")
    }

    @Test
    fun testHasHit() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hitInMinimumTouchTarget("Hello", 1f, true) {
            hitTestResult.hit("World", true) {
                assertThat(hitTestResult.hasHit()).isFalse()
            }
            assertThat(hitTestResult.hasHit()).isTrue()
        }
        assertThat(hitTestResult.hasHit()).isTrue()
    }

    @Test
    fun testEasySpeculativeHit() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.speculativeHit("Hello", 1f, true) {
        }
        assertThat(hitTestResult).hasSize(0)

        hitTestResult.speculativeHit("Hello", 1f, true) {
            hitTestResult.hitInMinimumTouchTarget("World", 2f, true) {}
        }

        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0.5f, true)).isTrue()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(1.5f, true)).isFalse()

        assertThat(hitTestResult).hasSize(2)
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")
    }

    @Test
    fun testSpeculativeHitWithMove() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hitInMinimumTouchTarget("Foo", 1.5f, true) { }

        hitTestResult.speculativeHit("Hello", 1f, true) {
        }

        assertThat(hitTestResult).hasSize(1)
        assertThat(hitTestResult[0]).isEqualTo("Foo")

        hitTestResult.speculativeHit("Hello", 1f, true) {
            hitTestResult.hitInMinimumTouchTarget("World", 2f, true) {}
        }

        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0.5f, true)).isTrue()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(1.25f, true)).isFalse()

        assertThat(hitTestResult).hasSize(2)
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")
    }

    @Test
    fun testSpeculateHitWithDeepHit() {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hitInMinimumTouchTarget("Foo", 1.5f, true) { }

        hitTestResult.speculativeHit("Hello", 2f, true) {
            hitTestResult.hitInMinimumTouchTarget("World", 1f, true) {}
        }

        assertThat(hitTestResult.hasHit()).isFalse()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(0.5f, true)).isTrue()
        assertThat(hitTestResult.isHitInMinimumTouchTargetBetter(1.25f, true)).isFalse()

        assertThat(hitTestResult).hasSize(2)
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")

        hitTestResult.speculativeHit("Goodbye", 2f, true) {
            hitTestResult.hitInMinimumTouchTarget("Cruel", 1f, true) {
                hitTestResult.hit("World!", true) {}
            }
        }

        assertThat(hitTestResult.toList()).isEqualTo(listOf("Goodbye", "Cruel", "World!"))
    }

    @Test
    fun testClear() {
        val hitTestResult = fillHitTestResult()
        assertThat(hitTestResult).hasSize(5)
        hitTestResult.clear()
        assertThat(hitTestResult).hasSize(0)
    }

    @Test
    fun testContains() {
        val hitTestResult = fillHitTestResult()
        assertThat(hitTestResult.contains("Hello")).isTrue()
        assertThat(hitTestResult.contains("World")).isTrue()
        assertThat(hitTestResult.contains("this")).isTrue()
        assertThat(hitTestResult.contains("is")).isTrue()
        assertThat(hitTestResult.contains("great")).isTrue()
        assertThat(hitTestResult.contains("foo")).isFalse()
    }

    @Test
    fun testContainsAll() {
        val hitTestResult = fillHitTestResult()
        assertThat(hitTestResult.containsAll(listOf("Hello", "great", "this"))).isTrue()
        assertThat(hitTestResult.containsAll(listOf("Hello", "great", "foo", "this"))).isFalse()
    }

    @Test
    fun testGet() {
        val hitTestResult = fillHitTestResult()
        assertThat(hitTestResult[0]).isEqualTo("Hello")
        assertThat(hitTestResult[1]).isEqualTo("World")
        assertThat(hitTestResult[2]).isEqualTo("this")
        assertThat(hitTestResult[3]).isEqualTo("is")
        assertThat(hitTestResult[4]).isEqualTo("great")
    }

    @Test
    fun testIndexOf() {
        val hitTestResult = fillHitTestResult("World")
        assertThat(hitTestResult.indexOf("Hello")).isEqualTo(0)
        assertThat(hitTestResult.indexOf("World")).isEqualTo(1)
        assertThat(hitTestResult.indexOf("this")).isEqualTo(2)
        assertThat(hitTestResult.indexOf("is")).isEqualTo(3)
        assertThat(hitTestResult.indexOf("great")).isEqualTo(4)
        assertThat(hitTestResult.indexOf("foo")).isEqualTo(-1)
    }

    @Test
    fun testIsEmpty() {
        val hitTestResult = fillHitTestResult()
        assertThat(hitTestResult.isEmpty()).isFalse()
        hitTestResult.clear()
        assertThat(hitTestResult.isEmpty()).isTrue()
        assertThat(HitTestResult<String>().isEmpty()).isTrue()
    }

    @Test
    fun testIterator() {
        val hitTestResult = fillHitTestResult()
        assertThat(hitTestResult.toList()).isEqualTo(
            listOf("Hello", "World", "this", "is", "great")
        )
    }

    @Test
    fun testLastIndexOf() {
        val hitTestResult = fillHitTestResult("World")
        assertThat(hitTestResult.lastIndexOf("Hello")).isEqualTo(0)
        assertThat(hitTestResult.lastIndexOf("World")).isEqualTo(5)
        assertThat(hitTestResult.lastIndexOf("this")).isEqualTo(2)
        assertThat(hitTestResult.lastIndexOf("is")).isEqualTo(3)
        assertThat(hitTestResult.lastIndexOf("great")).isEqualTo(4)
        assertThat(hitTestResult.lastIndexOf("foo")).isEqualTo(-1)
    }

    @Test
    fun testListIterator() {
        val hitTestResult = fillHitTestResult()
        val iterator = hitTestResult.listIterator()

        val values = listOf("Hello", "World", "this", "is", "great")

        values.forEachIndexed { index, value ->
            assertThat(iterator.nextIndex()).isEqualTo(index)
            if (index > 0) {
                assertThat(iterator.previousIndex()).isEqualTo(index - 1)
            }
            assertThat(iterator.hasNext()).isTrue()
            val hasPrevious = (index != 0)
            assertThat(iterator.hasPrevious()).isEqualTo(hasPrevious)
            assertThat(iterator.next()).isEqualTo(value)
        }

        for (index in values.lastIndex downTo 0) {
            val value = values[index]
            assertThat(iterator.previous()).isEqualTo(value)
        }
    }

    @Test
    fun testListIteratorWithStart() {
        val hitTestResult = fillHitTestResult()
        val iterator = hitTestResult.listIterator(2)

        val values = listOf("Hello", "World", "this", "is", "great")

        for (index in 2..values.lastIndex) {
            assertThat(iterator.nextIndex()).isEqualTo(index)
            if (index > 0) {
                assertThat(iterator.previousIndex()).isEqualTo(index - 1)
            }
            assertThat(iterator.hasNext()).isTrue()
            val hasPrevious = (index != 0)
            assertThat(iterator.hasPrevious()).isEqualTo(hasPrevious)
            assertThat(iterator.next()).isEqualTo(values[index])
        }

        for (index in values.lastIndex downTo 0) {
            val value = values[index]
            assertThat(iterator.previous()).isEqualTo(value)
        }
    }

    @Test
    fun testSubList() {
        val hitTestResult = fillHitTestResult()
        val subList = hitTestResult.subList(2, 4)
        assertThat(subList).hasSize(2)

        assertThat(subList.toList()).isEqualTo(listOf("this", "is"))
        assertThat(subList.contains("this")).isTrue()
        assertThat(subList.contains("foo")).isFalse()
        assertThat(subList.containsAll(listOf("this", "is"))).isTrue()
        assertThat(subList.containsAll(listOf("is", "this"))).isTrue()
        assertThat(subList.containsAll(listOf("foo", "this"))).isFalse()
        assertThat(subList[0]).isEqualTo("this")
        assertThat(subList[1]).isEqualTo("is")
        assertThat(subList.indexOf("is")).isEqualTo(1)
        assertThat(subList.isEmpty()).isFalse()
        assertThat(hitTestResult.subList(4, 4).isEmpty()).isTrue()
        assertThat(subList.subList(0, 2).toList()).isEqualTo(subList.toList())
        assertThat(subList.subList(0, 1)[0]).isEqualTo("this")

        val listIterator1 = subList.listIterator()
        assertThat(listIterator1.hasNext()).isTrue()
        assertThat(listIterator1.hasPrevious()).isFalse()
        assertThat(listIterator1.nextIndex()).isEqualTo(0)
        assertThat(listIterator1.next()).isEqualTo("this")
        assertThat(listIterator1.hasNext()).isTrue()
        assertThat(listIterator1.hasPrevious()).isTrue()
        assertThat(listIterator1.nextIndex()).isEqualTo(1)
        assertThat(listIterator1.next()).isEqualTo("is")
        assertThat(listIterator1.hasNext()).isFalse()
        assertThat(listIterator1.hasPrevious()).isTrue()
        assertThat(listIterator1.previousIndex()).isEqualTo(1)
        assertThat(listIterator1.previous()).isEqualTo("is")

        val listIterator2 = subList.listIterator(1)
        assertThat(listIterator2.hasPrevious()).isTrue()
        assertThat(listIterator2.hasNext()).isTrue()
        assertThat(listIterator2.previousIndex()).isEqualTo(0)
        assertThat(listIterator2.nextIndex()).isEqualTo(1)
        assertThat(listIterator2.previous()).isEqualTo("this")
    }

    @Test
    fun siblingHits() {
        val hitTestResult = HitTestResult<String>()

        hitTestResult.siblingHits {
            hitTestResult.hit("Hello", true) {
                hitTestResult.siblingHits {
                    hitTestResult.hit("World", true) {}
                }
            }
            hitTestResult.acceptHits()
            hitTestResult.hit("this", true) {
                hitTestResult.siblingHits {
                    hitTestResult.hit("is", true) {}
                }
            }
            hitTestResult.acceptHits()
            hitTestResult.hit("great", true) {}
        }
        assertThat(hitTestResult.toList()).isEqualTo(
            listOf(
                "Hello",
                "World",
                "this",
                "is",
                "great"
            )
        )
    }

    private fun fillHitTestResult(last: String? = null): HitTestResult<String> {
        val hitTestResult = HitTestResult<String>()
        hitTestResult.hit("Hello", true) {
            hitTestResult.hit("World", true) {
                hitTestResult.hit("this", true) {
                    hitTestResult.hit("is", true) {
                        hitTestResult.hit("great", true) {
                            last?.let {
                                hitTestResult.hit(it, true) {}
                            }
                        }
                    }
                }
            }
        }
        return hitTestResult
    }
}
