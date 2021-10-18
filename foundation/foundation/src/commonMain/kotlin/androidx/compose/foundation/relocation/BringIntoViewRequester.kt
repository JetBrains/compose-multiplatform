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

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder.Companion.RootBringIntoViewResponder
import androidx.compose.foundation.relocation.BringIntoViewResponder.Companion.ModifierLocalBringIntoViewResponder
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.toSize

/**
 * Can be used to send [bringIntoView] requests. Pass it as a parameter to
 * [Modifier.bringIntoView()][bringIntoView].
 *
 * For instance, you can call [BringIntoViewRequester.bringIntoView][bringIntoView] to
 * make all the scrollable parents scroll so that the specified item is brought into parent
 * bounds. This sample demonstrates this use case:
 *
 * Here is a sample where a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * Here is a sample where a part of a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 */
@ExperimentalFoundationApi
sealed interface BringIntoViewRequester {
    /**
     * Bring this item into bounds by making all the scrollable parents scroll appropriately.
     *
     * @param rect The rectangle (In local coordinates) that should be brought into view. If you
     * don't specify the coordinates, the coordinates of the
     * [Modifier.bringIntoViewRequester()][bringIntoViewRequester] associated with this
     * [BringIntoViewRequester] will be used.
     *
     * Here is a sample where a composable is brought into view:
     * @sample androidx.compose.foundation.samples.BringIntoViewSample
     *
     * Here is a sample where a part of a composable is brought into view:
     * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
     */
    suspend fun bringIntoView(rect: Rect? = null)
}

/**
 * Create an instance of [BringIntoViewRequester] that can be used with
 * [Modifier.bringIntoViewRequester][bringIntoViewRequester]. A child can then call
 * [BringIntoViewRequester.bringIntoView] to send a request any scrollable parents so that they
 * scroll to bring this item into view.
 *
 * Here is a sample where a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * Here is a sample where a part of a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 */
@ExperimentalFoundationApi
fun BringIntoViewRequester(): BringIntoViewRequester {
    return BringIntoViewRequesterImpl()
}

/**
 * This is a modifier that can be used to send bringIntoView requests.
 *
 * Here is an example where the a [bringIntoViewRequester] can be used to bring an item into parent
 * bounds. It demonstrates how a composable can ask its parents to scroll so that the component
 * using this modifier is brought into the bounds of all its parents.
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * @param bringIntoViewRequester an instance of [BringIntoViewRequester]. This hoisted object can be
 * used to send bringIntoView requests to parents of the current composable.
 */
@ExperimentalFoundationApi
fun Modifier.bringIntoViewRequester(
    bringIntoViewRequester: BringIntoViewRequester
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "bringIntoViewRequester"
        properties["bringIntoViewRequester"] = bringIntoViewRequester
    }
) {
    val bringIntoViewData = remember { BringIntoViewData(BringRectangleOnScreenRequester()) }
    if (bringIntoViewRequester is BringIntoViewRequesterImpl) {
        DisposableEffect(bringIntoViewRequester) {
            bringIntoViewRequester.bringIntoViewUsages += bringIntoViewData
            onDispose { bringIntoViewRequester.bringIntoViewUsages -= bringIntoViewData }
        }
    }

    Modifier
        .bringRectangleOnScreenRequester(bringIntoViewData.bringRectangleOnScreenRequester)
        .onGloballyPositioned { bringIntoViewData.layoutCoordinates = it }
        .then(
            remember {
                object : ModifierLocalConsumer {
                    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
                        bringIntoViewData.parent = scope.run {
                            ModifierLocalBringIntoViewResponder.current
                        }
                    }
                }
            }
        )
}

@ExperimentalFoundationApi
private class BringIntoViewRequesterImpl : BringIntoViewRequester {
    val bringIntoViewUsages: MutableVector<BringIntoViewData> = mutableVectorOf()

    override suspend fun bringIntoView(rect: Rect?) {
        bringIntoViewUsages.forEach {
            val layoutCoordinates = it.layoutCoordinates
            if (layoutCoordinates == null || !layoutCoordinates.isAttached) return@forEach

            // If the rect is not specified, use a rectangle representing the entire composable.
            val sourceRect = rect ?: layoutCoordinates.size.toSize().toRect()

            // Convert the rect into parent coordinates.
            val rectInParentCoordinates = it.parent.toLocalRect(sourceRect, layoutCoordinates)

            if (it.parent == RootBringIntoViewResponder) {
                // Use the platform specific API to bring the rectangle on screen.
                it.bringRectangleOnScreenRequester.bringRectangleOnScreen(rectInParentCoordinates)
            } else {
                it.parent.bringIntoView(rectInParentCoordinates)
            }
        }
    }
}

@ExperimentalFoundationApi
private data class BringIntoViewData(
    val bringRectangleOnScreenRequester: BringRectangleOnScreenRequester,
    var parent: BringIntoViewResponder = RootBringIntoViewResponder,
    var layoutCoordinates: LayoutCoordinates? = null
)
