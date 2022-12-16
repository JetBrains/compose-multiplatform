/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.tooling

import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.unit.IntRect

@OptIn(UiToolingDataApi::class)
private fun List<ViewInfo>.filterTree(
    filter: (ViewInfo) -> Boolean = { true }
): List<ViewInfo> =
        flatMap {
            val acceptedNodes =
                it.children.filterTree(filter).flatMap { child ->
                    if (child.location == null)
                        child.children
                    else listOf(child)
                }

            if (filter(it)) {
                listOf(ViewInfo(
                    it.fileName,
                    it.lineNumber,
                    it.bounds,
                    it.location,
                    acceptedNodes,
                    it.layoutInfo
                ))
            } else {
                // Create a fake node to attach the children to
                listOf(ViewInfo(
                    "<root>",
                    -1,
                    IntRect.Zero,
                    null,
                    acceptedNodes,
                    null
                ))
            }
        }

@OptIn(UiToolingDataApi::class)
internal fun List<ViewInfo>.toDebugString(
    indentation: Int = 0,
    filter: (ViewInfo) -> Boolean = { true }
): String {
    val indentString = ".".repeat(indentation)
    val builder = StringBuilder()

    filterTree(filter)
        .sortedWith(compareBy({ it.fileName }, { it.lineNumber }, { it.allChildren().size }))
        .forEach {
            if (it.location != null)
                builder.appendLine("$indentString|${it.fileName}:${it.lineNumber}")
            else
                builder.appendLine("$indentString|<root>")

            val childrenString = it.children
                .toDebugString(indentation + 1, filter).trim()
            if (childrenString.isNotEmpty()) builder.appendLine(childrenString)
        }

    return builder.toString()
}