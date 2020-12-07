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

package androidx.compose.ui.tooling.inspector

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.ModalDrawerLayout
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.resetSourceInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.Group
import androidx.compose.ui.tooling.Inspectable
import androidx.compose.ui.tooling.R
import androidx.compose.ui.tooling.SlotTableRecord
import androidx.compose.ui.tooling.ToolingTest
import androidx.compose.ui.tooling.asTree
import androidx.compose.ui.tooling.position
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

private const val DEBUG = false

@LargeTest
@RunWith(AndroidJUnit4::class)
class LayoutInspectorTreeTest : ToolingTest() {
    private lateinit var density: Density
    private lateinit var view: View

    @Before
    fun density() {
        @OptIn(InternalComposeApi::class)
        resetSourceInfo()
        density = Density(activity)
        view = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
    }

    @Ignore("Manual test")
    @Test
    fun buildTree() {
        val slotTableRecord = SlotTableRecord.create()

        show {
            Inspectable(slotTableRecord) {
                Column {
                    Text(text = "Hello World", color = Color.Green)
                    Surface {
                        Button(onClick = {}) { Text(text = "OK") }
                    }
                }
            }
        }

        // TODO: Find out if we can set "settings put global debug_view_attributes 1" in tests
        view.setTag(R.id.inspection_slot_table_set, slotTableRecord.store)
        val viewWidth = with(density) { view.width.toDp() }
        val viewHeight = with(density) { view.height.toDp() }
        dumpSlotTableSet(slotTableRecord)
        val builder = LayoutInspectorTree()
        val nodes = builder.convert(view)
        dumpNodes(nodes, builder)

        validate(nodes, builder, checkParameters = true) {
            node(
                name = "Box",
                fileName = "Box.kt",
                left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
                children = listOf("Column")
            ) {
                parameter(name = "shape", type = ParameterType.String, value = "Shape")
                parameter(name = "backgroundColor", type = ParameterType.Color, value = 0x0)
                parameter(name = "padding", type = ParameterType.DimensionDp, value = 0.0f)
                parameter(
                    name = "paddingStart",
                    type = ParameterType.DimensionDp,
                    value = Float.NaN
                )
                parameter(name = "paddingTop", type = ParameterType.DimensionDp, value = Float.NaN)
                parameter(name = "paddingEnd", type = ParameterType.DimensionDp, value = Float.NaN)
                parameter(
                    name = "paddingBottom",
                    type = ParameterType.DimensionDp,
                    value = Float.NaN
                )
                parameter(name = "gravity", type = ParameterType.String, value = "TopStart")
            }

            node(
                name = "Column",
                fileName = "Box.kt",
                left = 0.0.dp, top = 0.0.dp, width = viewWidth, height = viewHeight,
                children = listOf("Column")
            )
            node(
                name = "Column",
                fileName = "LayoutInspectorTreeTest.kt",
                left = 0.0.dp,
                top = 0.0.dp,
                width = 70.5.dp,
                height = 54.9.dp,
                children = listOf("Text", "Surface")
            ) {
                parameter(name = "verticalArrangement", type = ParameterType.String, value = "Top")
                parameter(name = "horizontalGravity", type = ParameterType.String, value = "Start")
            }
            node(
                name = "Text",
                fileName = "LayoutInspectorTreeTest.kt",
                left = 0.0.dp,
                top = 0.0.dp, width = 70.5.dp, height = 18.9.dp, children = listOf("Text")
            ) {
                parameter(name = "text", type = ParameterType.String, value = "Hello World")
                parameter(name = "color", type = ParameterType.Color, value = 0xff00ff00.toInt())
                parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                parameter(name = "overflow", type = ParameterType.String, value = "Clip")
                parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
                parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            }
            node(
                name = "Text",
                fileName = "Text.kt",
                left = 0.0.dp,
                top = 0.0.dp, width = 70.5.dp, height = 18.9.dp, children = listOf("CoreText")
            ) {
                parameter(name = "text", type = ParameterType.String, value = "Hello World")
                parameter(name = "color", type = ParameterType.Color, value = 0xff00ff00.toInt())
                parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                parameter(name = "overflow", type = ParameterType.String, value = "Clip")
                parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
                parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            }
            node(
                name = "CoreText",
                fileName = "CoreText.kt",
                isRenderNode = true,
                left = 0.0.dp, top = 0.0.dp, width = 70.5.dp, height = 18.9.dp
            ) {
                parameter(name = "text", type = ParameterType.String, value = "Hello World")
                parameter(name = "style", type = ParameterType.String, value = "TextStyle") {
                    parameter(
                        name = "color",
                        type = ParameterType.Color,
                        value = 0xff00ff00.toInt()
                    )
                    parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                    parameter(
                        name = "letterSpacing",
                        type = ParameterType.String,
                        value = "Inherit"
                    )
                    parameter(name = "background", type = ParameterType.String, value = "Unset")
                    parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                }
                parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
                parameter(name = "overflow", type = ParameterType.String, value = "Clip")
                parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            }
            node(
                name = "Surface",
                fileName = "LayoutInspectorTreeTest.kt",
                isRenderNode = true,
                left = 0.0.dp,
                top = 18.9.dp, width = 64.0.dp, height = 36.0.dp, children = listOf("Button")
            ) {
                parameter(name = "shape", type = ParameterType.String, value = "Shape")
                parameter(name = "color", type = ParameterType.Color, value = 0xffffffff.toInt())
                parameter(
                    name = "contentColor",
                    type = ParameterType.Color,
                    value = 0xff000000.toInt()
                )
                parameter(name = "elevation", type = ParameterType.DimensionDp, value = 0.0f)
            }
            node(
                name = "Button",
                fileName = "LayoutInspectorTreeTest.kt",
                left = 0.0.dp,
                top = 18.9.dp, width = 64.0.dp, height = 36.0.dp, children = listOf("Surface")
            ) {
                parameter(name = "enabled", type = ParameterType.Boolean, value = true)
                parameter(name = "elevation", type = ParameterType.DimensionDp, value = 2.0f)
                parameter(
                    name = "disabledElevation",
                    type = ParameterType.DimensionDp,
                    value = 0.0f
                )
                parameter(
                    name = "shape",
                    type = ParameterType.String,
                    value = "RoundedCornerShape"
                ) {
                    parameter(name = "topLeft", type = ParameterType.DimensionDp, value = 4.0f)
                    parameter(name = "topRight", type = ParameterType.DimensionDp, value = 4.0f)
                    parameter(name = "bottomLeft", type = ParameterType.DimensionDp, value = 4.0f)
                    parameter(name = "bottomRight", type = ParameterType.DimensionDp, value = 4.0f)
                }
                parameter(
                    name = "backgroundColor", type = ParameterType.Color,
                    value = 0xff6200ee.toInt()
                )
                parameter(
                    name = "disabledBackgroundColor", type = ParameterType.Color,
                    value = 0xffe0e0e0.toInt()
                )
                parameter(
                    name = "contentColor",
                    type = ParameterType.Color,
                    value = 0xffffffff.toInt()
                )
                parameter(
                    name = "disabledContentColor",
                    type = ParameterType.Color,
                    value = 0x61000000
                )
                parameter(name = "padding", type = ParameterType.String, value = "PaddingValues") {
                    parameter(name = "start", type = ParameterType.DimensionDp, value = 16.0f)
                    parameter(name = "end", type = ParameterType.DimensionDp, value = 16.0f)
                    parameter(name = "top", type = ParameterType.DimensionDp, value = 8.0f)
                    parameter(name = "bottom", type = ParameterType.DimensionDp, value = 8.0f)
                }
            }
            node(
                name = "Surface",
                fileName = "Button.kt",
                isRenderNode = true,
                left = 0.0.dp,
                top = 18.9.dp,
                width = 64.0.dp,
                height = 36.0.dp,
                children = listOf("ProvideTextStyle")
            ) {
                parameter(
                    name = "shape",
                    type = ParameterType.String,
                    value = "RoundedCornerShape"
                ) {
                    parameter(name = "topLeft", type = ParameterType.DimensionDp, value = 4.0f)
                    parameter(name = "topRight", type = ParameterType.DimensionDp, value = 4.0f)
                    parameter(name = "bottomLeft", type = ParameterType.DimensionDp, value = 4.0f)
                    parameter(name = "bottomRight", type = ParameterType.DimensionDp, value = 4.0f)
                }
                parameter(name = "color", type = ParameterType.Color, value = 0xff6200ee.toInt())
                parameter(
                    name = "contentColor",
                    type = ParameterType.Color,
                    value = 0xffffffff.toInt()
                )
                parameter(name = "elevation", type = ParameterType.DimensionDp, value = 2.0f)
            }
            node(
                name = "ProvideTextStyle",
                fileName = "Button.kt",
                left = 16.0.dp,
                top = 26.9.dp, width = 32.0.dp, height = 20.0.dp, children = listOf("Row")
            ) {
                parameter(name = "value", type = ParameterType.String, value = "TextStyle") {
                    parameter(name = "color", type = ParameterType.String, value = "Unset")
                    parameter(name = "fontSize", type = ParameterType.DimensionSp, value = 14.0f)
                    parameter(name = "fontWeight", type = ParameterType.String, value = "Medium")
                    parameter(name = "fontFamily", type = ParameterType.String, value = "Default")
                    parameter(
                        name = "letterSpacing",
                        type = ParameterType.DimensionSp,
                        value = 1.25f
                    )
                    parameter(name = "background", type = ParameterType.String, value = "Unset")
                    parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                }
            }
            node(
                name = "Row",
                fileName = "Button.kt",
                left = 16.0.dp,
                top = 26.9.dp, width = 32.0.dp, height = 20.0.dp, children = listOf("Text")
            ) {
                parameter(
                    name = "horizontalArrangement",
                    type = ParameterType.String,
                    value = "Center"
                )
                parameter(
                    name = "verticalGravity", type = ParameterType.String,
                    value = "CenterVertically"
                )
            }
            node(
                name = "Text",
                fileName = "LayoutInspectorTreeTest.kt",
                left = 21.8.dp,
                top = 27.6.dp, width = 20.4.dp, height = 18.9.dp, children = listOf("Text")
            ) {
                parameter(name = "text", type = ParameterType.String, value = "OK")
                parameter(name = "color", type = ParameterType.String, value = "Unset")
                parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                parameter(name = "overflow", type = ParameterType.String, value = "Clip")
                parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
                parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            }
            node(
                name = "Text",
                fileName = "Text.kt",
                left = 21.8.dp,
                top = 27.6.dp, width = 20.4.dp, height = 18.9.dp, children = listOf("CoreText")
            ) {
                parameter(name = "text", type = ParameterType.String, value = "OK")
                parameter(name = "color", type = ParameterType.String, value = "Unset")
                parameter(name = "fontSize", type = ParameterType.String, value = "Inherit")
                parameter(name = "letterSpacing", type = ParameterType.String, value = "Inherit")
                parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                parameter(name = "overflow", type = ParameterType.String, value = "Clip")
                parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
                parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            }
            node(
                name = "CoreText",
                fileName = "CoreText.kt",
                isRenderNode = true,
                left = 21.8.dp, top = 27.6.dp, width = 20.4.dp, height = 18.9.dp
            ) {
                parameter(name = "text", type = ParameterType.String, value = "OK")
                parameter(name = "style", type = ParameterType.String, value = "TextStyle") {
                    parameter(
                        name = "color",
                        type = ParameterType.Color,
                        value = 0xffffffff.toInt()
                    )
                    parameter(name = "fontSize", type = ParameterType.DimensionSp, value = 14.0f)
                    parameter(name = "fontWeight", type = ParameterType.String, value = "Medium")
                    parameter(name = "fontFamily", type = ParameterType.String, value = "Default")
                    parameter(
                        name = "letterSpacing",
                        type = ParameterType.DimensionSp,
                        value = 1.25f
                    )
                    parameter(name = "background", type = ParameterType.String, value = "Unset")
                    parameter(name = "lineHeight", type = ParameterType.String, value = "Inherit")
                }
                parameter(name = "softWrap", type = ParameterType.Boolean, value = true)
                parameter(name = "overflow", type = ParameterType.String, value = "Clip")
                parameter(name = "maxLines", type = ParameterType.Int32, value = 2147483647)
            }
        }
    }

    @Test
    fun testStitchTreeFromModelDrawerLayout() {
        val slotTableRecord = SlotTableRecord.create()

        show {
            Inspectable(slotTableRecord) {
                ModalDrawerLayout(
                    drawerContent = { Text("Something") },
                    bodyContent = {
                        Column {
                            Text(text = "Hello World", color = Color.Green)
                            Button(onClick = {}) { Text(text = "OK") }
                        }
                    }
                )
            }
        }
        view.setTag(R.id.inspection_slot_table_set, slotTableRecord.store)
        dumpSlotTableSet(slotTableRecord)
        val builder = LayoutInspectorTree()
        val nodes = builder.convert(view)
        dumpNodes(nodes, builder)

        if (DEBUG) {
            validate(nodes, builder, checkParameters = false) {
                node("Box", children = listOf("ModalDrawerLayout"))
                node("ModalDrawerLayout", children = listOf("WithConstraints"))
                node("WithConstraints", children = listOf("SubcomposeLayout"))
                node("SubcomposeLayout", children = listOf("Box"))
                node("Box", children = listOf("Box", "Canvas", "Surface"))
                node("Box", children = listOf("Column"))
                node("Column", children = listOf("Text", "Button"))
                node("Text", children = listOf("Text"))
                node("Text", children = listOf("CoreText"))
                node("CoreText", children = listOf())
                node("Button", children = listOf("Surface"))
                node("Surface", children = listOf("ProvideTextStyle"))
                node("ProvideTextStyle", children = listOf("Row"))
                node("Row", children = listOf("Text"))
                node("Text", children = listOf("Text"))
                node("Text", children = listOf("CoreText"))
                node("CoreText", children = listOf())
                node("Canvas", children = listOf("Spacer"))
                node("Spacer", children = listOf())
                node("Surface", children = listOf("Column"))
                node("Column", children = listOf("Text"))
                node("Text", children = listOf("Text"))
                node("Text", children = listOf("CoreText"))
                node("CoreText", children = listOf())
            }
        }
        assertThat(nodes.size).isEqualTo(1)
    }

    @Test
    fun testSpacer() {
        val slotTableRecord = SlotTableRecord.create()

        show {
            Inspectable(slotTableRecord) {
                Column {
                    Text(text = "Hello World", color = Color.Green)
                    Spacer(Modifier.preferredHeight(16.dp))
                    Image(Icons.Filled.Call)
                }
            }
        }

        view.setTag(R.id.inspection_slot_table_set, slotTableRecord.store)
        val builder = LayoutInspectorTree()
        val node = builder.convert(view)
            .flatMap { flatten(it) }
            .firstOrNull { it.name == "Spacer" }

        // Spacer should show up in the Compose tree:
        assertThat(node).isNotNull()
    }

    @Test // regression test b/174855322
    fun testBasicText() {
        val slotTableRecord = SlotTableRecord.create()

        view.setTag(R.id.inspection_slot_table_set, slotTableRecord.store)
        show {
            Column {
                BasicText(
                    text = "Some text",
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }
        }

        val builder = LayoutInspectorTree()
        val node = builder.convert(view)
            .flatMap { flatten(it) }
            .firstOrNull { it.name == "BasicText" }

        assertThat(node).isNotNull()

        assertThat(node?.parameters).isNotEmpty()
    }

    @SdkSuppress(minSdkVersion = 29) // Render id is not returned for api < 29:  b/171519437
    @Test
    @Ignore("b/174152464")
    fun testTextId() {
        val slotTableRecord = SlotTableRecord.create()

        show {
            Inspectable(slotTableRecord) {
                Text(text = "Hello World")
            }
        }

        view.setTag(R.id.inspection_slot_table_set, slotTableRecord.store)
        val builder = LayoutInspectorTree()
        val node = builder.convert(view)
            .flatMap { flatten(it) }
            .firstOrNull { it.name == "CoreText" }

        // LayoutNode id should be captured by the CoreText node:
        assertThat(node?.id).isGreaterThan(0)
    }

    private fun validate(
        result: List<InspectorNode>,
        builder: LayoutInspectorTree,
        checkParameters: Boolean,
        block: TreeValidationReceiver.() -> Unit = {}
    ) {
        val nodes = result.flatMap { flatten(it) }.iterator()
        val tree = TreeValidationReceiver(nodes, density, checkParameters, builder)
        tree.block()
    }

    private class TreeValidationReceiver(
        val nodeIterator: Iterator<InspectorNode>,
        val density: Density,
        val checkParameters: Boolean,
        val builder: LayoutInspectorTree
    ) {
        fun node(
            name: String,
            fileName: String? = null,
            lineNumber: Int = -1,
            isRenderNode: Boolean? = null,
            left: Dp = Dp.Unspecified,
            top: Dp = Dp.Unspecified,
            width: Dp = Dp.Unspecified,
            height: Dp = Dp.Unspecified,
            children: List<String> = listOf(),
            block: ParameterValidationReceiver.() -> Unit = {}
        ) {
            assertWithMessage("No such node found: $name").that(nodeIterator.hasNext()).isTrue()
            val node = nodeIterator.next()
            assertThat(node.name).isEqualTo(name)
            assertThat(node.children.map { it.name }).containsExactlyElementsIn(children).inOrder()
            fileName?.let { assertThat(node.fileName).isEqualTo(fileName) }
            if (lineNumber != -1) {
                assertThat(node.lineNumber).isEqualTo(lineNumber)
            }
            if (isRenderNode != null) {
                if (isRenderNode) {
                    assertThat(node.id).isGreaterThan(0L)
                } else {
                    assertThat(node.id).isLessThan(0L)
                }
            }
            if (left != Dp.Unspecified) {
                with(density) {
                    assertThat(node.left.toDp().value).isWithin(2.0f).of(left.value)
                    assertThat(node.top.toDp().value).isWithin(2.0f).of(top.value)
                    assertThat(node.width.toDp().value).isWithin(2.0f).of(width.value)
                    assertThat(node.height.toDp().value).isWithin(2.0f).of(height.value)
                }
            }

            if (checkParameters) {
                val params = builder.convertParameters(node)
                val receiver = ParameterValidationReceiver(params.listIterator())
                receiver.block()
                if (receiver.parameterIterator.hasNext()) {
                    val elementNames = mutableListOf<String>()
                    receiver.parameterIterator.forEachRemaining { elementNames.add(it.name) }
                    error("$name: has more parameters like: ${elementNames.joinToString()}")
                }
            }
        }
    }

    private fun flatten(node: InspectorNode): List<InspectorNode> =
        listOf(node).plus(node.children.flatMap { flatten(it) })

    // region DEBUG print methods
    private fun dumpNodes(nodes: List<InspectorNode>, builder: LayoutInspectorTree) {
        @Suppress("ConstantConditionIf")
        if (!DEBUG) {
            return
        }
        println()
        println("=================== Nodes ==========================")
        nodes.forEach { dumpNode(it, indent = 0) }
        println()
        println("=================== validate statements ==========================")
        nodes.forEach { generateValidate(it, builder) }
    }

    private fun dumpNode(node: InspectorNode, indent: Int) {
        println(
            "\"${"  ".repeat(indent * 2)}\", \"${node.name}\", \"${node.fileName}\", " +
                "${node.lineNumber}, ${node.left}, ${node.top}, " +
                "${node.width}, ${node.height}"
        )
        node.children.forEach { dumpNode(it, indent + 1) }
    }

    private fun generateValidate(node: InspectorNode, builder: LayoutInspectorTree) {
        with(density) {
            val left = round(node.left.toDp())
            val top = round(node.top.toDp())
            val width = if (node.width == view.width) "viewWidth" else round(node.width.toDp())
            val height = if (node.height == view.height) "viewHeight" else round(node.height.toDp())

            print(
                """
                  validate(
                      name = "${node.name}",
                      fileName = "${node.fileName}",
                      left = $left, top = $top, width = $width, height = $height
                """.trimIndent()
            )
        }
        if (node.id > 0L) {
            println(",")
            print("    isRenderNode = true")
        }
        if (node.children.isNotEmpty()) {
            println(",")
            val children = node.children.joinToString { "\"${it.name}\"" }
            print("    children = listOf($children)")
        }
        println()
        print(")")
        if (node.parameters.isNotEmpty()) {
            generateParameters(builder.convertParameters(node), 0)
        }
        println()
        node.children.forEach { generateValidate(it, builder) }
    }

    private fun generateParameters(parameters: List<NodeParameter>, indent: Int) {
        val indentation = " ".repeat(indent * 2)
        println(" {")
        for (param in parameters) {
            val name = param.name
            val type = param.type
            val value = toDisplayValue(type, param.value)
            print("$indentation  parameter(name = \"$name\", type = $type, value = $value)")
            if (param.elements.isNotEmpty()) {
                generateParameters(param.elements, indent + 1)
            }
            println()
        }
        print("$indentation}")
    }

    private fun toDisplayValue(type: ParameterType, value: Any?): String =
        when (type) {
            ParameterType.Boolean -> value.toString()
            ParameterType.Color ->
                "0x${Integer.toHexString(value as Int)}${if (value < 0) ".toInt()" else ""}"
            ParameterType.DimensionSp,
            ParameterType.DimensionDp -> "${value}f"
            ParameterType.Int32 -> value.toString()
            ParameterType.String -> "\"$value\""
            else -> value?.toString() ?: "null"
        }

    private fun dumpSlotTableSet(slotTableRecord: SlotTableRecord) {
        @Suppress("ConstantConditionIf")
        if (!DEBUG) {
            return
        }
        println()
        println("=================== Groups ==========================")
        slotTableRecord.store.forEach { dumpGroup(it.asTree(), indent = 0) }
    }

    private fun dumpGroup(group: Group, indent: Int) {
        val position = group.position?.let { "\"$it\"" } ?: "null"
        val box = group.box
        val id = group.modifierInfo.mapNotNull { (it.extra as? OwnedLayer)?.layerId }
            .singleOrNull() ?: 0
        println(
            "\"${"  ".repeat(indent)}\", ${group.javaClass.simpleName}, \"${group.name}\", " +
                "params: ${group.parameters.size}, children: ${group.children.size}, " +
                "$id, $position, " +
                "${box.left}, ${box.right}, ${box.right - box.left}, ${box.bottom - box.top}"
        )
        for (parameter in group.parameters) {
            println("\"${"  ".repeat(indent + 4)}\"- ${parameter.name}")
        }
        group.children.forEach { dumpGroup(it, indent + 1) }
    }

    private fun round(dp: Dp): Dp = Dp((dp.value * 10.0f).roundToInt() / 10.0f)

    //endregion
}
