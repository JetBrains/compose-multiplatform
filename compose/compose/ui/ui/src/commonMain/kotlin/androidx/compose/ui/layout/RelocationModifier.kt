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

package androidx.compose.ui.layout

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A [modifier][Modifier.Element] that can be used to respond to relocation requests to relocate
 * an item on screen.
 *
 * When a child calls [RelocationRequester.bringIntoView](), the framework calls
 * [computeDestination] where you can take the source bounds and compute the destination
 * rectangle for the child. Relocation Modifiers higher up the hierarchy will receive this
 * destination as their source rect. Finally after all relocation modifiers have a chance to
 * compute their destinations, the framework calls [performRelocation](source, destination)
 * which performs the actual relocation (scrolling).
 *
 * @see RelocationRequester
 */
@Suppress("unused", "DeprecatedCallableAddReplaceWith")
@ExperimentalComposeUiApi
@Deprecated(
    message = "Please use BringIntoViewResponder instead.",
    level = DeprecationLevel.ERROR
)
@JvmDefaultWithCompatibility
interface RelocationModifier : Modifier.Element {
    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param source The bounding box of the item that sent the request to be brought into view.
     * @param layoutCoordinates The layoutCoordinates associated with this modifier.
     * @return the destination rectangle.
     */
    fun computeDestination(source: Rect, layoutCoordinates: LayoutCoordinates): Rect

    /**
     * Using the source and destination bounds, perform a relocation operation that moves the
     * source rect to the destination location. (This is usually achieved by scrolling).
     */
    suspend fun performRelocation(source: Rect, destination: Rect)
}

/**
 * Add this modifier to respond to requests to bring an item into view.
 */
@Suppress("UNUSED_PARAMETER", "unused", "DeprecatedCallableAddReplaceWith")
@ExperimentalComposeUiApi
@Deprecated(
    message = "Please use BringIntoViewResponder instead.",
    level = DeprecationLevel.ERROR
)
fun Modifier.onRelocationRequest(
    /**
     * Provide the destination given the source rectangle and current bounds.
     *
     * rect: The bounding box of the item that sent the request to be brought into view.
     * layoutCoordinates: The layoutCoordinates associated with this modifier.
     * @return the destination rectangle.
     */
    onProvideDestination: (rect: Rect, layoutCoordinates: LayoutCoordinates) -> Rect,

    /**
     * Using the source and destination bounds, perform a relocation operation that moves the
     * source rect to the destination location. (This is usually achieved by scrolling).
     */
    onPerformRelocation: suspend (sourceRect: Rect, destinationRect: Rect) -> Unit
): Modifier = this
