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

/**
 * Test for [ModifierDeclarationDetector].
 */
@RunWith(JUnit4::class)
class ModifierDeclarationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierDeclarationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            ModifierDeclarationDetector.ComposableModifierFactory,
            ModifierDeclarationDetector.ModifierFactoryExtensionFunction,
            ModifierDeclarationDetector.ModifierFactoryReturnType,
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
    fun modifierVals_noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                val modifier1: TestModifier? = null
                val modifier2: TestModifier = TestModifier

                class Foo(
                    val modifier1: TestModifier,
                ) {
                    val modifier2: TestModifier? = null
                    val modifier3: TestModifier = TestModifier
                    val modifier4: TestModifier? get() = null
                    val modifier5: TestModifier get() = TestModifier
                }

                interface Bar {
                    val modifier2: TestModifier
                    val modifier3: TestModifier
                }
            """
            ),
            modifierStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun noModifierReceiver() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun fooModifier(): Modifier {
                    return TestModifier
                }

                val fooModifier get(): Modifier {
                    return TestModifier
                }

                val fooModifier2: Modifier get() {
                    return TestModifier
                }

                val fooModifier3: Modifier get() = TestModifier
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun fooModifier(): Modifier {
                    ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier get(): Modifier {
                                ~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier2: Modifier get() {
                                           ~~~
src/androidx/compose/ui/foo/TestModifier.kt:20: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier3: Modifier get() = TestModifier
                                           ~~~
4 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Add Modifier receiver:
@@ -8 +8
-                 fun fooModifier(): Modifier {
+                 fun Modifier.fooModifier(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 12: Add Modifier receiver:
@@ -12 +12
-                 val fooModifier get(): Modifier {
+                 val Modifier.fooModifier get(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 16: Add Modifier receiver:
@@ -16 +16
-                 val fooModifier2: Modifier get() {
+                 val Modifier.fooModifier2: Modifier get() {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 20: Add Modifier receiver:
@@ -20 +20
-                 val fooModifier3: Modifier get() = TestModifier
+                 val Modifier.fooModifier3: Modifier get() = TestModifier
            """
            )
    }

    @Test
    fun incorrectReceiver() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun TestModifier.fooModifier(): Modifier {
                    return TestModifier
                }

                val TestModifier.fooModifier get(): Modifier {
                    return TestModifier
                }

                val TestModifier.fooModifier2: Modifier get() {
                    return TestModifier
                }

                val TestModifier.fooModifier3: Modifier get() = TestModifier
            """
            ),
            modifierStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun TestModifier.fooModifier(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier get(): Modifier {
                                             ~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier2: Modifier get() {
                                                        ~~~
src/androidx/compose/ui/foo/TestModifier.kt:20: Error: Modifier factory functions must be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier3: Modifier get() = TestModifier
                                                        ~~~
4 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 8: Change receiver to Modifier:
@@ -8 +8
-                 fun TestModifier.fooModifier(): Modifier {
+                 fun Modifier.fooModifier(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 12: Change receiver to Modifier:
@@ -12 +12
-                 val TestModifier.fooModifier get(): Modifier {
+                 val Modifier.fooModifier get(): Modifier {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 16: Change receiver to Modifier:
@@ -16 +16
-                 val TestModifier.fooModifier2: Modifier get() {
+                 val Modifier.fooModifier2: Modifier get() {
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 20: Change receiver to Modifier:
@@ -20 +20
-                 val TestModifier.fooModifier3: Modifier get() = TestModifier
+                 val Modifier.fooModifier3: Modifier get() = TestModifier
            """
            )
    }

    @Test
    fun composableModifierFactories() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier

                class TestModifier(val value: Int) : Modifier.Element

                @Composable
                fun someComposableCall(int: Int) = 5

                @Composable
                fun Modifier.fooModifier1(): Modifier {
                    val value = someComposableCall(3)
                    return TestModifier(value)
                }

                @Composable
                fun Modifier.fooModifier2(): Modifier = TestModifier(someComposableCall(3))

                @get:Composable
                val Modifier.fooModifier3: Modifier get() {
                    val value = someComposableCall(3)
                    return TestModifier(value)
                }

                @get:Composable
                val Modifier.fooModifier4: Modifier get() = TestModifier(someComposableCall(3))
            """
            ),
            modifierStub,
            composableStub
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:13: Error: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                fun Modifier.fooModifier1(): Modifier {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:19: Error: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                fun Modifier.fooModifier2(): Modifier = TestModifier(someComposableCall(3))
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:22: Error: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                val Modifier.fooModifier3: Modifier get() {
                                                    ~~~
src/androidx/compose/ui/foo/TestModifier.kt:28: Error: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                val Modifier.fooModifier4: Modifier get() = TestModifier(someComposableCall(3))
                                                    ~~~
4 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 13: Replace @Composable with composed call:
@@ -12 +12
-                 @Composable
-                 fun Modifier.fooModifier1(): Modifier {
+                 fun Modifier.fooModifier1(): Modifier = composed {
@@ -15 +14
-                     return TestModifier(value)
+                     TestModifier(value)
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 19: Replace @Composable with composed call:
@@ -18 +18
-                 @Composable
-                 fun Modifier.fooModifier2(): Modifier = TestModifier(someComposableCall(3))
+                 fun Modifier.fooModifier2(): Modifier = composed { TestModifier(someComposableCall(3)) }
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 22: Replace @Composable with composed call:
@@ -21 +21
-                 @get:Composable
-                 val Modifier.fooModifier3: Modifier get() {
+                 val Modifier.fooModifier3: Modifier get() = composed {
@@ -24 +23
-                     return TestModifier(value)
+                     TestModifier(value)
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 28: Replace @Composable with composed call:
@@ -27 +27
-                 @get:Composable
-                 val Modifier.fooModifier4: Modifier get() = TestModifier(someComposableCall(3))
+                 val Modifier.fooModifier4: Modifier get() = composed { TestModifier(someComposableCall(3)) }
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

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
