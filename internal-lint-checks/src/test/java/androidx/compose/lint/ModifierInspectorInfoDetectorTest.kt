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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class ModifierInspectorInfoDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierInspectorInfoDetector()

    override fun getIssues(): List<Issue> = listOf(ModifierInspectorInfoDetector.ISSUE)

    private val modifierFile = kotlin(
        """
        package androidx.compose.ui

        interface Modifier {
          infix fun then(other: Modifier): Modifier =
              if (other === Modifier) this else CombinedModifier(this, other)

          interface Element : Modifier {
          }

          companion object : Modifier {
            override infix fun then(other: Modifier): Modifier = other
          }
        }

        class CombinedModifier(
            private val outer: Modifier,
            private val inner: Modifier
        ) : Modifier {}
        """
    ).indented()

    private val inspectableInfoFile = kotlin(
        """
        package androidx.compose.ui.platform

        val NoInspectorInfo: InspectorInfo.() -> Unit = {}
        val DebugInspectorInfo = false

        interface InspectableValue {
            val inspectableElements: Sequence<ValueElement>
                get() = emptySequence()

            val nameFallback: String?
                get() = null

            val valueOverride: Any?
                get() = null
        }

        data class ValueElement(val name: String, val value: Any?)

        class InspectorInfo {
            var name: String? = null
            var value: Any? = null
            val properties = ValueElementSequence()
        }

        class ValueElementSequence : Sequence<ValueElement> {
            private val elements = mutableListOf<ValueElement>()

            override fun iterator(): Iterator<ValueElement> = elements.iterator()

            operator fun set(name: String, value: Any?) {
                elements.add(ValueElement(name, value))
            }
        }

        abstract class InspectorValueInfo(
            private val info: InspectorInfo.() -> Unit
        ) : InspectableValue {
            private var _values: InspectorInfo? = null

            private val values: InspectorInfo
                get() {
                    val valueInfo = _values ?: InspectorInfo().apply { info() }
                    _values = valueInfo
                    return valueInfo
                }

            override val nameFallback: String?
                get() = values.name

            override val valueOverride: Any?
                get() = values.value

            override val inspectableElements: Sequence<ValueElement>
                get() = values.properties
        }

        inline fun debugInspectorInfo(
            crossinline definitions: InspectorInfo.() -> Unit
        ): InspectorInfo.() -> Unit =
            if (DebugInspectorInfo) ({ definitions() }) else NoInspectorInfo

        """
    ).indented()

    @Test
    fun existingInspectorInfo() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                inline class Dp(val value: Float)

                fun Modifier.preferredWidth1(width: Dp) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        properties["width"] = width
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)

                """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithStatementsBeforeDefinition() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                inline class Dp(val value: Float)

                inline fun require(value: Boolean, lazyMessage: () -> String) {
                    if (!value) {
                        val message = lazyMessage()
                        throw IllegalArgumentException(message)
                    }
                }

                fun Modifier.preferredWidth1(width: Dp): Modifier {
                    require(width.value > 0.0f) { return "sds" }

                    val x = width.value.toInt() * 2
                    for (i in 0..4) {
                        println("x = " + x)
                    }

                    return this.then(SizeModifier1(x, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        properties["width"] = width
                    }))
                }

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)

                """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithValue() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth2(width: Int) =
                    this.then(SizeModifier2(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth2"
                        value = width
                    }))

                private class SizeModifier2(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoViaSynonym() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                inline class Dp(val value: Float)

                fun Modifier.preferredWidth1(width: Dp) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        properties["width"] = width
                    }))

                fun Modifier.preferredWidth2(width: Dp) = preferredWidth1(width)

                fun Modifier.preferredWidth20() = preferredWidth1(Dp(20.0f))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)

                """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithDataClassMemberValues() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(values: Borders) =
                    this.then(BorderModifier(values, inspectorInfo = debugInspectorInfo {
                        name = "border"
                        properties["start"] = values.start
                        properties["top"] = values.top
                        properties["end"] = values.end
                        properties["bottom"] = values.bottom
                    }))

                private class BorderModifier(
                    val values: Borders,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                data class Borders(val start: Int, val top: Int, val end: Int, val bottom: Int) {
                    constructor(all: Int) : this(all, all, all, all)
                }

                """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithConditional() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(painter: Painter) =
                    this.then(
                        if (painter.size > 0) {
                            BorderModifier(inspectorInfo = debugInspectorInfo {
                                name = "border"
                                properties["painter"] = painter
                            })
                        } else {
                            Modifier
                        }
                    )

                private class BorderModifier(
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                class Painter(val size: Int)

                """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun missingInspectorInfo() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth2(width: Int) = this.then(SizeModifier2(width))

                private data class SizeModifier2(
                    val width: Int,
                ): Modifier.Element

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier2.kt:8: Error: Modifiers should include inspectorInfo for the Layout Inspector [ModifierInspectorInfo]
                    fun Modifier.preferredWidth2(width: Int) = this.then(SizeModifier2(width))
                                                                         ~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun missingInspectorInfoFromInnerClassImplementation() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                /**
                 * Documentation
                 */
                fun Modifier.preferredSize(width: Int) =
                    this.then(SizeModifier.WithOption(width))

                internal sealed class SizeModifier : Modifier.Element {
                    internal data class WithOption(val width: Int) : SizeModifier() {
                    }
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier.kt:12: Error: Modifiers should include inspectorInfo for the Layout Inspector [ModifierInspectorInfo]
                        this.then(SizeModifier.WithOption(width))
                                               ~~~~~~~~~~
                    1 errors, 0 warnings
                """

            )
    }

    @Test
    fun inspectorInfoWithWrongName() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth1(width: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "otherName"
                        value = width
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:10: Error: Expected name of the modifier: "name" = "preferredWidth1" [ModifierInspectorInfo]
                            name = "otherName"
                                    ~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithWrongValue() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth1(width: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        value = 3.4
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:11: Error: Expected the variable: "width" [ModifierInspectorInfo]
                            value = 3.4
                                    ~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithWrongValueWhenMultipleAreAvailable() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        value = "oldWidth"
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:11: Error: Expected one of the variables: "width, height" [ModifierInspectorInfo]
                            value = "oldWidth"
                                     ~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithWrongParameterNameInProperties() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        properties["width"] = width
                        properties["other"] = height
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:12: Error: Expected one of the variables: "width, height" [ModifierInspectorInfo]
                            properties["other"] = height
                                        ~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithMismatchInProperties() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "preferredWidth1"
                        properties["height"] = width
                        properties["width"] = height
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }
                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:11: Error: The value should match the index name: height [ModifierInspectorInfo]
                            properties["height"] = width
                                                   ~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithMissingDebugSelector() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo

                fun Modifier.preferredWidth1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, height, inspectorInfo = {
                        name = "preferredWidth1"
                        properties["width"] = width
                        properties["height"] = height
                    }))

                private class SizeModifier1(
                    val width: Int,
                    val height: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:8: Error: Expected debugInspectorInfo call [ModifierInspectorInfo]
                        this.then(SizeModifier1(width, height, inspectorInfo = {
                                                                               ^
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithMissingName() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.preferredWidth1(width: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        value = width
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:9: Error: Expected name of the modifier: "name" = "preferredWidth1" [ModifierInspectorInfo]
                        this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                                                                                          ^
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithMissingVariables() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(start: Int, top: Int, end: Int, bottom: Int) =
                    this.then(
                        BorderModifier(
                            start, top, end, bottom, debugInspectorInfo {
                                name = "border"
                                properties["start"] = start
                            }
                        ))

                private class BorderModifier(
                    val start: Int,
                    val top: Int,
                    val end: Int,
                    bottom: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/BorderModifier.kt:11: Error: These lambda arguments are missing in the InspectorInfo: bottom, end, top [ModifierInspectorInfo]
                                start, top, end, bottom, debugInspectorInfo {
                                                                            ^
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithMissingDataClassMemberValues() {
        lint().files(
            modifierFile,
            inspectableInfoFile,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(values: Borders) =
                    this.then(BorderModifier(values, inspectorInfo = debugInspectorInfo {
                        name = "border"
                        value = values.start
                        properties["top"] = values.top
                    }))

                private class BorderModifier(
                    val values: Borders,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                data class Borders(val start: Int, val top: Int, val end: Int, val bottom: Int) {
                    constructor(all: Int) : this(all, all, all, all)
                }

                """
            ).indented()
        )
            .run()
            .expect(
                """
                    src/androidx/compose/ui/BorderModifier.kt:9: Error: These lambda arguments are missing in the InspectorInfo: bottom, end [ModifierInspectorInfo]
                        this.then(BorderModifier(values, inspectorInfo = debugInspectorInfo {
                                                                                            ^
                    1 errors, 0 warnings
                """
            )
    }
}
