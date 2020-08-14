/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.animation.core

import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TransitionDefinitionTest {
    @Test
    fun getTransitionSpecTest() {
        val spec1 = transDef.getSpec(TestState.A, TestState.B)
        val spec2 = transDef.getSpec(TestState.B, TestState.C)
        val spec3 = transDef.getSpec(TestState.B, TestState.A)
        val spec4 = transDef.getSpec(TestState.C, TestState.B)
        val spec5 = transDef.getSpec(TestState.A, TestState.C)
        val spec6 = transDef.getSpec(TestState.C, TestState.A)

        assertSame(spec1, spec2)
        assertSame(spec3, spec4)
        assertSame(spec5, spec6)

        assertNotSame(spec1, spec3)
        assertNotSame(spec3, spec5)
        assertNotSame(spec1, spec5)
    }
}

private enum class TestState { A, B, C }
private val testProp = FloatPropKey()

private val transDef = transitionDefinition<TestState> {
    state(TestState.A) {
        this[testProp] = 0f
    }
    state(TestState.B) {
        this[testProp] = 1f
    }
    state(TestState.C) {
        this[testProp] = 2f
    }

    transition(TestState.A to TestState.B, TestState.B to TestState.C) { }
    transition(TestState.B to TestState.A, TestState.C to TestState.B) { }
    // Intentionally leaving transition for  A to C and C to A undefined
}
