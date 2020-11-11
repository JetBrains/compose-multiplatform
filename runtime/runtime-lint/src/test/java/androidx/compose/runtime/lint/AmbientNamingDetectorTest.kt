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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [AmbientNamingDetector].
 */
class AmbientNamingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = AmbientNamingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AmbientNamingDetector.AmbientNaming)

    // Simplified Ambient.kt stubs
    private val ambientStub = kotlin(
        """
            package androidx.compose.runtime

            sealed class Ambient<T> constructor(defaultFactory: (() -> T)? = null)

            abstract class ProvidableAmbient<T> internal constructor(
                defaultFactory: (() -> T)?
            ) : Ambient<T>(defaultFactory)

            internal class DynamicProvidableAmbient<T> constructor(
                defaultFactory: (() -> T)?
            ) : ProvidableAmbient<T>(defaultFactory)

            internal class StaticProvidableAmbient<T>(
                defaultFactory: (() -> T)?
            ) : ProvidableAmbient<T>(defaultFactory)

            fun <T> ambientOf(
                defaultFactory: (() -> T)? = null
            ): ProvidableAmbient<T> = DynamicProvidableAmbient(defaultFactory)

            fun <T> staticAmbientOf(
                defaultFactory: (() -> T)? = null
            ): ProvidableAmbient<T> = StaticProvidableAmbient(defaultFactory)
        """
    )

    @Test
    fun ambientUsedAsNoun() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val FooAmbient = ambientOf { 5 }

                object Test {
                    val BarAmbient: Ambient<String?> = staticAmbientOf { null }
                }

                class Test2 {
                    companion object {
                        val BazAmbient: ProvidableAmbient<Int> = ambientOf()
                    }
                }
            """
            ),
            ambientStub
        )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/Test.kt:6: Error: Ambient should not be used as a noun when naming Ambient properties [AmbientNaming]
                val FooAmbient = ambientOf { 5 }
                    ~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:9: Error: Ambient should not be used as a noun when naming Ambient properties [AmbientNaming]
                    val BarAmbient: Ambient<String?> = staticAmbientOf { null }
                        ~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:14: Error: Ambient should not be used as a noun when naming Ambient properties [AmbientNaming]
                        val BazAmbient: ProvidableAmbient<Int> = ambientOf()
                            ~~~~~~~~~~
3 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/runtime/foo/Test.kt line 6: Use Ambient as an adjective (prefix):
@@ -6 +6
-                 val FooAmbient = ambientOf { 5 }
+                 val AmbientFoo = ambientOf { 5 }
Fix for src/androidx/compose/runtime/foo/Test.kt line 9: Use Ambient as an adjective (prefix):
@@ -9 +9
-                     val BarAmbient: Ambient<String?> = staticAmbientOf { null }
+                     val AmbientBar: Ambient<String?> = staticAmbientOf { null }
Fix for src/androidx/compose/runtime/foo/Test.kt line 14: Use Ambient as an adjective (prefix):
@@ -14 +14
-                         val BazAmbient: ProvidableAmbient<Int> = ambientOf()
+                         val AmbientBaz: ProvidableAmbient<Int> = ambientOf()
                """
            )
    }

    @Test
    fun ambientUsedAsAdjective() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val AmbientFoo = ambientOf { 5 }

                object Test {
                    val AmbientBar: Ambient<String?> = staticAmbientOf { null }
                }

                class Test2 {
                    companion object {
                        val AmbientBaz: ProvidableAmbient<Int> = ambientOf()
                    }
                }
            """
            ),
            ambientStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun descriptiveAdjectives() {
        lint().files(
            kotlin(
                """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val ThemeFoo = ambientOf { 5 }

                object Test {
                    val ThemeBar: Ambient<String?> = staticAmbientOf { null }
                }

                class Test2 {
                    companion object {
                        val StyledBaz: ProvidableAmbient<Int> = ambientOf()
                    }
                }
            """
            ),
            ambientStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
