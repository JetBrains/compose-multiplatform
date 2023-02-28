/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

/* ktlint-disable max-line-length */
class ModifierNodeInspectablePropertiesDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierNodeInspectablePropertiesDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(
        ModifierNodeInspectablePropertiesDetector.ModifierNodeInspectableProperties
    )

    @Test
    fun testNodeElementWithNoInspectableValues_flagsError() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectableValue
                import androidx.compose.ui.node.ModifierNodeElement

                class Element : ModifierNodeElement<Modifier.Node>() {
                    override fun create() = object : Modifier.Node() {}
                    override fun update(node: Modifier.Node) = node
                }
                """
            ),
            ModifierStub, InspectableValueStub, ModifierNodeElementStub
        )
            .run()
            .expect(
                """
src/test/Element.kt:8: Information: Element does not override inspectableProperties(). The layout inspector will use the default implementation of this function, which will attempt to read Element's properties reflectively. Override inspectableProperties() if you'd like to customize this modifier's presentation in the layout inspector. [ModifierNodeInspectableProperties]
                class Element : ModifierNodeElement<Modifier.Node>() {
                      ~~~~~~~
0 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun testNodeElementWithAlmostInspectableValues_flagsError() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectableValue
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.node.ModifierNodeElement

                class ElementWithExtraParam : ModifierNodeElement<Modifier.Node>() {
                    override fun create() = object : Modifier.Node() {}
                    override fun update(node: Modifier.Node) = node
                    // Doesn't override the base declaration
                    fun InspectorInfo.inspectableProperties(count: Int) {
                        name = "element"
                        properties["count"] = count
                    }
                }
                """
            ),
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectableValue
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.node.ModifierNodeElement

                class ElementWithoutReceiver : ModifierNodeElement<Modifier.Node>() {
                    override fun create() = object : Modifier.Node() {}
                    override fun update(node: Modifier.Node) = node
                    // Doesn't override the base declaration
                    fun inspectableProperties() { }
                }
                """
            ),
            ModifierStub, InspectableValueStub, ModifierNodeElementStub
        )
            .run()
            .expect(
                """
src/test/ElementWithExtraParam.kt:9: Information: ElementWithExtraParam does not override inspectableProperties(). The layout inspector will use the default implementation of this function, which will attempt to read ElementWithExtraParam's properties reflectively. Override inspectableProperties() if you'd like to customize this modifier's presentation in the layout inspector. [ModifierNodeInspectableProperties]
                class ElementWithExtraParam : ModifierNodeElement<Modifier.Node>() {
                      ~~~~~~~~~~~~~~~~~~~~~
src/test/ElementWithoutReceiver.kt:9: Information: ElementWithoutReceiver does not override inspectableProperties(). The layout inspector will use the default implementation of this function, which will attempt to read ElementWithoutReceiver's properties reflectively. Override inspectableProperties() if you'd like to customize this modifier's presentation in the layout inspector. [ModifierNodeInspectableProperties]
                class ElementWithoutReceiver : ModifierNodeElement<Modifier.Node>() {
                      ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun testNodeElementWithInspectableValues_doesNotFlagError() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectableValue
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.node.ModifierNodeElement

                class Element : ModifierNodeElement<Modifier.Node>() {
                    override fun create() = object : Modifier.Node() {}
                    override fun update(node: Modifier.Node) = node
                    override fun InspectorInfo.inspectableProperties() {
                        name = "element"
                        properties["count"] = count
                    }
                }
                """
            ),
            ModifierStub, InspectableValueStub, ModifierNodeElementStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun testNodeElementWithInheritedInspectableValues_doesNotFlagError() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.platform.InspectableValue
                import androidx.compose.ui.platform.InspectorInfo
                import androidx.compose.ui.node.ModifierNodeElement

                open class Element : ModifierNodeElement<Modifier.Node>() {
                    override fun create() = object : Modifier.Node() {}
                    override fun update(node: Modifier.Node) = node
                    override fun InspectorInfo.inspectableProperties() {
                        name = "element"
                        properties["count"] = count
                    }
                }

                class InheritingElement : Element() {
                    override fun create() = object : Modifier.Node() {}
                    override fun update(node: Modifier.Node) = node
                }
                """
            ),
            ModifierStub, InspectableValueStub, ModifierNodeElementStub
        )
            .run()
            .expectClean()
    }

    companion object {
        private val ModifierStub = kotlin(
            """
            package androidx.compose.ui

            interface Modifier {
                interface Node
                interface Element
            }
            """
        )

        private val InspectableValueStub = kotlin(
            """
            package androidx.compose.ui.platform

            import androidx.compose.ui.Modifier

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
            """
        )

        private val ModifierNodeElementStub = kotlin(
            """
            package androidx.compose.ui.node

            abstract class ModifierNodeElement<N : Modifier.Node> : Modifier.Element {

                abstract fun create(): N

                abstract fun update(node: N): N

                open fun InspectorInfo.inspectableProperties() {
                    // Reflective implementation omitted from the stubs.
                }
            }
            """
        )
    }
}
/* ktlint-enable max-line-length */