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

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
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
            ModifierDeclarationDetector.ModifierFactoryUnreferencedReceiver
        )

    // Simplified Density.kt stubs
    private val DensityStub = compiledStub(
        filename = "Density.kt",
        filepath = "androidx/compose/ui/unit",
        """
            package androidx.compose.ui.unit

            interface Density
        """,
"""
        androidx/compose/ui/unit/Density.class:
        H4sIAAAAAAAAAIVOTUvDQBB9s7FNjV+pH1Bv4g9w2+LNkyBCoCIoeMlpm6yy
        Tbor3U2pt/4uD9KzP0qcqHdn4M17M/DefH69fwC4xDHhTNly4Uy5koWbvzqv
        ZWNkY02QN9p6E95iECGdqaWStbIv8n4600WIERH6k8qF2lh5p4MqVVBXBDFf
        RuxNLXQIVPFqZVo1ZFaOCCebdS8RA5GIlNnzYLMeiyG1xzHhfPLfP5wBQvKn
        LqrA4tE1i0LfmloTTh8aG8xcPxlvprW+ttYFFYyzvssZ2MJvCRz+YB9HPEds
        2eHu5ogyxBl6jNhuIcmwg90c5LGH/RzC48Aj/QaMxaG1RAEAAA==
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcIlnpiXUpSfmVKhl5yfW5BfnKpX
        mqmXlp8vxOmWn++SWJLoXaLEoMUAAALEmjo+AAAA
        """
    )

    // Simplified ParentDataModifier.kt / Measurable.kt merged stubs
    private val MeasurableAndParentDataModifierStub = compiledStub(
        filename = "Measurable.kt",
        filepath = "androidx/compose/ui/layout",
        """
            package androidx.compose.ui.layout

            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Density

            interface ParentDataModifier : Modifier.Element {
                fun Density.modifyParentData(parentData: Any?): Any?
            }

            interface Measurable {
                val parentData: Any?
            }
        """,
"""
        androidx/compose/ui/layout/ParentDataModifier＄DefaultImpls.class:
        H4sIAAAAAAAAAKVSW08TQRT+pqW01FZKtSii9UKVXoSVxCf7ZECTTdpKxPTF
        p+nuUKbdnSGzsw3+Kx+JD8Znf5TxbGkQK0GJm+y5f9/MnHO+//jyFcBLOAyv
        uPKNlv6J4+nwWEfCiaUT8E86ts4+N0LZPW55V/vyUApT2xOHPA6sGx4HURaM
        oTTiE04ANXTeDUbCs1mkGRbskVAMYb1zLfr2peXn2cbVaYaNjjZDZyTswHCp
        IocrpS23UpPd07YXB0F7ejkZ5ZBjqI61DaRyRpPQkcoKo3jguMoaAkuPHphn
        qHhHwhvP0HRlHgoqZNisd+af3r4QOUhIhu1Gv4ACinncwE2GjKa+mBxKDLWr
        nlJ7E4iQepNFmWHnXyrnBnObwb+89/PA/2t5ARms5lHBHQbnmrOm9v9tniud
        2YC6wnKfwBRLhZM0LS9LRIaBjRODVi51IhPrBcPWte6RxQZD4WL7GIpdwaPY
        8EEgtseWNmZX+4JhuSOV6MXhQJgPSY6h3NEeD/rcyMSfBdffx8rKULhqIiNJ
        ode/1pAGP58936nfygquUsLsBjyKBLn5Ax0bT7yVyQFrM4r+H/TYQQoLSD6G
        JRrOItLYJK9L8RTpSrO8dIrlVnmFZPMbbrVOcfczJVKok8yTLoD2h8AN8lfP
        QMhhbUpaQQn3iLpJ9iLpLOkW/cXUzDmTaTyfktFUUMNTAm9Nj3iGbdJViq9T
        zf2PSLt44KJKEg9dPMJjF09+As+nXNieBAAA
        """,
        """
        androidx/compose/ui/layout/Measurable.class:
        H4sIAAAAAAAAAI1PwU7bQBB9YzuO65Li0AAJvVK1veAEcaInpAopUmirIKFK
        OW2SJdrE2UXedURv+ZYe+hE9oChHPgoxRkhB7aV7eDPz5s3Om/uHP3cATtAi
        vBd6nBs1vk1HZn5jrEwLlWbipylceiGFLXIxzGQVREimYiG4pyfpt+FUjlwV
        PqE2ke67yKV2X4QThMbHT72/hZ8Jhz2TT9KpdMNcKG1TobVxwinD+dciy8ot
        LKv3ZsZlSvNuJ8b8IXPefOGzXSqhQqAZU7eqrNqcjTuEzmq5FXtNL/aS1TL2
        Ij+6bq6Wx16b+o3EOwia1KYf69/B+lcYHgSRnwTl4DHhQ++/zmcTIMQ3L86s
        bbpHM0d41y+0U3PZ1QtlFbNnmwN59NIU+Uieq0wSWs/Sq3+EIdtCgPIRW6wg
        5LV7XJWxytHD/hPuosnxlNmIVa8G8LuIu3jNiK0Sal28wfYAZJGgPkDFYsfi
        rUXDlnn4CLI2KhoDAgAA
        """,
        """
        androidx/compose/ui/layout/ParentDataModifier.class:
        H4sIAAAAAAAAAJVRTW/TQBB963w4mARSKJC0fLWNqoIEDhEnqh4QAdUogQok
        Ljlt4k21ib0beddRc8vv4oBy5kchxgHUkgYQh52d9/xmdjzv67fPXwA8xx7D
        E67CRMvwzB/oeKKN8FPpR3ymU+uf8EQo2+aWd3Uoh1IkLhhDdcSnnDTq1H/f
        H4mBdZFjaKxr9Kuu8ToSMfVyUaD6OGNn590Zjg8668pTJa3fFspIOzvsrD57
        +OgyRXN0dHLqj4TtJ1wq43OltOVWasrfpVHE+5Eg2d7fZNpmSlJtdMbaRlL5
        XWF5SKMS58TTHG2PZYH+ho2JOpMZalIWPmM4Wsw3PafmLM9i7jnVLCzz0rC2
        mLecJntbqzpb+Rpr5h47zXyrVC38QMf7WZMWg792I390hgbbX1uxagEJwbD7
        730zeJMLFr34r3EabTHkaWSDeBIZF1sM5YsMQ6UruEmTzI2nY8uw/SFVVsYi
        UFNpJLEvzw0hU1e/0os8FlYkv8nKgVIieRVxYwRB76NOk4F4IyPBUP/Z4tOl
        9kXaOPK0lGJma57BRYmY+4SKxF6h+wGdikPAyyRLKoeHFD1C2yS+izp2CDu4
        h126j4i/igLKPeQCVAJco4jrWagG2MCNHpjBTWz2UDS4ZXDbwDW4Y1AzqH8H
        FbjiFqEDAAA=
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcIlnpiXUpSfmVKhl5yfW5BfnKpX
        mqmXlp8vxOmWn++SWJLoXaLEoMUAAALEmjo+AAAA
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
                    return this.then(TestModifier)
                }
            """
            ),
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier(): Modifier.Element {
                             ~~~~~~~~~~~
0 errors, 1 warnings
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
                    return this.then(TestModifier)
                }

                val Modifier.fooModifier2: Modifier.Element get() {
                    return this.then(TestModifier)
                }

                val Modifier.fooModifier3: Modifier.Element get() = this.then(TestModifier)
            """
            ),
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier get(): Modifier.Element {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier2: Modifier.Element get() {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier3: Modifier.Element get() = this.then(TestModifier)
                             ~~~~~~~~~~~~
0 errors, 3 warnings
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
-                 val Modifier.fooModifier3: Modifier.Element get() = this.then(TestModifier)
+                 val Modifier.fooModifier3: Modifier get() = this.then(TestModifier)
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
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier() = TestModifier
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier() = TestModifier
                             ~~~~~~~~~~~
1 errors, 1 warnings
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
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                val Modifier.fooModifier get() = TestModifier
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:8: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                val Modifier.fooModifier get() = TestModifier
                             ~~~~~~~~~~~
1 errors, 1 warnings
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
                    return this.then(TestModifier)
                }
            """
            ),
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should have a return type of Modifier [ModifierFactoryReturnType]
                fun Modifier.fooModifier(): TestModifier {
                             ~~~~~~~~~~~
0 errors, 1 warnings
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
            Stubs.Modifier
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
            Stubs.Modifier
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
                    return this.then(TestModifier)
                }

                val fooModifier get(): Modifier {
                    return this.then(TestModifier)
                }

                val fooModifier2: Modifier get() {
                    return this.then(TestModifier)
                }

                val fooModifier3: Modifier get() = TestModifier
            """
            ),
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun fooModifier(): Modifier {
                    ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier get(): Modifier {
                    ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier2: Modifier get() {
                    ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:20: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val fooModifier3: Modifier get() = TestModifier
                    ~~~~~~~~~~~~
0 errors, 4 warnings
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
                    return this.then(TestModifier)
                }

                val TestModifier.fooModifier get(): Modifier {
                    return this.then(TestModifier)
                }

                val TestModifier.fooModifier2: Modifier get() {
                    return this.then(TestModifier)
                }

                val TestModifier.fooModifier3: Modifier get() = this.then(TestModifier)
            """
            ),
            Stubs.Modifier
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:8: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun TestModifier.fooModifier(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:12: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier get(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:16: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier2: Modifier get() {
                                 ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:20: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                val TestModifier.fooModifier3: Modifier get() = this.then(TestModifier)
                                 ~~~~~~~~~~~~
0 errors, 4 warnings
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
-                 val TestModifier.fooModifier3: Modifier get() = this.then(TestModifier)
+                 val Modifier.fooModifier3: Modifier get() = this.then(TestModifier)
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
                    return this.then(TestModifier(value))
                }

                @Composable
                fun Modifier.fooModifier2(): Modifier =
                    this.then(TestModifier(someComposableCall(3)))

                @get:Composable
                val Modifier.fooModifier3: Modifier get() {
                    val value = someComposableCall(3)
                    return this.then(TestModifier(value))
                }

                @get:Composable
                val Modifier.fooModifier4: Modifier get() =
                    this.then(TestModifier(someComposableCall(3)))
            """
            ),
            Stubs.Modifier,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:13: Warning: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                fun Modifier.fooModifier1(): Modifier {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:19: Warning: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                fun Modifier.fooModifier2(): Modifier =
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:23: Warning: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                val Modifier.fooModifier3: Modifier get() {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:29: Warning: Modifier factory functions should not be marked as @Composable, and should use composed instead [ComposableModifierFactory]
                val Modifier.fooModifier4: Modifier get() =
                             ~~~~~~~~~~~~
0 errors, 4 warnings
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
-                     return this.then(TestModifier(value))
+                     this.then(TestModifier(value))
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 19: Replace @Composable with composed call:
@@ -18 +18
-                 @Composable
@@ -20 +19
-                     this.then(TestModifier(someComposableCall(3)))
+                     composed { this.then(TestModifier(someComposableCall(3))) }
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 23: Replace @Composable with composed call:
@@ -22 +22
-                 @get:Composable
-                 val Modifier.fooModifier3: Modifier get() {
+                 val Modifier.fooModifier3: Modifier get() = composed {
@@ -25 +24
-                     return this.then(TestModifier(value))
+                     this.then(TestModifier(value))
Fix for src/androidx/compose/ui/foo/TestModifier.kt line 29: Replace @Composable with composed call:
@@ -28 +28
-                 @get:Composable
@@ -30 +29
-                     this.then(TestModifier(someComposableCall(3)))
+                     composed { this.then(TestModifier(someComposableCall(3))) }
            """
            )
    }

    @Test
    fun unreferencedReceiver() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.*

                object TestModifier : Modifier.Element

                // Modifier factory without a receiver - since this has no receiver it should
                // trigger an error if this is returned inside another factory function
                fun testModifier(): Modifier = TestModifier

                interface FooInterface {
                    fun Modifier.fooModifier(): Modifier {
                        return TestModifier
                    }
                }

                fun Modifier.fooModifier(): Modifier {
                    return TestModifier
                }

                fun Modifier.fooModifier2(): Modifier {
                    return testModifier()
                }

                fun Modifier.fooModifier3(): Modifier = TestModifier

                fun Modifier.fooModifier4(): Modifier = testModifier()

                fun Modifier.fooModifier5(): Modifier {
                    return Modifier.then(TestModifier)
                }

                fun Modifier.fooModifier6(): Modifier {
                    return Modifier.fooModifier()
                }
            """
            ),
            Stubs.Modifier,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/androidx/compose/ui/foo/TestModifier.kt:10: Warning: Modifier factory functions should be extensions on Modifier [ModifierFactoryExtensionFunction]
                fun testModifier(): Modifier = TestModifier
                    ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:13: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                    fun Modifier.fooModifier(): Modifier {
                                 ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:18: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier(): Modifier {
                             ~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:22: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier2(): Modifier {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:26: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier3(): Modifier = TestModifier
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:28: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier4(): Modifier = testModifier()
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:30: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier5(): Modifier {
                             ~~~~~~~~~~~~
src/androidx/compose/ui/foo/TestModifier.kt:34: Error: Modifier factory functions must use the receiver Modifier instance [ModifierFactoryUnreferencedReceiver]
                fun Modifier.fooModifier6(): Modifier {
                             ~~~~~~~~~~~~
7 errors, 1 warnings
            """
            )
    }

    @Test
    fun ignoresParentDataModifiers() {
        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.layout.Measurable
                import androidx.compose.ui.layout.ParentDataModifier
                import androidx.compose.ui.unit.Density

                private val Measurable.boxChildData: FooData? get() = parentData as? FooData

                private class FooData(var boolean: Boolean) : ParentDataModifier {
                    override fun Density.modifyParentData(parentData: Any?) = this
                }
            """
            ),
            Stubs.Modifier,
            DensityStub,
            MeasurableAndParentDataModifierStub
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

                object TestModifier : Modifier.Element

                fun Modifier.fooModifier(): Modifier {
                    return this.then(TestModifier)
                }

                fun Modifier.fooModifier2(): Modifier {
                    return then(TestModifier)
                }

                fun Modifier.fooModifier3(): Modifier {
                    return fooModifier()
                }
            """
            ),
            Stubs.Modifier
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
