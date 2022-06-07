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
        checksum = 0xaa534a7a,
        """
            package androidx.compose.ui.unit

            interface Density
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcIlnpiXUpSfmVKhl5yfW5BfnKpX
        mqmXlp8vxOmWn++SWJLoXaLEoMUAAALEmjo+AAAA
        """,
        """
        androidx/compose/ui/unit/Density.class:
        H4sIAAAAAAAAAIVOTUvDQBB9s7FNjV+pH1Bv4g9w2+LNkyBCoCIoeMlpm6yy
        Tbor3U2pt/4uD9KzP0qcqHdn4M17M/DefH69fwC4xDHhTNly4Uy5koWbvzqv
        ZWNkY02QN9p6E95iECGdqaWStbIv8n4600WIERH6k8qF2lh5p4MqVVBXBDFf
        RuxNLXQIVPFqZVo1ZFaOCCebdS8RA5GIlNnzYLMeiyG1xzHhfPLfP5wBQvKn
        LqrA4tE1i0LfmloTTh8aG8xcPxlvprW+ttYFFYyzvssZ2MJvCRz+YB9HPEds
        2eHu5ogyxBl6jNhuIcmwg90c5LGH/RzC48Aj/QaMxaG1RAEAAA==
        """
    )

    // Simplified ParentDataModifier.kt / Measurable.kt merged stubs
    private val MeasurableAndParentDataModifierStub = compiledStub(
        filename = "Measurable.kt",
        filepath = "androidx/compose/ui/layout",
        checksum = 0xd1bf915a,
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
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcIlnpiXUpSfmVKhl5yfW5BfnKpX
        mqmXlp8vxOmWn++SWJLoXaLEoMUAAALEmjo+AAAA
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
                    val modifier1: TestModifier?
                    val modifier2: TestModifier
                }

                object Baz : Bar {
                    override val modifier1: TestModifier? = null
                    override val modifier2: TestModifier = TestModifier
                    val modifier3: TestModifier = TestModifier
                    val modifier4: TestModifier? get() = null
                    val modifier5: TestModifier get() = TestModifier
                }

                val Qux = object : Bar {
                    override val modifier1: TestModifier? = null
                    override val modifier2: TestModifier = TestModifier
                    val modifier3: TestModifier = TestModifier
                    val modifier4: TestModifier? get() = null
                    val modifier5: TestModifier get() = TestModifier
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
    fun noErrors_inlineAndValueClasses() {
        val inlineAndValueClassStub = compiledStub(
            filename = "InlineAndValueClassStub.kt",
            filepath = "androidx/compose/ui/foo",
            checksum = 0x16c1f1c4,
            """
            package androidx.compose.ui.foo

            import androidx.compose.ui.Modifier

            inline class Inline(val value: Float)

            @JvmInline
            value class Value(val value: Float)

            private object TestModifier : Modifier.Element

            fun Modifier.inline(inline: Inline = Inline(1f)): Modifier {
                return this.then(TestModifier)
            }

            fun Modifier.value(value: Value = Value(1f)): Modifier {
                return this.then(TestModifier)
            }
        """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAAAGNgYGBmYGBgBGI2BijgsuEST8xLKcrPTKnQS87PLcgvTtUr
            zdRLy88XkvTMy8nMS3XMSwlLzClNdc5JLC4OLilN8i4RYgtJLS7xLlFi0GIA
            APJuU+dWAAAA
            """,
            """
            androidx/compose/ui/foo/Inline.class:
            H4sIAAAAAAAAAIVU3VMbVRT/3ZuvzbLAkvIVrJVCLQkfDdBqVVpaQGKDoShU
            LMWvJVlgIdnF7IbhkfFF/wIffPSZ6eiMAmNHB+mbf5PjeO7dDWDI1Jlk7z3n
            no/f+Z1z71///PYHgDvYZLhm2MWKYxX3MgWnvOO4ZqZqZdYdJ5OzS5ZtxsAY
            9C1j18iUDHsjs7C2ZRa8GEIMyobpLRulqskQSqWzDJFdX2JZDTEocXDEGcLe
            puUy9OZfnWiCodlzlryKZW+MWOWdEkNHKpvOn6f2z8iuq143XbVKRbMSQytD
            9J5lW96kxLSsoQ0JFTquUHQ/T0qCvK+gg2yNnR3TLjKMpC7nuZw6SDOhoQvd
            ImqS4WojjBcNXxOGV4XhzKsNrwnDN4jWGgsM7akG9Wu4jj5h20/cGpWNUQ3N
            aFGJ7JtU5Kbhbs44RTOgMEzwcgyt51GyJceg/g1SopqthmGkVQxhRLKW05AS
            MscoQ5P5ddUouUG4zlQ2Xz8LE+lnDGrVXnP2pJVGcxUV3m8xxCTZC+uilP/w
            JFFQKXfxjkj8Ls3htuNRfzJbu+WMZXtmxTZKNBqiZtcquDFQ4xWjYs4KPAwD
            qctAGkHTcB+TKu7hAc2n422aFYbEZUOaBb9QMXWN44zjtqhq1id1WUVYTJVe
            cGzXq1QLnlO5wDndBqVGCEO/KP5/xl+M6gciwTxdoF0G7QLx1IdIKpsVRPOd
            MfEZp17kLzA2t1s+u0ZttYN50zOKhmeQjpd3Q3TjmfhExQeUZZv0e5aQKAEv
            UuDfT/YHVd7NVa6f7Kv043pM5UqE1iZaw7QqtIZobaGVK6ffPuw+2R/no2w6
            kYjqvIePhl4es5P90x+jYSWsR+Z6dIWU8XFFV3vC3WyUPXr5fUieNunanK43
            02kL6ZjUteo66dpIlzjTXdHbF9v80CQrBKsnrET12Ol3jPu5vuGETEmKIogY
            Kq3HJ2PKLsoHaqZkuO6SV127te1Rf8TM053Ik8XjannNrDwx1kqmmAunYJSW
            jYol5EDZvOQZhe15YyeQ1SWnWimYWUsIycWq7Vllc9lyLTqdsm3HMzyLRgJj
            1Mqw5DkhHkPaiQ5HECXNGkkZ0QFaI4O/QH1OG46CaI5URlGUDtIATbQD4uKa
            B853yVqcJV9AXzlCe6LzED19h3hdTx+i9xA3fpKZz4Mk8abEwMTjEQS5GSBQ
            BIJjDNT7KGeJ6TkIfPprqPuOceugziFylmRYltcgyVi9z3kSul+Bz1OqTkxl
            /9Cf4D8gEjoYOgE/xNuzlPW9G/Q/xkOpDx9I4kz6xsDjf6OLy9idpBRgfTxi
            N4VpiWQG7wdZRIOEVVwgGzpG9hya7x4PoImddNe5uKOB+2Tgrg4e4dFg/69Q
            f27YRD+WehZLldPAKGYOc0Gs3oAk3ve8jh7uz46exIfIB9YDRI44i78AX+k7
            wuP6xsWxIJ3axEtc37jauLEGI5bER/g4cLgT1KcJzvt9zusZ0rAYEKxhSVbF
            sS5RG9ig1abdE1o/oQzLqwjl8GkOT3NYwTPaYjWHz/D5KpiLL/DlKjpcaC6+
            chGT35yLORcRF1EXM1Iz5WLcxW0Xw1JMuUi7uC73zS5a/gWm4bi81wgAAA==
            """,
            """
            androidx/compose/ui/foo/InlineAndValueClassStubKt.class:
            H4sIAAAAAAAAAIVU31PbRhD+Tv4lZEMMJCQQIElxE+zEkSH0p9sk1IlTJcbt
            xAwvPHTOsoDDssRIJ0/60mH6X/Sx/Qs6faJ96HjoW/+oTlayQ2qC8YP2dve+
            29v9dk///vfX3wA28JJhjTstzxWtN7rpdo5c39IDoe+5rm44tnCsTae1w+3A
            qtjc9xsyaL6SKTCG7CHvct3mzr7+XfPQMskbY5gS0aFiacP5Yeuly/BktXZR
            /C23JfaE5ZWr+cv3GVZqrrevH1qy6XHh+Dp3HFdyKVzS666sB7ZNqExOHgg/
            179ehcqw3HYlGfpht6MLR1qew22qSXoURJh+ChrDNfPAMtuDKN9zj3csAjLc
            W62dL6/8P08jDLJfzu9kkMGkhjSmGHKjeNy2fPmunhSyDKpRb2xv1ivPGe5e
            WP35U+UMZjA7gWlcZVi8jK8U5hji8sByGB6PoX4M8xncwHwa17FAXI5rUrJP
            PAOrMswND0GuZe3xwJYMr8cNg/Eh7WPnY/ny+U3hDg2rSdMivcCUrlcUnSOb
            SFqt5qsZrCCn4SN8nEECSQ0K7jFMdsOBL64VxSteNRnS/dmKvCoKDIlIpfEZ
            Ar6vc2lUStFLSuGhBj28Mt+/co1hujaY1i1L8haXnApTOt0YPVIWimQoQPS2
            Q0WhzTci1Eqktej8j73jq1rvWFNuKP1PjUVr73jhTpaEUmIF+tZVVembyj8n
            rHdMgp3+loyrsWycgIkhXGglh2GpbPz0ZyVN4ee1hHr663KJhQmsM9weOcj9
            NlA9t0ZCIloIsTDil/OwTaTGK26LOL9SI0Q96DQtb5s3bfLM1FyT2zvcE6E9
            cN58HThSdCzD6QpfkGvz/W+D3ur53bO3PwSbbEhutrf40SCo1nADz7SqIjTm
            BzF2PoiPNWpqPGwYyflwsBDDN2S9IL9C62xhZuIEVwp/4FoP1//ETQW/h21F
            JWw1tTmJKTwjfa4Ph4rFKNwslrBM+88HuBStVfomlIGB7ARu4TZZ4X3rgzym
            F+M//QI1c4K7zworJ1jt3/aCZAwsfXYtMEn55i/K9/6YfGeG8i2c5ftgfL7F
            S/ItRfmuj8x3mtzfRpubMGitkPcRMb6xi5iBTwx8auAzfG7gC3xpoIyvdsF8
            fI3Hu1B9LPlY9PHER8JH0scDH0995N8CsSzNZB0HAAA=
            """,
            """
            androidx/compose/ui/foo/TestModifier.class:
            H4sIAAAAAAAAAKVUW0/UQBg9Mwu7pRa5eOGmeGFFLkqB+AaSIGLSZFmNkE0M
            T7PbAQfaDmmnhEfiT/EXSHzAaGI2+uaPMn4tCwlykcSH+W5zzrffnJnur99f
            vwN4hmmGsoj8WCt/z23ocEcn0k2Vu6G1uyYTs6J9taFkXAJj6N4Su8INRLTp
            vq5vyYYpoXAB/5hXXg5kKCNCtjMU51WkzAJDYWy85qAEy0YbOhjazHuVMIxW
            rjLKXI6XEcPC2LmEE+D45dsMIxUdb7pb0tRjoaLEFVGkjTBKU1zVppoGAaFm
            rnLA8ku5IdLAeOFOkJTQzeBfPt0xce6/zuCgE702enCDoV2TLDHD8L+Obc03
            gvwmbPBMfsurrq4tVpeWHQzA6aDiIENPZVsbgrkr0ghfGEFEHu4W6NmwzBQz
            Awa2TfU9lWX0mLg/Q7I2922b9/N8Nfetnx94f3N/lk+zFyWL//hY5N08g85e
            dOVnNGIY9CIaRi5Gfk0EqVwKRJKsmrQ+tW0Yht6mkVGh9KJdlah6QLCTi6TX
            sqR9ydBVIX41DesyXhOEYeit6IYIaiJWWd4qlv/u9UbEIpRGxqea2qs6jRvy
            lco4Ay1O7cyvY5rUbCOZirQGMnnJj2fyke8iX6B9+hAom6DMzQQl3z5xCPuA
            Ao7JFhi4hidknSMAZU6ufyeuU5OM/JzQnHzHRBPFyS+4+encBrePQK0GWXSL
            aqenekqrxFqJhb6TAftyMrX6Bv7uEP2fMXSQFzimcjtGZ8j+WBju0JB311Hw
            MOzhnof7eEAhHnoYQXkdLMEjjNJ+AifB4wTWH5GWtQCVBAAA
            """,
            """
            androidx/compose/ui/foo/Value.class:
            H4sIAAAAAAAAAH1UW1MjRRT+unObDAMMWW6Jew3rEm4bYFdXZRcXkLhBWBRW
            XBZvk2SAgWQmZiYpHilf9Bf44KPP1JZWKVBuaSH75m+yLE93hoshRVXS3ef0
            uXznO6fn739//xPAfWwyXDPsQsWxCjvpvFMqO66ZrlrpdcdJrxjFqhkBY9C3
            jJqRLhr2Rnoxt2XmvQgCDMqG6UkbhkBqIMMQqtUlltEQgRIFR5Qh6G1aLsON
            +UvzTDC0es6yV7HsjRGrVC4ydKUyA/Nnmet3ZNfTqJuuWsWCWYmgnSH80LIt
            b1JCWtHQgZgKHVcYNJkmJSE+UtBFpka5bNoFhpHUxTQXM/tZJjT0oFcEjTNc
            bQbxvOEbwvCqMJy53PC6MLxBpJ6QwNCZalK+hltICts+YtaobIxqaEWbSlTf
            IQY3DXdzximYPoNBgpdlaD+Lkik6BnVvkBKd2GoYxoCKIYxI0rIaUkLmGGVo
            Mb+pGkXXD9edysw3TsLEwAsGtWrnnB1ppdFQhYX3WwwRSfbiuijlfzxJFFTK
            A7wjEr/LcH3b8YqWnd6qldKW7ZkV2yims7ao2bXybgTUd8WomLMCD0N/6iKQ
            ZtA0PMKkiod4n6bT8TbNCkPsoiHNQr1QMXTN44zjnqhqtk7qioqgGCo979iu
            V6nmPadyjnN6C8oJIQxJUfzlwy8G9UMRf4FeT42G9Rzv1IZQKpMRPPPymFjG
            qRXz5wibq5WyNgniEXWcXCyYnlEwPIN0vFQL0GtnYgmLBZRlm/Q7lpAoAS9Q
            4D+OdgdV3stVrh/tqvTjekTlSoj2FtqDtCu0B2hvo50rx9897j3aHeejbDoW
            C+s8wUcDrw/Z0e7xT+GgEtRDcwldIWV0XNHVRLCXjbInr38IyNsWXZvT9Va6
            bSMdk7p2XSddB+lip7oreudSRz00yQrBSgSVsB45/p7xeq5vOSFT4qIIIoZK
            S9TJmLILktyZouG6y141d3fbo/aIkacnMU8WT6ulnFl5ZuSKphgLJ28UV4yK
            JWRf2brsGfntBaPsy+qyU63kzYwlhPhS1faskrliuRbdTtm24xmeRROBMWpl
            UPIcE19COokOhxAmTY6ktOgA7aHBX6G+pANHXjRHKqMoSAdpgBY6CR29ct/5
            AVmLu/gr6KsH6Ix17yOR3Mc1fWAfN/dx+2eZ+SxIHG9KDEx8O/wgd3wEikBw
            iP5GH+U0MX0NfJ++E9TJQ9zda3AInSYZluU1STLW6HOWhJ6X7/OcqhNT2Tf0
            F/iPCAX2ho7A9/H2LGV97zb9D/FY6oN7kjiT1gh49B/0cBm7m5QCbB2POE1h
            WiKZwQd+FtEgYRUVyIYOkTmDVneP+tDESbrrXLxR333Sd1cHD/BksO83qL80
            bWI9lnoaS5XTwOg2izk/1k2fJJ582UAPr8+OHsdHmPet+4kcGf8V+GryAE8b
            GxfFonTqEB/ixsadjBtrMmJxfIxPfIf7fn2a4LyvznkjQxqWfII1LMuqONYl
            agMbtNt0ekb7p5RhZQ2BLD7L4nkWq3hBR6xl8Tm+WANz8SW+WkOXC83F1y4i
            cs26mHMRchF2MSM1Uy7GXdxzMSzFlIsBF7fkudVF239hftWt0wgAAA==
            """
        )

        lint().files(
            kotlin(
                """
                package androidx.compose.ui.foo

                import androidx.compose.ui.Modifier

                fun Modifier.inlineModifier(): Modifier = inline()

                fun Modifier.valueModifier(): Modifier = value()
            """
            ),
            Stubs.Modifier,
            inlineAndValueClassStub
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
