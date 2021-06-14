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

package androidx.compose.ui

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.remember
import androidx.compose.runtime.withRunningRecomposer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class TestTagModifier<T>(val name: String, val value: T) : Modifier.Element

fun <T> Modifier.testTag(name: String, value: T) = this then TestTagModifier(name, value)

fun <T> Modifier.getTestTag(name: String, default: T): T = foldIn(default) { acc, element ->
    @Suppress("UNCHECKED_CAST")
    if (element is TestTagModifier<*> && element.name == name) element.value as T else acc
}

@Suppress("UnnecessaryComposedModifier")
@OptIn(InternalComposeApi::class)
class ComposedModifierTest {

    /**
     * Confirm that a [composed] modifier correctly constructs separate instances when materialized
     */
    @Test
    fun materializeComposedModifier() = runBlocking(TestFrameClock()) {
        // Note: assumes single-threaded composition
        var counter = 0
        val sourceMod = Modifier.testTag("static", 0)
            .composed { testTag("dynamic", ++counter) }

        withRunningRecomposer { recomposer ->
            lateinit var firstMaterialized: Modifier
            lateinit var secondMaterialized: Modifier
            compose(recomposer) {
                firstMaterialized = currentComposer.materialize(sourceMod)
                secondMaterialized = currentComposer.materialize(sourceMod)
            }

            assertNotEquals("I recomposed some modifiers", 0, counter)

            assertEquals(
                "first static value equal to source",
                sourceMod.getTestTag("static", Int.MIN_VALUE),
                firstMaterialized.getTestTag("static", Int.MAX_VALUE)
            )
            assertEquals(
                "second static value equal to source",
                sourceMod.getTestTag("static", Int.MIN_VALUE),
                secondMaterialized.getTestTag("static", Int.MAX_VALUE)
            )
            assertEquals(
                "dynamic value not present in source",
                Int.MIN_VALUE,
                sourceMod.getTestTag("dynamic", Int.MIN_VALUE)
            )
            assertNotEquals(
                "dynamic value present in first materialized",
                Int.MIN_VALUE,
                firstMaterialized.getTestTag("dynamic", Int.MIN_VALUE)
            )
            assertNotEquals(
                "dynamic value present in second materialized",
                Int.MIN_VALUE,
                firstMaterialized.getTestTag("dynamic", Int.MIN_VALUE)
            )
            assertNotEquals(
                "first and second dynamic values must be unequal",
                firstMaterialized.getTestTag("dynamic", Int.MIN_VALUE),
                secondMaterialized.getTestTag("dynamic", Int.MIN_VALUE)
            )
        }
    }

    /**
     * Confirm that recomposition occurs on invalidation
     */
    @Test
    fun recomposeComposedModifier() = runBlocking {
        // Manually invalidate the composition of the modifier instead of using mutableStateOf
        // Snapshot-based recomposition requires explicit snapshot commits/global write observers.
        var value = 0
        lateinit var scope: RecomposeScope

        val sourceMod = Modifier.composed {
            scope = currentRecomposeScope
            testTag("changing", value)
        }

        val frameClock = TestFrameClock()
        withContext(frameClock) {
            withRunningRecomposer { recomposer ->
                lateinit var materialized: Modifier
                compose(recomposer) {
                    materialized = currentComposer.materialize(sourceMod)
                }

                assertEquals(
                    "initial composition value",
                    0,
                    materialized.getTestTag("changing", Int.MIN_VALUE)
                )

                value = 5
                scope.invalidate()
                frameClock.frame(0L)

                assertEquals(
                    "recomposed composition value",
                    5,
                    materialized.getTestTag("changing", Int.MIN_VALUE)
                )
            }
        }
    }

    @Test
    fun rememberComposedModifier() = runBlocking {
        lateinit var scope: RecomposeScope
        val sourceMod = Modifier.composed {
            scope = currentRecomposeScope
            val state = remember { Any() }
            testTag("remembered", state)
        }

        val frameClock = TestFrameClock()

        withContext(frameClock) {
            withRunningRecomposer { recomposer ->
                val results = mutableListOf<Any?>()
                val notFound = Any()
                compose(recomposer) {
                    results.add(
                        currentComposer.materialize(sourceMod).getTestTag("remembered", notFound)
                    )
                }

                assertTrue("one item added for initial composition", results.size == 1)
                assertNotNull("remembered object not null", results[0])

                scope.invalidate()
                frameClock.frame(0)

                assertEquals("two items added after recomposition", 2, results.size)
                assertTrue("no null items", results.none { it === notFound })
                assertEquals("remembered references are equal", results[0], results[1])
            }
        }
    }

    @Test
    fun nestedComposedModifiers() = runBlocking {
        val mod = Modifier.composed {
            composed {
                testTag("nested", 10)
            }
        }

        val frameClock = TestFrameClock()

        withContext(frameClock) {
            withRunningRecomposer { recomposer ->
                lateinit var materialized: Modifier
                compose(recomposer) {
                    materialized = currentComposer.materialize(mod)
                }

                assertEquals(
                    "fully unwrapped composed modifier value",
                    10,
                    materialized.getTestTag("nested", 0)
                )
            }
        }
    }
}

@OptIn(InternalComposeApi::class)
fun compose(
    recomposer: Recomposer,
    block: @Composable () -> Unit
): Composition {
    return Composition(
        EmptyApplier(),
        recomposer
    ).apply {
        setContent(block)
    }
}

internal class TestFrameClock : MonotonicFrameClock {

    private val frameCh = Channel<Long>()

    suspend fun frame(frameTimeNanos: Long) {
        frameCh.send(frameTimeNanos)
    }

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(frameCh.receive())
}

class EmptyApplier : Applier<Unit> {
    override val current: Unit = Unit
    override fun down(node: Unit) {}
    override fun up() {}
    override fun insertTopDown(index: Int, instance: Unit) {
        error("Unexpected")
    }
    override fun insertBottomUp(index: Int, instance: Unit) {
        error("Unexpected")
    }
    override fun remove(index: Int, count: Int) {
        error("Unexpected")
    }
    override fun move(from: Int, to: Int, count: Int) {
        error("Unexpected")
    }
    override fun clear() {}
}
