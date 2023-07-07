/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("unused")

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun ModifierUsageSample() {
    Text(
        "Hello, World!",
        Modifier
            .padding(16.dp) // Outer padding; outside background
            .background(color = Color.Green) // Solid element background color
            .padding(16.dp) // Inner padding; inside background, around text
    )
}

@Sampled
@Composable
fun ModifierFactorySample() {
    class FancyModifier(val level: Float) : Modifier.Element

    fun Modifier.fancy(level: Float) = this.then(FancyModifier(level))

    Row(
        Modifier
            .fancy(1f)
            .padding(10.dp)) {
        // content
    }
}

@Sampled
@Composable
fun ModifierParameterSample() {
    @Composable
    fun PaddedColumn(modifier: Modifier = Modifier) {
        Column(modifier.padding(10.dp)) {
            // ...
        }
    }
}

@Sampled
@Composable
fun SubcomponentModifierSample() {
    @Composable
    fun ButtonBar(
        onOk: () -> Unit,
        onCancel: () -> Unit,
        modifier: Modifier = Modifier,
        buttonModifier: Modifier = Modifier
    ) {
        Row(modifier) {
            Button(onCancel, buttonModifier) {
                Text("Cancel")
            }
            Button(onOk, buttonModifier) {
                Text("Ok")
            }
        }
    }
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun DelegatedNodeSample() {
    class TapGestureNode(var onTap: () -> Unit) : PointerInputModifierNode, Modifier.Node() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize
        ) {
            // ...
        }

        override fun onCancelPointerInput() {
            // ...
        }
    }
    class TapGestureWithClickSemantics(onTap: () -> Unit) :
        PointerInputModifierNode, SemanticsModifierNode, DelegatingNode() {
        var onTap: () -> Unit
            get() = gesture.onTap
            set(value) { gesture.onTap = value }

        val gesture = delegated { TapGestureNode(onTap) }

        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize
        ) {
            gesture.onPointerEvent(pointerEvent, pass, bounds)
        }

        override fun onCancelPointerInput() {
            gesture.onCancelPointerInput()
        }

        override val semanticsConfiguration: SemanticsConfiguration = SemanticsConfiguration()
            .apply {
                onClick {
                    gesture.onTap()
                    true
                }
            }
    }
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun ModifierNodeElementSample() {
    class Circle(var color: Color) : DrawModifierNode, Modifier.Node() {
        override fun ContentDrawScope.draw() {
            drawCircle(color)
        }
    }
    data class CircleElement(
        val color: Color
    ) : ModifierNodeElement<Circle>() {
        override fun create() = Circle(color)
        override fun update(node: Circle): Circle {
            node.color = color
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "circle"
            properties["color"] = color
        }
    }
    fun Modifier.circle(color: Color) = this then CircleElement(color)
}

@Suppress("LocalVariableName")
@ExperimentalComposeUiApi
@Sampled
@Composable
fun SemanticsModifierNodeSample() {
    class HeadingNode : SemanticsModifierNode, Modifier.Node() {
        override val semanticsConfiguration: SemanticsConfiguration = SemanticsConfiguration()
            .apply {
                heading()
            }
    }

    val HeadingElement = object : ModifierNodeElement<HeadingNode>() {
        override fun create() = HeadingNode()

        override fun update(node: HeadingNode): HeadingNode {
            // Nothing to update.
            return node
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "heading"
        }

        override fun hashCode(): Int = "heading".hashCode()
        override fun equals(other: Any?) = (other === this)
    }

    fun Modifier.heading() = this then HeadingElement
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun PointerInputModifierNodeSample() {
    class OnPointerEventNode(var callback: (PointerEvent) -> Unit) :
        PointerInputModifierNode, Modifier.Node() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize
        ) {
            if (pass == PointerEventPass.Initial) {
                callback(pointerEvent)
            }
        }

        override fun onCancelPointerInput() {
            // Do nothing
        }
    }

    data class PointerInputElement(
        val callback: (PointerEvent) -> Unit
    ) : ModifierNodeElement<OnPointerEventNode>() {
        override fun create() = OnPointerEventNode(callback)
        override fun update(node: OnPointerEventNode): OnPointerEventNode {
            node.callback = callback
            return node
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "onPointerEvent"
            properties["callback"] = callback
        }
    }

    fun Modifier.onPointerEvent(callback: (PointerEvent) -> Unit) =
        this then PointerInputElement(callback)
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun LayoutAwareModifierNodeSample() {
    class SizeLoggerNode(var id: String) : LayoutAwareModifierNode, Modifier.Node() {
        override fun onRemeasured(size: IntSize) {
            println("The size of $id was $size")
        }
    }

    data class LogSizeElement(val id: String) : ModifierNodeElement<SizeLoggerNode>() {
        override fun create(): SizeLoggerNode = SizeLoggerNode(id)
        override fun update(node: SizeLoggerNode): SizeLoggerNode {
            node.id = id
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "logSize"
            properties["id"] = id
        }
    }

    fun Modifier.logSize(id: String) = this then LogSizeElement(id)
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun GlobalPositionAwareModifierNodeSample() {
    class PositionLoggerNode(var id: String) : GlobalPositionAwareModifierNode, Modifier.Node() {
        override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
            // This will be the size of the Layout.
            coordinates.size
            // The position of the Layout relative to the application window.
            coordinates.positionInWindow()
            // The position of the Layout relative to the Compose root.
            coordinates.positionInRoot()
            // These will be the alignment lines provided to the Layout
            coordinates.providedAlignmentLines
            // This will be a LayoutCoordinates instance corresponding to the parent of the Layout.
            coordinates.parentLayoutCoordinates
        }
    }

    data class PositionLoggerElement(val id: String) : ModifierNodeElement<PositionLoggerNode>() {
        override fun create() = PositionLoggerNode(id)
        override fun update(node: PositionLoggerNode): PositionLoggerNode {
            node.id = id
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "logPosition"
            properties["id"] = id
        }
    }

    fun Modifier.logPosition(id: String) = this then PositionLoggerElement(id)
}

@ExperimentalComposeUiApi
@Sampled
@Composable
fun JustReadingOrProvidingModifierLocalNodeSample() {
    class Logger { fun log(string: String) { println(string) } }

    val loggerLocal = modifierLocalOf<Logger> { Logger() }

    class ProvideLoggerNode(logger: Logger) : ModifierLocalNode, Modifier.Node() {
        override val providedValues = modifierLocalMapOf(loggerLocal to logger)
    }

    data class ProvideLoggerElement(
        val logger: Logger
    ) : ModifierNodeElement<ProvideLoggerNode>() {
        override fun create() = ProvideLoggerNode(logger)
        override fun update(node: ProvideLoggerNode): ProvideLoggerNode {
            node.provide(loggerLocal, logger)
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "provideLogger"
            properties["logger"] = logger
        }
    }

    class SizeLoggerNode(
        var id: String
    ) : ModifierLocalNode, LayoutAwareModifierNode, Modifier.Node() {
        override fun onRemeasured(size: IntSize) {
            loggerLocal.current.log("The size of $id was $size")
        }
    }

    data class SizeLoggerElement(
        val id: String
    ) : ModifierNodeElement<SizeLoggerNode>() {
        override fun create() = SizeLoggerNode(id)
        override fun update(node: SizeLoggerNode): SizeLoggerNode {
            node.id = id
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "logSize"
            properties["id"] = id
        }
    }

    fun Modifier.logSize(id: String) = this then SizeLoggerElement(id)
    fun Modifier.provideLogger(logger: Logger) = this then ProvideLoggerElement(logger)
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun ModifierNodeResetSample() {
    class SelectableNode : Modifier.Node() {
        var selected by mutableStateOf(false)

        override fun onReset() {
            // reset `selected` to the initial value as if the node will be reused for
            // displaying different content it shouldn't be selected straight away.
            selected = false
        }

        // some logic which sets `selected` to true when it is selected
    }
}
