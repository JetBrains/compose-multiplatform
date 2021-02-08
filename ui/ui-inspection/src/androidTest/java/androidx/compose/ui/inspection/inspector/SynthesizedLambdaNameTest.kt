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

package androidx.compose.ui.inspection.inspector

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private val topLambda1 = {}
private val topLambda2 = withArgument {}
private val topLambda3 = withArguments({}, {})

private fun withArgument(a: (Int) -> Unit = {}): (Int) -> Unit = a
private fun withArguments(a1: () -> Unit = {}, a2: () -> Unit = {}): List<() -> Unit> =
    listOf(a1, a2)

/**
 * Test the compiler generated lambda names.
 *
 * There is code in Studio that relies on this format.
 * If this test should start to fail, please check the LambdaResolver in the Layout Inspector.
 */
@Suppress("JoinDeclarationAndAssignment")
class SynthesizedLambdaNameTest {
    private val cls = SynthesizedLambdaNameTest::class.java.name
    private val memberLambda1 = {}
    private val memberLambda2 = withArgument {}
    private val memberLambda3 = withArguments({}, {})
    private val initLambda1: (Int) -> Unit
    private val initLambda2: (Int) -> Unit
    private val defaultLambda1 = withArgument()
    private val defaultLambda2 = withArguments()

    init {
        initLambda1 = withArgument {}
        initLambda2 = withArgument {}
    }

    @Test
    fun testSynthesizedNames() {
        assertThat(name(topLambda1)).isEqualTo("${cls}Kt\$topLambda1$1")
        assertThat(name(topLambda2)).isEqualTo("${cls}Kt\$topLambda2$1")
        assertThat(name(topLambda3[0])).isEqualTo("${cls}Kt\$topLambda3$1")
        assertThat(name(topLambda3[1])).isEqualTo("${cls}Kt\$topLambda3$2")
        assertThat(name(memberLambda1)).isEqualTo("$cls\$memberLambda1$1")
        assertThat(name(memberLambda2)).isEqualTo("$cls\$memberLambda2$1")
        assertThat(name(memberLambda3[0])).isEqualTo("$cls\$memberLambda3$1")
        assertThat(name(memberLambda3[1])).isEqualTo("$cls\$memberLambda3$2")
        assertThat(name(initLambda1)).isEqualTo("$cls$1")
        assertThat(name(initLambda2)).isEqualTo("$cls$2")
        assertThat(name(defaultLambda1)).isEqualTo("${cls}Kt\$withArgument$1")
        assertThat(name(defaultLambda2[0])).isEqualTo("${cls}Kt\$withArguments$1")
        assertThat(name(defaultLambda2[1])).isEqualTo("${cls}Kt\$withArguments$2")
    }

    private fun name(lambda: Any) = lambda.javaClass.name
}