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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanSynchronizedMethodsTest : AbstractLintDetectorTest(
    useDetector = BanSynchronizedMethods(),
    useIssues = listOf(BanSynchronizedMethods.ISSUE),
) {

    @Test
    fun `Detection of synchronized methods in Java sources`() {
        val input = java(
            "src/androidx/SynchronizedMethodJava.java",
            """
                public class SynchronizedMethodJava {

                    public synchronized void someMethod() {
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/SynchronizedMethodJava.java:3: Error: Use of synchronized methods is not recommended [BanSynchronizedMethods]
    public synchronized void someMethod() {
    ^
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }

    @Test
    fun `Detection of synchronized methods in Kotlin sources`() {
        val input = kotlin(
            "src/androidx/SynchronizedMethodKotlin.kt",
            """
                class SynchronizedMethodKotlin {

                    @Synchronized
                    fun someMethod() {}
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/SynchronizedMethodKotlin.kt:3: Error: Use of synchronized methods is not recommended [BanSynchronizedMethods]
    @Synchronized
    ^
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }
}
