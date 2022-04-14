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
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class LiveEditTests {

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

fun liveEditTest(fn: @Composable LiveEditTestScope.() -> Unit) = compositionTest {
    with(LiveEditTestScope()) {
        compose { fn(this) }
        invalidateTargets()
        advance()
        runChecks()
    }
}

@Stable
class LiveEditTestScope {
    private val targetKeys = mutableSetOf<Int>()
    private val checks = mutableListOf<() -> Unit>()
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
    fun addLogCheck(ref: String, validate: (List<String>) -> Unit) {
        checks.add {
            validate(logs.filter { it.first == ref }.map { it.second }.toList())
        }
    }
    fun expectLogCount(ref: String, msg: String, expected: Int) {
        addLogCheck(ref) { logs ->
            val actual = logs.filter { m -> m == msg }.count()
            Assert.assertEquals(
                "Ref '$ref' had an unexpected # of '$msg' logs",
                expected,
                actual
            )
        }
    }
}
