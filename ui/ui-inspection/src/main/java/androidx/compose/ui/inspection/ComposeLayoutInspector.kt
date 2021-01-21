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
import androidx.compose.ui.inspection.proto.StringTable
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
            else -> error("Unexpected compose inspector command case: ${command.specializedCase}")
        }
    }

    private fun handleGetComposablesCommand(
        getComposablesCommand: GetComposablesCommand,
        callback: CommandCallback
    ) {
        ThreadUtils.runOnMainThread {
            val stringTable = StringTable()
            val composeRoots =
                getComposableRoots(
                    getComposablesCommand.rootViewId,
                    getComposablesCommand.skipSystemComposables
                )
                    .map { it.createComposableRoot(stringTable) }
                    .toList()

            environment.executors().primary().execute {
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
        ThreadUtils.runOnMainThread {
            val foundComposable = getComposableRoots(
                getParametersCommand.rootViewId,
                getParametersCommand.skipSystemComposables
            )
                .flatMap { it.inspectorNodes }
                .flatMap { it.flatten() }
                .firstOrNull { it.id == getParametersCommand.composableId }

            environment.executors().primary().execute {
                callback.reply {
                    getParametersResponse = if (foundComposable != null) {
                        val stringTable = StringTable()
                        val parameters = foundComposable.convertParameters().convertAll(stringTable)
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
        }
    }

    private fun handleGetAllParametersCommand(
        getAllParametersCommand: GetAllParametersCommand,
        callback: CommandCallback
    ) {
        ThreadUtils.runOnMainThread {

            val allComposables = getComposableRoots(
                getAllParametersCommand.rootViewId,
                getAllParametersCommand.skipSystemComposables
            )
                .flatMap { it.inspectorNodes }
                .flatMap { it.flatten() }
                .toList()

            environment.executors().primary().execute {
                callback.reply {
                    val stringTable = StringTable()
                    val parameterGroups = allComposables.map { composable ->
                        val parameters = composable.convertParameters().convertAll(stringTable)
                        ParameterGroup.newBuilder().apply {
                            composableId = composable.id
                            addAllParameter(parameters)
                        }.build()
                    }

                    getAllParametersResponse = GetAllParametersResponse.newBuilder().apply {
                        rootViewId = getAllParametersCommand.rootViewId
                        addAllParameterGroups(parameterGroups)
                        addAllStrings(stringTable.toStringEntries())
                    }.build()
                }
            }
        }
    }
}

private fun getComposableRoots(
    rootViewId: Long,
    skipSystemComposables: Boolean
): Sequence<AndroidComposeViewWrapper> {
    return WindowInspector.getGlobalWindowViews()
        .asSequence()
        .filter { view -> view.visibility == View.VISIBLE && view.isAttachedToWindow }
        // Note: When querying root views, there should only be 0 or 1 match here, but it's
        // easier to handle this as a general filter, to avoid ? operators all the rest of
        // the way down
        .filter { it.uniqueDrawingId == rootViewId }
        .flatMap { it.flatten() }
        .mapNotNull {
            AndroidComposeViewWrapper.tryCreateFor(
                it,
                skipSystemComposables
            )
        }
}

private fun Inspector.CommandCallback.reply(initResponse: Response.Builder.() -> Unit) {
    val response = Response.newBuilder()
    response.initResponse()
    reply(response.build().toByteArray())
}
