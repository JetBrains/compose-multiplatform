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

package androidx.compose.ui.inspection.util

import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ParameterReference
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.UpdateSettingsCommand

fun List<LayoutInspectorComposeProtocol.StringEntry>.toMap() = associate { it.id to it.str }

fun GetParametersCommand(
    rootViewId: Long,
    composableId: Long,
    skipSystemComposables: Boolean = true
): Command = Command.newBuilder().apply {
    getParametersCommand = GetParametersCommand.newBuilder().apply {
        this.rootViewId = rootViewId
        this.composableId = composableId
        this.skipSystemComposables = skipSystemComposables
    }.build()
}.build()

fun GetParametersByAnchorHashCommand(
    rootViewId: Long,
    anchorHash: Int,
    skipSystemComposables: Boolean = true
): Command = Command.newBuilder().apply {
    getParametersCommand = GetParametersCommand.newBuilder().apply {
        this.rootViewId = rootViewId
        this.anchorHash = anchorHash
        this.skipSystemComposables = skipSystemComposables
    }.build()
}.build()

fun GetAllParametersCommand(
    rootViewId: Long,
    skipSystemComposables: Boolean = true
): Command = Command.newBuilder().apply {
    getAllParametersCommand = GetAllParametersCommand.newBuilder().apply {
        this.rootViewId = rootViewId
        this.skipSystemComposables = skipSystemComposables
    }.build()
}.build()

fun GetParameterDetailsCommand(
    rootViewId: Long,
    reference: ParameterReference,
    startIndex: Int,
    maxElements: Int,
    skipSystemComposables: Boolean = true
): Command = Command.newBuilder().apply {
    getParameterDetailsCommand = GetParameterDetailsCommand.newBuilder().apply {
        this.rootViewId = rootViewId
        this.skipSystemComposables = skipSystemComposables
        this.reference = reference
        if (startIndex >= 0) {
            this.startIndex = startIndex
        }
        if (maxElements >= 0) {
            this.maxElements = maxElements
        }
    }.build()
}.build()

fun GetComposablesCommand(
    rootViewId: Long,
    skipSystemComposables: Boolean = true,
    generation: Int = 1,
    extractAllParameters: Boolean = false
): Command =
    Command.newBuilder().apply {
        getComposablesCommand = GetComposablesCommand.newBuilder().apply {
            this.rootViewId = rootViewId
            this.skipSystemComposables = skipSystemComposables
            this.generation = generation
            this.extractAllParameters = extractAllParameters
        }
            .setRootViewId(rootViewId)
            .setSkipSystemComposables(skipSystemComposables)
            .setGeneration(generation)
            .build()
    }.build()

fun GetUpdateSettingsCommand(
    includeRecomposeCounts: Boolean = false,
    keepRecomposeCounts: Boolean = false,
    delayParameterExtractions: Boolean = false
): Command =
    Command.newBuilder().apply {
        updateSettingsCommand = UpdateSettingsCommand.newBuilder().apply {
            this.includeRecomposeCounts = includeRecomposeCounts
            this.keepRecomposeCounts = keepRecomposeCounts
            this.delayParameterExtractions = delayParameterExtractions
        }.build()
    }.build()

fun ComposableNode.flatten(): List<ComposableNode> =
    listOf(this).plus(this.childrenList.flatMap { it.flatten() })
