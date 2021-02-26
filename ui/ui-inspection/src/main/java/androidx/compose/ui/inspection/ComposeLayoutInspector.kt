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
import androidx.compose.ui.inspection.compose.convertParameters
import androidx.compose.ui.inspection.compose.flatten
import androidx.compose.ui.inspection.framework.flatten
import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.LayoutInspectorTree
import androidx.compose.ui.inspection.inspector.NodeParameterReference
import androidx.compose.ui.inspection.proto.StringTable
import androidx.compose.ui.inspection.proto.convert
import androidx.compose.ui.inspection.proto.convertAll
import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ParameterGroup
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response

private const val LAYOUT_INSPECTION_ID = "layoutinspector.compose.inspection"

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
        /** If the cached data includes system nodes or not */
        val systemComposablesSkipped: Boolean,
        /** The cached nodes themselves as a map from node id to InspectorNode */
        val nodes: Map<Long, InspectorNode>,
    )

    private val layoutInspectorTree = LayoutInspectorTree()

    // Sidestep threading concerns by only ever accessing cachedNodes on the inspector thread
    private val inspectorThread = Thread.currentThread()
    private val _cachedNodes = mutableMapOf<Long, CacheData>()
    private val cachedNodes: MutableMap<Long, CacheData>
        get() {
            check(Thread.currentThread() == inspectorThread)
            return _cachedNodes
        }

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        val command = Command.parseFrom(data)
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
            else -> error("Unexpected compose inspector command case: ${command.specializedCase}")
        }
    }

    private fun handleGetComposablesCommand(
        getComposablesCommand: GetComposablesCommand,
        callback: CommandCallback
    ) {
        ThreadUtils.runOnMainThread {
            val stringTable = StringTable()
            val rootIds = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }
            val composeViews = getAndroidComposeViews(
                getComposablesCommand.rootViewId,
                getComposablesCommand.skipSystemComposables
            )

            val composeRoots = composeViews.map { it.createComposableRoot(stringTable) }

            environment.executors().primary().execute {
                // As long as we're modifying cachedNodes anyway, remove any nodes associated with
                // layout roots that have since been removed.
                cachedNodes.keys.removeAll { rootId -> !rootIds.contains(rootId) }
                cachedNodes[getComposablesCommand.rootViewId] = CacheData(
                    getComposablesCommand.skipSystemComposables,
                    composeViews.toInspectorNodes().associateBy { it.id }
                )

                callback.reply {
                    getComposablesResponse = GetComposablesResponse.newBuilder().apply {
                        addAllStrings(stringTable.toStringEntries())
                        addAllRoots(composeRoots)
                    }.build()
                }
            }
        }
    }

    private fun handleGetParametersCommand(
        getParametersCommand: GetParametersCommand,
        callback: CommandCallback
    ) {
        val foundComposable =
            getComposableNodes(
                getParametersCommand.rootViewId,
                getParametersCommand.skipSystemComposables
            )[getParametersCommand.composableId]

        val rootId = getParametersCommand.rootViewId

        callback.reply {
            getParametersResponse = if (foundComposable != null) {
                val stringTable = StringTable()
                val parameters = foundComposable.convertParameters(layoutInspectorTree, rootId)
                    .convertAll(stringTable)
                GetParametersResponse.newBuilder().apply {
                    parameterGroup = ParameterGroup.newBuilder().apply {
                        composableId = getParametersCommand.composableId
                        addAllParameter(parameters)
                    }.build()
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
                getAllParametersCommand.skipSystemComposables
            ).values

        val rootId = getAllParametersCommand.rootViewId

        callback.reply {
            val stringTable = StringTable()
            val parameterGroups = allComposables.map { composable ->
                val parameters = composable.convertParameters(layoutInspectorTree, rootId)
                    .convertAll(stringTable)
                ParameterGroup.newBuilder().apply {
                    composableId = composable.id
                    addAllParameter(parameters)
                }.build()
            }

            getAllParametersResponse = GetAllParametersResponse.newBuilder().apply {
                rootViewId = rootId
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
            getParameterDetailsCommand.skipSystemComposables
        )
        val reference = NodeParameterReference(
            getParameterDetailsCommand.reference.composableId,
            getParameterDetailsCommand.reference.parameterIndex,
            getParameterDetailsCommand.reference.compositeIndexList
        )
        val expanded = composables[reference.nodeId]?.let { composable ->
            layoutInspectorTree.expandParameter(
                getParameterDetailsCommand.rootViewId,
                composable,
                reference,
                getParameterDetailsCommand.startIndex,
                getParameterDetailsCommand.maxElements
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
        skipSystemComposables: Boolean
    ): Map<Long, InspectorNode> {
        cachedNodes[rootViewId]?.let { cacheData ->
            if (cacheData.systemComposablesSkipped == skipSystemComposables) {
                return cacheData.nodes
            }
        }

        return ThreadUtils.runOnMainThread {
            getAndroidComposeViews(rootViewId, skipSystemComposables)
                .toInspectorNodes()
                .associateBy { it.id }
        }.get()
    }

    /**
     * Get all AndroidComposeView instances found within the layout tree rooted by [rootViewId].
     */
    private fun getAndroidComposeViews(
        rootViewId: Long,
        skipSystemComposables: Boolean
    ): List<AndroidComposeViewWrapper> {
        ThreadUtils.assertOnMainThread()

        layoutInspectorTree.resetGeneratedId()
        return WindowInspector.getGlobalWindowViews()
            .asSequence()
            .filter { root ->
                root.visibility == View.VISIBLE && root.isAttachedToWindow &&
                    root.uniqueDrawingId == rootViewId
            }
            .flatMap { it.flatten() }
            .mapNotNull { view ->
                AndroidComposeViewWrapper.tryCreateFor(
                    layoutInspectorTree,
                    view,
                    skipSystemComposables
                )
            }
            .toList()
    }
}

private fun Inspector.CommandCallback.reply(initResponse: Response.Builder.() -> Unit) {
    val response = Response.newBuilder()
    response.initResponse()
    reply(response.build().toByteArray())
}

/**
 * Convert an [AndroidComposeViewWrapper] to a flat list of all inspector nodes (including children)
 * that live underneath it.
 */
private fun List<AndroidComposeViewWrapper>.toInspectorNodes(): List<InspectorNode> {
    return this
        .flatMap { it.inspectorNodes }
        .flatMap { it.flatten() }
        .toList()
}
