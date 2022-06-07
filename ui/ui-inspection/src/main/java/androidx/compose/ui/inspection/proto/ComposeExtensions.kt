/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.inspection.proto

import android.view.inspector.WindowInspector
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.inspection.ComposeLayoutInspector.CacheTree
import androidx.compose.ui.inspection.LambdaLocation
import androidx.compose.ui.inspection.RecompositionHandler
import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.NodeParameter
import androidx.compose.ui.inspection.inspector.NodeParameterReference
import androidx.compose.ui.inspection.inspector.ParameterKind
import androidx.compose.ui.inspection.inspector.ParameterType
import androidx.compose.ui.inspection.inspector.systemPackages
import androidx.compose.ui.unit.IntOffset
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Bounds
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableRoot
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.LambdaValue
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ParameterReference
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Quad
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Rect

fun InspectorNode.toComposableNode(
    stringTable: StringTable,
    windowPos: IntOffset,
    recompositionHandler: RecompositionHandler
): ComposableNode {
    return toComposableNodeImpl(stringTable, windowPos, recompositionHandler).resetSystemFlag()
        .build()
}

private val SELECTOR_EXPR = Regex("(\\\$(lambda-)?[0-9]+)+$")

private fun InspectorNode.toComposableNodeImpl(
    stringTable: StringTable,
    windowPos: IntOffset,
    recompositionHandler: RecompositionHandler
): ComposableNode.Builder {
    val inspectorNode = this
    return ComposableNode.newBuilder().apply {
        id = inspectorNode.id

        packageHash = inspectorNode.packageHash
        filename = stringTable.put(inspectorNode.fileName)
        lineNumber = inspectorNode.lineNumber
        offset = inspectorNode.offset

        name = stringTable.put(inspectorNode.name)

        bounds = Bounds.newBuilder().apply {
            layout = Rect.newBuilder().apply {
                x = inspectorNode.left + windowPos.x
                y = inspectorNode.top + windowPos.y
                w = inspectorNode.width
                h = inspectorNode.height
            }.build()
            if (inspectorNode.bounds != null) {
                render = Quad.newBuilder().apply {
                    x0 = inspectorNode.bounds.x0
                    y0 = inspectorNode.bounds.y0
                    x1 = inspectorNode.bounds.x1
                    y1 = inspectorNode.bounds.y1
                    x2 = inspectorNode.bounds.x2
                    y2 = inspectorNode.bounds.y2
                    x3 = inspectorNode.bounds.x3
                    y3 = inspectorNode.bounds.y3
                }.build()
            }
        }.build()

        flags = flags()
        viewId = inspectorNode.viewId
        recompositionHandler.getCounts(inspectorNode.key, inspectorNode.anchorHash)?.let {
            recomposeCount = it.count
            recomposeSkips = it.skips
        }

        children.forEach { child ->
            addChildren(child.toComposableNodeImpl(stringTable, windowPos, recompositionHandler))
        }

        anchorHash = inspectorNode.anchorHash
    }
}

private fun InspectorNode.flags(): Int {
    var flags = 0
    if (packageHash in systemPackages) {
        flags = flags or ComposableNode.Flags.SYSTEM_CREATED_VALUE
    }
    if (mergedSemantics.isNotEmpty()) {
        flags = flags or ComposableNode.Flags.HAS_MERGED_SEMANTICS_VALUE
    }
    if (unmergedSemantics.isNotEmpty()) {
        flags = flags or ComposableNode.Flags.HAS_UNMERGED_SEMANTICS_VALUE
    }
    return flags
}

private fun ComposableNode.Builder.resetSystemFlag(): ComposableNode.Builder =
    apply { flags = flags and ComposableNode.Flags.SYSTEM_CREATED_VALUE.inv() }

fun ParameterType.convert(): Parameter.Type {
    return when (this) {
        ParameterType.String -> Parameter.Type.STRING
        ParameterType.Boolean -> Parameter.Type.BOOLEAN
        ParameterType.Double -> Parameter.Type.DOUBLE
        ParameterType.Float -> Parameter.Type.FLOAT
        ParameterType.Int32 -> Parameter.Type.INT32
        ParameterType.Int64 -> Parameter.Type.INT64
        ParameterType.Color -> Parameter.Type.COLOR
        ParameterType.Resource -> Parameter.Type.RESOURCE
        ParameterType.DimensionDp -> Parameter.Type.DIMENSION_DP
        ParameterType.DimensionSp -> Parameter.Type.DIMENSION_SP
        ParameterType.DimensionEm -> Parameter.Type.DIMENSION_EM
        ParameterType.Lambda -> Parameter.Type.LAMBDA
        ParameterType.FunctionReference -> Parameter.Type.FUNCTION_REFERENCE
        ParameterType.Iterable -> Parameter.Type.ITERABLE
    }
}

fun ParameterKind.convert(): ParameterReference.Kind {
    return when (this) {
        ParameterKind.Normal -> ParameterReference.Kind.NORMAL
        ParameterKind.MergedSemantics -> ParameterReference.Kind.MERGED_SEMANTICS
        ParameterKind.UnmergedSemantics -> ParameterReference.Kind.UNMERGED_SEMANTICS
    }
}

fun ParameterReference.Kind.convert(): ParameterKind {
    return when (this) {
        ParameterReference.Kind.NORMAL -> ParameterKind.Normal
        ParameterReference.Kind.MERGED_SEMANTICS -> ParameterKind.MergedSemantics
        ParameterReference.Kind.UNMERGED_SEMANTICS -> ParameterKind.UnmergedSemantics
        else -> ParameterKind.Normal
    }
}

private fun Parameter.Builder.setValue(stringTable: StringTable, value: Any?) {
    when (type) {
        Parameter.Type.ITERABLE,
        Parameter.Type.STRING -> {
            int32Value = stringTable.put(value as String)
        }
        Parameter.Type.BOOLEAN -> {
            int32Value = if (value as Boolean) 1 else 0
        }
        Parameter.Type.DOUBLE -> {
            doubleValue = value as Double
        }
        Parameter.Type.FLOAT,
        Parameter.Type.DIMENSION_DP,
        Parameter.Type.DIMENSION_SP,
        Parameter.Type.DIMENSION_EM -> {
            floatValue = value as Float
        }
        Parameter.Type.INT32,
        Parameter.Type.COLOR -> {
            int32Value = value as Int
        }
        Parameter.Type.INT64 -> {
            int64Value = value as Long
        }
        Parameter.Type.RESOURCE -> setResourceType(value, stringTable)
        Parameter.Type.LAMBDA -> setFunctionType(value, stringTable)
        Parameter.Type.FUNCTION_REFERENCE -> setFunctionType(value, stringTable)
        else -> error("Unknown Composable parameter type: $type")
    }
}

private fun Parameter.Builder.setResourceType(value: Any?, stringTable: StringTable) {
    // A Resource is passed by resource id for Compose
    val resourceId = (value as? Int) ?: return
    resourceValue = WindowInspector.getGlobalWindowViews()
        .firstOrNull()
        ?.createResource(stringTable, resourceId)
        ?: return
}

private fun Parameter.Builder.setFunctionType(value: Any?, stringTable: StringTable) {
    if (value !is Array<*> || value.size > 2 || value.size == 0) {
        return
    }
    val lambdaInstance = value[0] ?: return
    val location = LambdaLocation.resolve(lambdaInstance) ?: return
    val lambdaClass = lambdaInstance::class.java
    val lambdaClassName = lambdaClass.name
    lambdaValue = LambdaValue.newBuilder().apply {
        packageName = stringTable.put(lambdaClassName.substringBeforeLast("."))
        functionName = if (value.size == 2 && value[1] != null && value[1] is String)
            stringTable.put(value[1] as String) else 0
        lambdaName = stringTable.put(findLambdaSelector(lambdaClassName))
        fileName = stringTable.put(location.fileName)
        startLineNumber = location.startLine
        endLineNumber = location.endLine
    }.build()
}

/**
 * Return the lambda selector from the [lambdaClassName].
 *
 * Example:
 * - className: com.example.composealertdialog.ComposableSingletons$MainActivityKt$lambda-10$1$2$2$1
 * - selector:  lambda-10$1$2$2$1
 */
@VisibleForTesting
fun findLambdaSelector(lambdaClassName: String): String =
    SELECTOR_EXPR.find(lambdaClassName)?.value?.substring(1) ?: ""

fun NodeParameter.convert(stringTable: StringTable): Parameter {
    val nodeParam = this
    return Parameter.newBuilder().apply {
        name = stringTable.put(nodeParam.name)
        type = nodeParam.type.convert()
        setValue(stringTable, nodeParam.value)
        index = nodeParam.index
        nodeParam.reference?.let { reference = it.convert() }
        if (nodeParam.elements.isNotEmpty()) {
            addAllElements(nodeParam.elements.map { it.convert(stringTable) })
        }
    }.build()
}

fun NodeParameterReference.convert(): ParameterReference {
    val reference = this
    return ParameterReference.newBuilder().apply {
        kind = reference.kind.convert()
        composableId = reference.nodeId
        anchorHash = reference.anchorHash
        parameterIndex = reference.parameterIndex
        addAllCompositeIndex(reference.indices.asIterable())
    }.build()
}

internal fun CacheTree.toComposableRoot(
    stringTable: StringTable,
    windowPos: IntOffset,
    recompositionHandler: RecompositionHandler
): ComposableRoot = ComposableRoot.newBuilder().also { root ->
    root.viewId = viewParent.uniqueDrawingId
    root.addAllNodes(nodes.map {
        it.toComposableNode(stringTable, windowPos, recompositionHandler)
    })
    root.addAllViewsToSkip(viewsToSkip)
}.build()

fun Iterable<NodeParameter>.convertAll(stringTable: StringTable): List<Parameter> {
    return this.map { it.convert(stringTable) }
}
