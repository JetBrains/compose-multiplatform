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

package androidx.compose.ui.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ModifierDeclarationDetector].
 */
class ModifierDeclarationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierDeclarationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ModifierDeclarationDetector.ModifierFactoryReturnType)

    private val modifierStub = kotlin(
        """
            package androidx.compose.ui

            interface Modifier {
                interface Element : Modifier
            }

            object TestModifier : Modifier
        """
    )

    @Test
    fun functionReturnsModifierElement() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): Modifier.Element {
                    return TestModifier
                }
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier(): Modifier.Element {
                             ~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change return type to Modifier:
@@ -8 +8
-                 fun Modifier.fooModifier(): Modifier.Element {
+                 fun Modifier.fooModifier(): Modifier {
            """
            )
    }

    @Test
    fun getterReturnsModifierElement() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                val Modifier.fooModifier get(): Modifier.Element {
                    return TestModifier
                }

                val Modifier.fooModifier2: Modifier.Element get() {
                    return TestModifier
                }

                val Modifier.fooModifier3: Modifier.Element get() = TestModifier
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier get(): Modifier.Element {
                                         ~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier2: Modifier.Element get() {
                                                            ~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier3: Modifier.Element get() = TestModifier
                                                            ~~~
3 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change return type to Modifier:
@@ -8 +8
-                 val Modifier.fooModifier get(): Modifier.Element {
+                 val Modifier.fooModifier get(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 12: Change return type to Modifier:
@@ -12 +12
-                 val Modifier.fooModifier2: Modifier.Element get() {
+                 val Modifier.fooModifier2: Modifier get() {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 16: Change return type to Modifier:
@@ -16 +16
-                 val Modifier.fooModifier3: Modifier.Element get() = TestModifier
+                 val Modifier.fooModifier3: Modifier get() = TestModifier
            """
            )
    }

    @Test
    fun functionImplicitlyReturnsModifierElement() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier() = TestModifier
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier() = TestModifier
                             ~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Add explicit Modifier return type:
@@ -8 +8
-                 fun Modifier.fooModifier() = TestModifier
+                 fun Modifier.fooModifier(): Modifier = TestModifier
            """
            )
    }

    @Test
    fun getterImplicitlyReturnsModifierElement() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                val Modifier.fooModifier get() = TestModifier
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier get() = TestModifier
                                         ~~~
1 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Add explicit Modifier return type:
@@ -8 +8
-                 val Modifier.fooModifier get() = TestModifier
+                 val Modifier.fooModifier get(): Modifier = TestModifier
            """
            )
    }

    @Test
    fun returnsCustomModifierImplementation() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): TestModifier {
                    return TestModifier
                }
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier(): TestModifier {
                             ~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change return type to Modifier:
@@ -8 +8
-                 fun Modifier.fooModifier(): TestModifier {
+                 fun Modifier.fooModifier(): Modifier {
            """
            )
    }

    @Test
    fun modifierVariables_noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                var modifier1: TestModifier? = null
                var modifier2: TestModifier = TestModifier
                lateinit var modifier3: TestModifier
                var modifier4 = TestModifier
                    set(value) { field = TestModifier }
                var modifier5 = TestModifier
                    get() = TestModifier
                    set(value) { field = TestModifier }

                class Foo(
                    var modifier1: TestModifier,
                ) {
                    var modifier2: TestModifier? = null
                    var modifier3: TestModifier = TestModifier
                    lateinit var modifier4: TestModifier
                    var modifier5 = TestModifier
                        set(value) { field = TestModifier }
                    var modifier6 = TestModifier
                        get() = TestModifier
                        set(value) { field = TestModifier }
                }
            """
            ),
            modifierStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                fun Modifier.fooModifier(): Modifier {
                    return TestModifier
                }
            """
            ),
            modifierStub
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
