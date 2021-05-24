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

package androidx.compose.ui.inspection

import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.ui.inspection.compose.AndroidComposeViewWrapper
import androidx.compose.ui.inspection.compose.convertToParameterGroup
import androidx.compose.ui.inspection.compose.flatten
import androidx.compose.ui.inspection.framework.flatten
import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.LayoutInspectorTree
import androidx.compose.ui.inspection.inspector.NodeParameterReference
import androidx.compose.ui.inspection.proto.StringTable
import androidx.compose.ui.inspection.proto.convert
import androidx.compose.ui.inspection.proto.toComposableNodes
import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.compose.ui.unit.IntOffset
import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableRoot
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.UnknownCommandResponse

private const val LAYOUT_INSPECTION_ID = "layoutinspector.compose.inspection"
private const val MAX_RECURSIONS = 2
private const val MAX_ITERABLE_SIZE = 5

// created by java.util.ServiceLoader
class ComposeLayoutInspectorFactory :
    InspectorFactory<ComposeLayoutInspector>(LAYOUT_INSPECTION_ID) {
    override fun createInspector(
        connection: Connection,
        environment: InspectorEnvironment
    ): ComposeLayoutInspector {
        return ComposeLayoutInspector(connection, environment)
    }
}

class ComposeLayoutInspector(
    connection: Connection,
    private val environment: InspectorEnvironment
) : Inspector(connection) {

    /** Cache data which allows us to reuse previously queried inspector nodes */
    private class CacheData(
        val rootView: View,
        val viewParent: View,
        val nodes: List<InspectorNode>,
        val viewsToSkip: List<Long>
    ) {
        /** The cached nodes as a map from node id to InspectorNode */
        val lookup = nodes.flatMap { it.flatten() }.associateBy { it.id }
    }

    private val layoutInspectorTree = LayoutInspectorTree()

    // Sidestep threading concerns by only ever accessing cachedNodes on the inspector thread
    private val inspectorThread = Thread.currentThread()
    private val _cachedNodes = mutableMapOf<Long, CacheData>()
    private var cachedGeneration = 0
    private var cachedSystemComposablesSkipped = false
    private val cachedNodes: MutableMap<Long, CacheData>
        get() {
            check(Thread.currentThread() == inspectorThread)
            return _cachedNodes
        }

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        val command = try {
            Command.parseFrom(data)
        } catch (ignored: InvalidProtocolBufferException) {
            handleUnknownCommand(data, callback)
            return
        }

        when (command.specializedCase) {
            Command.SpecializedCase.GET_COMPOSABLES_COMMAND -> {
                handleGetComposablesCommand(command.getComposablesCommand, callback)
            }
            Command.SpecializedCase.GET_PARAMETERS_COMMAND -> {
                handleGetParametersCommand(command.getParametersCommand, callback)
            }
            Command.SpecializedCase.GET_ALL_PARAMETERS_COMMAND -> {
                handleGetAllParametersCommand(command.getAllParametersCommand, callback)
            }
            Command.SpecializedCase.GET_PARAMETER_DETAILS_COMMAND -> {
                handleGetParameterDetailsCommand(command.getParameterDetailsCommand, callback)
            }
            else -> handleUnknownCommand(data, callback)
        }
    }

    private fun handleUnknownCommand(commandBytes: ByteArray, callback: CommandCallback) {
        callback.reply {
            unknownCommandResponse = UnknownCommandResponse.newBuilder().apply {
                this.commandBytes = ByteString.copyFrom(commandBytes)
            }.build()
        }
    }

    private fun handleGetComposablesCommand(
        getComposablesCommand: GetComposablesCommand,
        callback: CommandCallback
    ) {
        val data = getComposableNodes(
            getComposablesCommand.rootViewId,
            getComposablesCommand.skipSystemComposables,
            getComposablesCommand.generation,
            getComposablesCommand.generation == 0
        )

        val location = IntArray(2)
        data?.rootView?.getLocationOnScreen(location)
        val windowPos = IntOffset(location[0], location[1])

        val stringTable = StringTable()
        val nodes = data?.nodes ?: emptyList()
        val composeNodes = nodes.toComposableNodes(stringTable, windowPos)

        callback.reply {
            getComposablesResponse = GetComposablesResponse.newBuilder().apply {
                addAllStrings(stringTable.toStringEntries())
                addRoots(
                    ComposableRoot.newBuilder().apply {
                        if (data != null) {
                            viewId = data.viewParent.uniqueDrawingId
                            addAllNodes(composeNodes)
                            addAllViewsToSkip(data.viewsToSkip)
                        }
                    }
                )
            }.build()
        }
    }

    private fun handleGetParametersCommand(
        getParametersCommand: GetParametersCommand,
        callback: CommandCallback
    ) {
        val foundComposable =
            getComposableNodes(
                getParametersCommand.rootViewId,
                getParametersCommand.skipSystemComposables,
                getParametersCommand.generation
            )?.lookup?.get(getParametersCommand.composableId)

        callback.reply {
            getParametersResponse = if (foundComposable != null) {
                val stringTable = StringTable()
                GetParametersResponse.newBuilder().apply {
                    parameterGroup = foundComposable.convertToParameterGroup(
                        layoutInspectorTree,
                        getParametersCommand.rootViewId,
                        getParametersCommand.maxRecursions.orElse(MAX_RECURSIONS),
                        getParametersCommand.maxInitialIterableSize.orElse(MAX_ITERABLE_SIZE),
                        stringTable
                    )
                    addAllStrings(stringTable.toStringEntries())
                }.build()
            } else {
                GetParametersResponse.getDefaultInstance()
            }
        }
    }

    private fun handleGetAllParametersCommand(
        getAllParametersCommand: GetAllParametersCommand,
        callback: CommandCallback
    ) {
        val allComposables =
            getComposableNodes(
                getAllParametersCommand.rootViewId,
                getAllParametersCommand.skipSystemComposables,
                getAllParametersCommand.generation
            )?.lookup?.values ?: emptyList()

        callback.reply {
            val stringTable = StringTable()
            val parameterGroups = allComposables.map { composable ->
                composable.convertToParameterGroup(
                    layoutInspectorTree,
                    getAllParametersCommand.rootViewId,
                    getAllParametersCommand.maxRecursions.orElse(MAX_RECURSIONS),
                    getAllParametersCommand.maxInitialIterableSize.orElse(MAX_ITERABLE_SIZE),
                    stringTable
                )
            }

            getAllParametersResponse = GetAllParametersResponse.newBuilder().apply {
                rootViewId = getAllParametersCommand.rootViewId
                addAllParameterGroups(parameterGroups)
                addAllStrings(stringTable.toStringEntries())
            }.build()
        }
    }

    private fun handleGetParameterDetailsCommand(
        getParameterDetailsCommand: GetParameterDetailsCommand,
        callback: CommandCallback
    ) {
        val composables = getComposableNodes(
            getParameterDetailsCommand.rootViewId,
            getParameterDetailsCommand.skipSystemComposables,
            getParameterDetailsCommand.generation
        )?.lookup ?: emptyMap()
        val reference = NodeParameterReference(
            getParameterDetailsCommand.reference.composableId,
            getParameterDetailsCommand.reference.kind.convert(),
            getParameterDetailsCommand.reference.parameterIndex,
            getParameterDetailsCommand.reference.compositeIndexList
        )
        val expanded = composables[reference.nodeId]?.let { composable ->
            layoutInspectorTree.expandParameter(
                getParameterDetailsCommand.rootViewId,
                composable,
                reference,
                getParameterDetailsCommand.startIndex,
                getParameterDetailsCommand.maxElements,
                getParameterDetailsCommand.maxRecursions.orElse(MAX_RECURSIONS),
                getParameterDetailsCommand.maxInitialIterableSize.orElse(MAX_ITERABLE_SIZE),
            )
        }

        callback.reply {
            getParameterDetailsResponse = if (expanded != null) {
                val stringTable = StringTable()
                GetParameterDetailsResponse.newBuilder().apply {
                    rootViewId = getParameterDetailsCommand.rootViewId
                    parameter = expanded.convert(stringTable)
                    addAllStrings(stringTable.toStringEntries())
                }.build()
            } else {
                GetParameterDetailsResponse.getDefaultInstance()
            }
        }
    }

    /**
     * Get all [InspectorNode]s found under the layout tree rooted by [rootViewId]. They will be
     * mapped with their ID as the key.
     *
     * This will return cached data if possible, but may request new data otherwise, blocking the
     * current thread potentially.
     */
    private fun getComposableNodes(
        rootViewId: Long,
        skipSystemComposables: Boolean,
        generation: Int,
        forceRegeneration: Boolean = false
    ): CacheData? {
        if (!forceRegeneration && generation == cachedGeneration &&
            skipSystemComposables == cachedSystemComposablesSkipped
        ) {
            return cachedNodes[rootViewId]
        }

        val data = ThreadUtils.runOnMainThread {
            layoutInspectorTree.resetAccumulativeState()
            val data = getAndroidComposeViews(rootViewId, skipSystemComposables, generation).map {
                CacheData(it.rootView, it.viewParent, it.createNodes(), it.viewsToSkip)
            }
            layoutInspectorTree.resetAccumulativeState()
            data
        }.get()

        cachedNodes.clear()
        data.associateByTo(cachedNodes) { it.rootView.uniqueDrawingId }
        cachedGeneration = generation
        cachedSystemComposablesSkipped = skipSystemComposables
        return cachedNodes[rootViewId]
    }

    /**
     * Get all AndroidComposeView instances found within the layout tree rooted by [rootViewId].
     */
    private fun getAndroidComposeViews(
        rootViewId: Long,
        skipSystemComposables: Boolean,
        generation: Int
    ): List<AndroidComposeViewWrapper> {
        ThreadUtils.assertOnMainThread()

        val roots = WindowInspector.getGlobalWindowViews()
            .asSequence()
            .filter { root ->
                root.visibility == View.VISIBLE && root.isAttachedToWindow &&
                    (generation > 0 || root.uniqueDrawingId == rootViewId)
            }

        val wrappers = mutableListOf<AndroidComposeViewWrapper>()
        roots.forEach { root ->
            root.flatten().mapNotNullTo(wrappers) { view ->
                AndroidComposeViewWrapper.tryCreateFor(
                    layoutInspectorTree,
                    root,
                    view,
                    skipSystemComposables
                )
            }
        }
        return wrappers
    }
}

private fun Inspector.CommandCallback.reply(initResponse: Response.Builder.() -> Unit) {
    val response = Response.newBuilder()
    response.initResponse()
    reply(response.build().toByteArray())
}

// Provide default for older version:
private fun Int.orElse(defaultValue: Int): Int =
    if (this == 0) defaultValue else this
