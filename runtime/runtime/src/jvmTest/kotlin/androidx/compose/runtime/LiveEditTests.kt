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

package androidx.compose.runtime

import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class LiveEditTests {

    @Before
    fun setUp() {
        Recomposer.setHotReloadEnabled(true)
    }

    @After
    fun tearDown() {
        clearCompositionErrors()
        Recomposer.setHotReloadEnabled(false)
    }

    @Test
    fun testRestartableFunctionPreservesParentAndSiblingState() = liveEditTest {
        EnsureStatePreservedAndNotRecomposed("a")
        RestartGroup {
            Text("Hello World")
            EnsureStatePreservedAndNotRecomposed("b")
            Target("c")
        }
    }

    // TODO: This should pass but doesn't. Need to investigate why.
    @Ignore
    fun testNonRestartableTargetAtRootScope() = liveEditTest {
        Target("b", restartable = false)
    }

    @Test
    fun testTargetSiblings() = liveEditTest {
        Target("a")
        Target("b")
    }

    @Test
    fun testMultipleFunctionPreservesParentAndSiblingState() = liveEditTest {
        EnsureStatePreservedAndNotRecomposed("a")
        Target("b")
        RestartGroup {
            Text("Hello World")
            EnsureStatePreservedAndNotRecomposed("c")
            Target("d")
            Target("e")
        }
        Target("f")
    }

    @Test
    fun testChildGroupStateIsDestroyed() = liveEditTest {
        EnsureStatePreservedAndNotRecomposed("a")
        RestartGroup {
            Text("Hello World")
            EnsureStatePreservedAndNotRecomposed("b")
            Target("c") {
                Text("Hello World")
                EnsureStateLost("d")
            }
        }
    }

    @Test
    fun testTargetWithinTarget() = liveEditTest {
        EnsureStatePreservedAndNotRecomposed("a")
        RestartGroup {
            Text("Hello World")
            EnsureStatePreservedAndNotRecomposed("b")
            Target("c") {
                Text("Hello World")
                EnsureStateLost("d")
                RestartGroup {
                    MarkAsTarget()
                }
            }
        }
    }

    @Test
    fun testNonRestartableFunctionPreservesParentAndSiblingState() = liveEditTest {
        EnsureStatePreservedButRecomposed("a")
        RestartGroup {
            Text("Hello World")
            EnsureStatePreservedButRecomposed("b")
            Target("c", restartable = false)
        }
    }

    @Test
    fun testMultipleNonRestartableFunctionPreservesParentAndSiblingState() = liveEditTest {
        RestartGroup {
            EnsureStatePreservedButRecomposed("a")
            Target("b", restartable = false)
            RestartGroup {
                Text("Hello World")
                EnsureStatePreservedButRecomposed("c")
                Target("d", restartable = false)
                Target("e", restartable = false)
            }
            Target("f", restartable = false)
        }
    }

    @Test
    fun testLambda() = liveEditTest {
        RestartGroup {
            MarkAsTarget()
            EnsureStateLost("a")
            Text("Hello World")
        }
    }

    @Test
    fun testInlineComposableLambda() = liveEditTest {
        RestartGroup {
            InlineTarget("a")
            EnsureStatePreservedButRecomposed("b")
            Text("Hello World")
        }
    }

    @Test
    fun testThrowing_initialComposition() = liveEditTest {
        RestartGroup {
            MarkAsTarget()
            // Fail once per each reload
            expectError("throwInCompose", 2)
            // Composed once - failed once
            Expect(
                "throw",
                compose = 2,
                onRememberd = 0,
                onForgotten = 0,
                onAbandoned = 2
            )
            error("throwInCompose")
        }
    }

    @Test
    fun testThrowing_recomposition() {
        var recomposeCount = 0
        liveEditTest(reloadCount = 2) {
            RestartGroup {
                MarkAsTarget()

                // only failed on 2nd recomposition
                expectError("throwInCompose", 1)
                // Composed 3 times, failed once
                Expect(
                    "throw",
                    compose = 3,
                    onRememberd = 2,
                    onForgotten = 1,
                    onAbandoned = 1
                )

                recomposeCount++
                if (recomposeCount == 2) {
                    error("throwInCompose")
                }
            }
        }
    }

    @Test
    fun testThrowing_initialComposition_sideEffect() {
        liveEditTest {
            RestartGroup {
                MarkAsTarget()

                // The error is not recoverable, so reload doesn't fix the error
                expectError("throwInEffect", 1)

                // Composition happens as usual
                Expect(
                    "a",
                    compose = 1,
                    onRememberd = 1,
                    onForgotten = 0,
                    onAbandoned = 0,
                )

                SideEffect {
                    error("throwInEffect")
                }
            }
        }
    }

    @Test
    fun testThrowing_recomposition_sideEffect() {
        var recomposeCount = 0
        liveEditTest {
            RestartGroup {
                MarkAsTarget()

                // The error is not recoverable, so reload doesn't fix the error
                expectError("throwInEffect", 1)

                // Composition happens as usual
                Expect(
                    "a",
                    compose = 2,
                    onRememberd = 2,
                    onForgotten = 1,
                    onAbandoned = 0,
                )

                recomposeCount++

                SideEffect {
                    if (recomposeCount == 2) {
                        error("throwInEffect")
                    }
                }
            }
        }
    }

    @Test
    fun testThrowing_initialComposition_remembered() {
        liveEditTest {
            RestartGroup {
                MarkAsTarget()

                // The error is not recoverable, so reload doesn't fix the error
                expectError("throwOnRemember", 1)

                // remembers as usual
                Expect(
                    "a",
                    compose = 1,
                    onRememberd = 1,
                    onForgotten = 0,
                    onAbandoned = 0,
                )

                remember {
                    object : RememberObserver {
                        override fun onRemembered() {
                            error("throwOnRemember")
                        }
                        override fun onForgotten() {}
                        override fun onAbandoned() {}
                    }
                }

                // The rest of remembers fail
                Expect(
                    "b",
                    compose = 1,
                    onRememberd = 0,
                    onForgotten = 0,
                    onAbandoned = 1,
                )
            }
        }
    }

    @Test
    fun testThrowing_recomposition_remembered() {
        var recomposeCount = 0
        liveEditTest {
            RestartGroup {
                MarkAsTarget()

                // The error is not recoverable, so reload doesn't fix the error
                expectError("throwOnRemember", 1)

                recomposeCount++

                // remembers as usual
                Expect(
                    "a",
                    compose = 2,
                    onRememberd = 2,
                    onForgotten = 1,
                    onAbandoned = 0,
                )

                remember {
                    object : RememberObserver {
                        override fun onRemembered() {
                            if (recomposeCount == 2) {
                                error("throwOnRemember")
                            }
                        }
                        override fun onForgotten() {}
                        override fun onAbandoned() {}
                    }
                }

                // The rest of remembers fail
                Expect(
                    "b",
                    compose = 2,
                    onRememberd = 1,
                    // todo: ensure forgotten is not dispatched for abandons?
                    onForgotten = 1,
                    onAbandoned = 1,
                )
            }
        }
    }

    @Test
    fun testThrowing_invalidationsCarriedAfterCrash() {
        var recomposeCount = 0
        val state = mutableStateOf(0)
        liveEditTest(reloadCount = 2) {
            RestartGroup {
                RestartGroup {
                    MarkAsTarget()

                    // Only error the first time
                    expectError("throwInComposition", 1)

                    if (recomposeCount == 0) {
                        // invalidate sibling group below in first composition
                        state.value += 1
                    }

                    if (recomposeCount++ == 1) {
                        // crash after first reload
                        error("throwInComposition")
                    }
                }
            }

            RestartGroup {
                // read state
                state.value

                // composed initially + invalidated by crashed composition
                Expect(
                    "state",
                    compose = 2,
                    onRememberd = 1,
                    onForgotten = 0,
                    onAbandoned = 0
                )
            }
        }
    }

    @Test
    fun testThrowing_movableContent() {
        liveEditTest {
            RestartGroup {
                MarkAsTarget()

                expectError("throwInMovableContent", 2)

                val content = remember {
                    movableContentOf {
                        error("throwInMovableContent")
                    }
                }

                content()
            }
        }
    }

    @Test
    fun testThrowing_movableContent_recomposition() {
        var recomposeCount = 0
        liveEditTest(reloadCount = 2) {
            RestartGroup {
                MarkAsTarget()

                expectError("throwInMovableContent", 1)

                val content = remember {
                    movableContentOf {
                        Expect(
                            "movable",
                            compose = 3,
                            onRememberd = 2,
                            onForgotten = 1,
                            onAbandoned = 1
                        )

                        if (recomposeCount == 1) {
                            error("throwInMovableContent")
                        }
                    }
                }

                content()

                recomposeCount++
            }
        }
    }

    @Test
    fun testThrowing_movableContent_throwAfterMove() {
        var recomposeCount = 0
        liveEditTest(reloadCount = 2) {
            expectError("throwInMovableContent", 1)

            val content = remember {
                movableContentOf {
                    recomposeCount++
                    Expect(
                        "movable",
                        compose = 4,
                        onRememberd = 3,
                        onForgotten = 2,
                        onAbandoned = 1
                    )

                    if (recomposeCount == 1) {
                        error("throwInMovableContent")
                    }
                }
            }

            RestartGroup {
                MarkAsTarget()

                if (recomposeCount == 0) {
                    content()
                }
            }

            RestartGroup {
                MarkAsTarget()

                if (recomposeCount > 0) {
                    content()
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun LiveEditTestScope.EnsureStatePreservedButRecomposed(ref: String) {
    Expect(
        ref,
        compose = 2,
        onRememberd = 1,
        onForgotten = 0,
        onAbandoned = 0,
    )
}

@Composable
@NonRestartableComposable
fun LiveEditTestScope.EnsureStatePreservedAndNotRecomposed(ref: String) {
    Expect(
        ref,
        compose = 1,
        onRememberd = 1,
        onForgotten = 0,
        onAbandoned = 0,
    )
}

@Composable
@NonRestartableComposable
fun LiveEditTestScope.EnsureStateLost(ref: String) {
    Expect(
        ref,
        compose = 2,
        onRememberd = 2,
        onForgotten = 1,
        onAbandoned = 0,
    )
}

@Composable
@NonRestartableComposable
fun LiveEditTestScope.Expect(
    ref: String,
    compose: Int,
    onRememberd: Int,
    onForgotten: Int,
    onAbandoned: Int,
) {
    log(ref, "compose")
    remember {
        object : RememberObserver {
            override fun onRemembered() {
                log(ref, "onRemembered")
            }

            override fun onForgotten() {
                log(ref, "onForgotten")
            }

            override fun onAbandoned() {
                log(ref, "onAbandoned")
            }
        }
    }
    expectLogCount(ref, "compose", compose)
    expectLogCount(ref, "onRemembered", onRememberd)
    expectLogCount(ref, "onForgotten", onForgotten)
    expectLogCount(ref, "onAbandoned", onAbandoned)
}

@Composable fun LiveEditTestScope.Target(
    ref: String,
    restartable: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    if (restartable) currentRecomposeScope
    MarkAsTarget()
    Expect(
        ref,
        compose = 2,
        onRememberd = 2,
        onForgotten = 1,
        onAbandoned = 0,
    )
    content()
}

@Composable fun LiveEditTestScope.InlineTarget(
    ref: String,
    content: @Composable () -> Unit = {}
) {
    MarkAsTarget()
    Expect(
        ref,
        compose = 2,
        onRememberd = 2,
        onForgotten = 1,
        onAbandoned = 0,
    )
    content()
}

@Composable
@ExplicitGroupsComposable
fun LiveEditTestScope.MarkAsTarget() {
    addTargetKey((currentComposer as ComposerImpl).parentKey())
}

@OptIn(InternalComposeApi::class)
fun liveEditTest(
    reloadCount: Int = 1,
    fn: @Composable LiveEditTestScope.() -> Unit,
) = compositionTest {
    with(LiveEditTestScope()) {
        addCheck {
            (composition as? ControlledComposition)?.verifyConsistent()
        }

        recordErrors {
            compose { fn(this) }
        }

        repeat(reloadCount) {
            invalidateTargets()
            recordErrors {
                advance()
            }
        }

        runChecks()
    }
}

@OptIn(InternalComposeApi::class)
private inline fun LiveEditTestScope.recordErrors(
    block: () -> Unit
) {
    try {
        block()
    } catch (e: ComposeRuntimeError) {
        throw e
    } catch (e: Exception) {
        addError(e)
    }
    currentCompositionErrors().forEach {
        addError(it.first)
    }
}

@Stable
class LiveEditTestScope {
    private val targetKeys = mutableSetOf<Int>()
    private val checks = mutableListOf<() -> Unit>()
    private val errors = mutableSetOf<Exception>()
    private val logs = mutableListOf<Pair<String, String>>()

    fun invalidateTargets() {
        for (key in targetKeys) {
            invalidateGroupsWithKey(key)
        }
    }

    fun runChecks() {
        for (check in checks) {
            check()
        }
    }

    fun addTargetKey(key: Int) {
        targetKeys.add(key)
    }

    fun log(ref: String, msg: String) {
        logs.add(ref to msg)
    }

    fun addError(e: Exception) {
        errors.add(e)
    }

    fun addCheck(check: () -> Unit) {
        checks.add(check)
    }

    fun expectLogCount(ref: String, msg: String, expected: Int) {
        addCheck {
            val logs = logs.filter { it.first == ref }.map { it.second }.toList()
            val actual = logs.filter { m -> m == msg }.count()
            Assert.assertEquals(
                "Ref '$ref' had an unexpected # of '$msg' logs",
                expected,
                actual
            )
        }
    }

    fun expectError(message: String, count: Int) {
        addCheck {
            val errors = errors.filter { it.message == message }
            Assert.assertEquals(
                "Got ${errors.size} errors with $message",
                count,
                errors.size
            )
        }
    }
}
