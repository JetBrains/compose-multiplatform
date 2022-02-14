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

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
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

    private val inspectableInfoStub = kotlin(
        """
        package androidx.compose.ui.platform

        import androidx.compose.ui.Modifier

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

        fun Modifier.inspectable(
            inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
            wrapped: Modifier
        ): Modifier = this.then(InspectableModifierImpl(inspectorInfo, wrapped))

        /**
         * Interface for a [Modifier] wrapped for inspector purposes.
         */
        interface InspectableModifier {
            val wrapped: Modifier
        }

        private class InspectableModifierImpl(
            inspectorInfo: InspectorInfo.() -> Unit,
            override val wrapped: Modifier
        ) : Modifier.Element, InspectableModifier, InspectorValueInfo(inspectorInfo) {
            override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
                wrapped.foldIn(operation(initial, this), operation)

            override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
                operation(this, wrapped.foldOut(initial, operation))

            override fun any(predicate: (Modifier.Element) -> Boolean): Boolean =
                wrapped.any(predicate)

            override fun all(predicate: (Modifier.Element) -> Boolean): Boolean =
                wrapped.all(predicate)
        }
        """
    ).indented()

    private val composedStub = kotlin(
        """
        package androidx.compose.ui

        import androidx.compose.ui.platform.InspectorInfo
        import androidx.compose.ui.platform.InspectorValueInfo

        fun Modifier.composed(
            inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
            factory: Modifier.() -> Modifier
        ): Modifier = this.then(ComposedModifier(inspectorInfo, factory))

        private class ComposedModifier(
            inspectorInfo: InspectorInfo.() -> Unit,
            val factory: Modifier.() -> Modifier
        ) : Modifier.Element, InspectorValueInfo(inspectorInfo)
        """
    ).indented()

    @Test
    fun existingInspectorInfo() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                inline class Dp(val value: Float)

                fun Modifier.width1(width: Dp) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
                        properties["width"] = width
                    }))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithStatementsBeforeDefinition() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
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

                fun Modifier.width1(width: Dp): Modifier {
                    require(width.value > 0.0f) { return "sds" }

                    val x = width.value.toInt() * 2
                    for (i in 0..4) {
                        println("x = " + x)
                    }

                    return this.then(SizeModifier1(x, inspectorInfo = debugInspectorInfo {
                        name = "width1"
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
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithValue() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width2(width: Int) =
                    this.then(SizeModifier2(width, inspectorInfo = debugInspectorInfo {
                        name = "width2"
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
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoViaSynonym() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                inline class Dp(val value: Float)

                fun Modifier.width1(width: Dp) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
                        properties["width"] = width
                    }))

                fun Modifier.width2(width: Dp) = width1(width)

                fun Modifier.width20() = width1(Dp(20.0f))

                fun Modifier.preferredIconWidth(x: Int) = this.then(
                    if (x == 7) DefaultIconSizeModifier else Modifier
                )

                private val DefaultIconSizeModifier = Modifier.width1(Dp(24.0f))

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)
                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithAnonymousClass() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.drawBehind() = this.then(
                  object : Modifier,
                           InspectorValueInfo(debugInspectorInfo { name = "drawBehind" }) {}
                )
                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithDataClassMemberValues() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
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

                fun Modifier.border2(start: Int, top: Int, end: Int, bottom: Int) =
                    this.then(
                        BorderModifier2(
                            start, top, end, bottom, inspectorInfo = debugInspectorInfo {
                                name = "border2"
                                properties["start"] = start
                                properties["top"] = top
                                properties["end"] = end
                                properties["bottom"] = bottom
                            }))

                fun Modifier.border2(values: Borders) =
                    border2(values.start, values.top, values.end, values.bottom)

                fun Modifier.border3(corner1: Location, corner2: Location) =
                    border2(corner1.x, corner1.y, corner2.x, corner2.y)

                private class BorderModifier(
                    val values: Borders,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                private class BorderModifier2(
                    val start: Int,
                    val top: Int,
                    val end: Int,
                    val bottom: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                data class Borders(val start: Int, val top: Int, val end: Int, val bottom: Int) {
                    constructor(all: Int) : this(all, all, all, all)
                }

                data class Location(val x: Int, val y: Int)
                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithConditional() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.padding(size: Int) =
                    this.then(
                        if (size >= 10) {
                            PaddingModifier(
                                paddingSize = size,
                                inspectorInfo = debugInspectorInfo {
                                    name = "padding"
                                    properties["size"] = size
                                }
                            )
                        } else {
                            Modifier
                        }
                    )

                fun Modifier.paddingFromBaseline(top: Int, bottom: Int) = this
                    .then(if (bottom > 0) padding(bottom) else Modifier)
                    .then(if (top > 0) padding(top) else Modifier)

                fun Modifier.paddingFromBaseline2(top: Int, bottom: Int) =
                    this.padding(bottom).padding(top)

                private class PaddingModifier(
                    paddingSize: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo) {
                }

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithWhen() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(painter: Painter) =
                    this.then(
                        when (painter.size) {
                            0 -> Modifier
                            1 -> BorderModifier(inspectorInfo = debugInspectorInfo {
                                    name = "border"
                                    properties["painter"] = painter
                                 })
                            else -> Modifier
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
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun existingInspectorInfoWithConditionals() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                class Brush

                class SolidColor(val color: Int): Brush()

                fun Modifier.border(width: Int, brush: Brush, shape: Shape): Modifier = composed(
                    factory = { BorderModifier(shape, width, brush) },
                    inspectorInfo = debugInspectorInfo {
                        name = "border"
                        properties["width"] = width
                        if (brush is SolidColor) {
                            properties["color"] = brush.value
                            value = brush.value
                        } else {
                            properties["brush"] = brush
                        }
                        properties["shape"] = shape
                    }
                )

                fun Modifier.border2(width: Int, color: Int, shape: Shape): Modifier =
                    if (width > 0) {
                        composed(
                            inspectorInfo = debugInspectorInfo {
                                name = "border2"
                                properties["width"] = width
                                properties["color"] = color
                                properties["shape"] = shape
                            }
                        ) {
                            border(width, SolidColor(color), shape)
                        }
                    } else {
                        this
                    }

                private class BorderModifier(shape: Shape, width: Int, brush: Brush)

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun composedModifierWithInspectorInfo() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(width: Int): Modifier = composed(
                    inspectorInfo = debugInspectorInfo {
                        name = "border"
                        properties["width"] = width
                    },
                    factory = { this.then(BorderModifier(width)) }
                )

                fun Modifier.border2(width: Int): Modifier = composed(
                    factory = { this.then(BorderModifier(width)) },
                    inspectorInfo = debugInspectorInfo {
                        name = "border2"
                        properties["width"] = width
                    }
                )

                private class BorderModifier(private val width: Int): Modifier.Element {
                }

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun rememberModifierInfo() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            Stubs.Remember,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int) = this.then(
                    remember {
                        SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                            name = "width1"
                            properties["width"] = width
                        })
                    }
                )

                private class SizeModifier1(
                    val width: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun emptyModifier() {
        lint().files(
            Stubs.Modifier,
            Stubs.Remember,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier

                internal actual fun Modifier.width1(width: Int): Modifier = this
                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun acceptMissingInspectorInfoInSamples() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui.demos.whatever

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width2(width: Int) = this.then(SizeModifier2(width))

                private data class SizeModifier2(
                    val width: Int,
                ): Modifier.Element

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun missingInspectorInfo() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
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
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier2.kt:8: Error: Modifier missing inspectorInfo [ModifierInspectorInfo]
                    fun Modifier.preferredWidth2(width: Int) = this.then(SizeModifier2(width))
                                                                         ~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun composedModifierWithMissingInspectorInfo() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.border(width: Int): Modifier =
                    composed { this.then(BorderModifier(width)) }

                private class BorderModifier(private val width: Int): Modifier.Element {
                }

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/androidx/compose/ui/BorderModifier.kt:9: Error: Modifier missing inspectorInfo [ModifierInspectorInfo]
                        composed { this.then(BorderModifier(width)) }
                        ~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun missingInspectorInfoFromInnerClassImplementation() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
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
                fun Modifier.size(width: Int) =
                    this.then(SizeModifier.WithOption(width))

                internal sealed class SizeModifier : Modifier.Element {
                    internal data class WithOption(val width: Int) : SizeModifier() {
                    }
                }

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier.kt:12: Error: Modifier missing inspectorInfo [ModifierInspectorInfo]
                        this.then(SizeModifier.WithOption(width))
                                               ~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithWrongName() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int) =
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
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:10: Error: Expected name of the modifier: "name" = "width1" [ModifierInspectorInfo]
                            name = "otherName"
                                    ~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithWrongValue() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
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
            .testModes(TestMode.DEFAULT)
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
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
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
            .testModes(TestMode.DEFAULT)
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
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
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
            .testModes(TestMode.DEFAULT)
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
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
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
            .testModes(TestMode.DEFAULT)
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
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo

                fun Modifier.width1(width: Int, height: Int) =
                    this.then(SizeModifier1(width, height, inspectorInfo = {
                        name = "width1"
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
            .testModes(TestMode.DEFAULT)
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
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.width1(width: Int) =
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
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/androidx/compose/ui/SizeModifier1.kt:9: Error: Expected name of the modifier: "name" = "width1" [ModifierInspectorInfo]
                        this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                                                                                          ^
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun inspectorInfoWithMissingVariables() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
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
            .testModes(TestMode.DEFAULT)
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
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
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
            .testModes(TestMode.DEFAULT)
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

    @Test
    fun missingInfoInConditionals() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                class Brush

                class SolidColor(val color: Int): Brush()

                fun Modifier.border(width: Int): Modifier = composed(
                    inspectorInfo = debugInspectorInfo {
                        name = "border"
                        value = width
                    }
                ) {
                    BorderModifier(shape, width, brush)
                }

                fun Modifier.border2(width: Int): Modifier =
                    if (width > 0) {
                        border(width)
                    } else {
                        composed { BorderModifier(shape, width, brush) }
                    }

                fun Modifier.border3(width: Int): Modifier =
                    when {
                        width < 0 -> this
                        width < 2 -> border(width)
                        width < 3 -> composed { BorderModifier(shape, width, brush) }
                        else -> this
                    }

                fun Modifier.border4(width: Int): Modifier =
                    when {
                        width < 0 -> this
                        width < 2 -> border(width)
                        else -> this.then(BorderModifier(shape, width, brush))
                    }

                private class BorderModifier(shape: Shape, width: Int, brush: Brush): Modifier

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/androidx/compose/ui/Brush.kt:25: Error: Modifier missing inspectorInfo [ModifierInspectorInfo]
                            composed { BorderModifier(shape, width, brush) }
                            ~~~~~~~~
                    src/androidx/compose/ui/Brush.kt:32: Error: Modifier missing inspectorInfo [ModifierInspectorInfo]
                            width < 3 -> composed { BorderModifier(shape, width, brush) }
                                         ~~~~~~~~
                    src/androidx/compose/ui/Brush.kt:40: Error: Modifier missing inspectorInfo [ModifierInspectorInfo]
                            else -> this.then(BorderModifier(shape, width, brush))
                                              ~~~~~~~~~~~~~~
                    3 errors, 0 warnings
                """
            )
    }

    @Test
    fun testInspectable() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package mypackage

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.inspectable
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.background(color: Int): Modifier = this.then(
                    Background(color, inspectorInfo = debugInspectorInfo {
                        name = "background"
                        value = color
                    })
                )

                fun Modifier.border(width: Int, color: Int): Modifier = this.then(
                    BorderModifier(width, color, inspectorInfo = debugInspectorInfo {
                        name = "border"
                        properties["width"] = width
                        properties["color"] = color
                    })
                )

                fun Modifier.frame(color: Int) = this.then(
                    Modifier.inspectable(
                        inspectorInfo = debugInspectorInfo {
                            name = "frame"
                            value = color
                        },
                        wrapped = Modifier.background(color).border(width = 5, color = color)
                    )
                )

                private class BackgroundModifier(
                    color: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier

                private class BorderModifier(
                    width: Int,
                    color: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }

    @Test
    fun testInspectableWithMissingParameter() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package mypackage

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.inspectable
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                fun Modifier.background(color: Int): Modifier = this.then(
                    Background(color, inspectorInfo = debugInspectorInfo {
                        name = "background"
                        value = color
                    })
                )

                fun Modifier.border(width: Int, color: Int): Modifier = this.then(
                    BorderModifier(width, color, inspectorInfo = debugInspectorInfo {
                        name = "border"
                        properties["width"] = width
                        properties["color"] = color
                    })
                )

                fun Modifier.frame(color: Int) = this.then(
                    Modifier.inspectable(
                        inspectorInfo = debugInspectorInfo {
                            name = "frame"
                        },
                        wrapped = Modifier.background(color).border(width = 5, color = color)
                    )
                )

                private class BackgroundModifier(
                    color: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier

                private class BorderModifier(
                    width: Int,
                    color: Int,
                    inspectorInfo: InspectorInfo.() -> Unit
                ): Modifier

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expect(
                """
                    src/mypackage/BackgroundModifier.kt:26: Error: These lambda arguments are missing in the InspectorInfo: color [ModifierInspectorInfo]
                            inspectorInfo = debugInspectorInfo {
                                                               ^
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun passInspectorInfoAtSecondLastParameter() {
        lint().files(
            Stubs.Modifier,
            composedStub,
            inspectableInfoStub,
            kotlin(
                """
                package androidx.compose.ui

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.platform.InspectorValueInfo
                import androidx.compose.ui.platform.debugInspectorInfo

                inline class Dp(val value: Float)

                fun Modifier.width1(width: Dp, height: Dp) =
                    this.then(SizeModifier1(width, inspectorInfo = debugInspectorInfo {
                        name = "width1"
                        properties["width"] = width
                        properties["height"] = height
                    }, height))

                private class SizeModifier1(
                    val width: Dp,
                    inspectorInfo: InspectorInfo.() -> Unit,
                    val height: Dp
                ): Modifier.Element, InspectorValueInfo(inspectorInfo)

                """
            ).indented()
        )
            .testModes(TestMode.DEFAULT)
            .run()
            .expectClean()
    }
}
