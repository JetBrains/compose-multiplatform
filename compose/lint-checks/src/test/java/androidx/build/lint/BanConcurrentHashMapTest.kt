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

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanConcurrentHashMapTest : AbstractLintDetectorTest(
    useDetector = BanConcurrentHashMap(),
    useIssues = listOf(BanConcurrentHashMap.ISSUE),
) {

    @Test
    fun `Detection of ConcurrentHashMap import in Java sources`() {
        val input = java(
            "src/androidx/ConcurrentHashMapImportJava.java",
            """
                import androidx.annotation.NonNull;
                import java.util.Map;
                import java.util.concurrent.ConcurrentHashMap;

                public class ConcurrentHashMapUsageJava {
                    private final Map<?, ?> mMap = new ConcurrentHashMap<>();

                    public <V, K> Map<V, K> createMap() {
                        return new ConcurrentHashMap<>();
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapImportJava.java:3: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
import java.util.concurrent.ConcurrentHashMap;
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }

    @Test
    fun `Detection of ConcurrentHashMap fully-qualified usage in Java sources`() {
        val input = java(
            "src/androidx/ConcurrentHashMapUsageJava.java",
            """
                import androidx.annotation.NonNull;
                import java.util.Map;

                public class ConcurrentHashMapUsageJava {
                    private final Map<?, ?> mMap = new java.util.concurrent.ConcurrentHashMap<>();

                    public <V, K> Map<V, K> createMap() {
                        return new java.util.concurrent.ConcurrentHashMap<>();
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapUsageJava.java:5: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
    private final Map<?, ?> mMap = new java.util.concurrent.ConcurrentHashMap<>();
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/ConcurrentHashMapUsageJava.java:8: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
        return new java.util.concurrent.ConcurrentHashMap<>();
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(input).expect(expected)
    }

    @Test
    fun `Detection of ConcurrentHashMap import in Kotlin sources`() {
        val input = kotlin(
            "src/androidx/ConcurrentHashMapImportKotlin.kt",
            """
                package androidx

                import java.util.concurrent.ConcurrentHashMap

                class ConcurrentHashMapUsageKotlin {
                    private val mMap: ConcurrentHashMap<*, *> = ConcurrentHashMap<Any, Any>()

                    fun <V, K> createMap(): Map<V, K> {
                        return ConcurrentHashMap()
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapImportKotlin.kt:3: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
import java.util.concurrent.ConcurrentHashMap
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                input
            )
            // This fails in IMPORT_ALIAS mode because changing the import line changes the error.
            // It fails in FULLY_QUALIFIED mode because more errors occur when the fully-qualified
            // class is used. These cases are tested separately.
            .skipTestModes(TestMode.IMPORT_ALIAS, TestMode.FULLY_QUALIFIED)
            .run()
            .expect(expected)
    }

    @Test
    fun `Detection of ConcurrentHashMap fully-qualified usage in Kotlin sources`() {
        val input = kotlin(
            "src/androidx/ConcurrentHashMapUsageKotlin.kt",
            """
                package androidx

                class ConcurrentHashMapUsageKotlin {
                    private val mMap: Map<*, *> = java.util.concurrent.ConcurrentHashMap<Any, Any>()

                    fun <V, K> createMap(): Map<V, K> {
                        return java.util.concurrent.ConcurrentHashMap()
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapUsageKotlin.kt:4: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
    private val mMap: Map<*, *> = java.util.concurrent.ConcurrentHashMap<Any, Any>()
                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/ConcurrentHashMapUsageKotlin.kt:7: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
        return java.util.concurrent.ConcurrentHashMap()
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                input
            )
            .run()
            .expect(expected)
    }

    @Test
    fun `Detection of ConcurrentHashMap import alias in Kotlin sources`() {
        val input = kotlin(
            "src/androidx/ConcurrentHashMapUsageAliasKotlin.kt",
            """
                package androidx

                import java.util.concurrent.ConcurrentHashMap as NewClassName

                class ConcurrentHashMapUsageAliasKotlin {
                    private val mMap: Map<*, *> = NewClassName<Any, Any>()

                    fun <V, K> createMap(): Map<V, K> {
                        return NewClassName()
                    }
                }
            """.trimIndent()
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ConcurrentHashMapUsageAliasKotlin.kt:3: Error: Detected ConcurrentHashMap usage. [BanConcurrentHashMap]
import java.util.concurrent.ConcurrentHashMap as NewClassName
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        lint()
            .files(
                *stubs,
                input
            )
            // In FULLY_QUALIFIED test mode, more errors occur when the fully-qualified class is
            // used. This case is tested separately.
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expect(expected)
    }
}