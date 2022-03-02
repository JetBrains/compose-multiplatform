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

package androidx.compose.ui.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Represents a component that handles scroll events, so that other components in the hierarchy
 * can adjust their behaviour.
 * @See [provideScrollContainerInfo] and [consumeScrollContainerInfo]
 */
interface ScrollContainerInfo {
    /** @return whether this component handles horizontal scroll events */
    fun canScrollHorizontally(): Boolean
    /** @return whether this component handles vertical scroll events */
    fun canScrollVertically(): Boolean
}

/** @return whether this container handles either horizontal or vertical scroll events */
fun ScrollContainerInfo.canScroll() = canScrollVertically() || canScrollHorizontally()

/**
 * A modifier to query whether there are any parents in the hierarchy that handle scroll events.
 * The [ScrollContainerInfo] provided in [consumer] will recursively look for ancestors if the
 * nearest parent does not handle scroll events in the queried direction.
 * This can be used to delay UI changes in cases where a pointer event may later become a scroll,
 * cancelling any existing press or other gesture.
 *
 * @sample androidx.compose.ui.samples.ScrollableContainerSample
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.consumeScrollContainerInfo(consumer: (ScrollContainerInfo?) -> Unit): Modifier =
    modifierLocalConsumer {
        consumer(ModifierLocalScrollContainerInfo.current)
    }

/**
 * A Modifier that indicates that this component handles scroll events. Use
 * [consumeScrollContainerInfo] to query whether there is a parent in the hierarchy that is
 * a [ScrollContainerInfo].
 */
fun Modifier.provideScrollContainerInfo(scrollContainerInfo: ScrollContainerInfo): Modifier =
    composed(
        inspectorInfo = debugInspectorInfo {
            name = "provideScrollContainerInfo"
            value = scrollContainerInfo
        }) {
    remember(scrollContainerInfo) {
        ScrollContainerInfoModifierLocal(scrollContainerInfo)
    }
}

/**
 * ModifierLocal to propagate ScrollableContainer throughout the hierarchy.
 * This Modifier will recursively check for ancestor ScrollableContainers,
 * if the current ScrollableContainer does not handle scroll events in a particular direction.
 */
private class ScrollContainerInfoModifierLocal(
    private val scrollContainerInfo: ScrollContainerInfo,
) : ScrollContainerInfo, ModifierLocalProvider<ScrollContainerInfo?>, ModifierLocalConsumer {

    private var parent: ScrollContainerInfo? by mutableStateOf(null)

    override val key: ProvidableModifierLocal<ScrollContainerInfo?> =
        ModifierLocalScrollContainerInfo
    override val value: ScrollContainerInfoModifierLocal = this

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        parent = ModifierLocalScrollContainerInfo.current
    }

    override fun canScrollHorizontally(): Boolean {
        return scrollContainerInfo.canScrollHorizontally() ||
            parent?.canScrollHorizontally() == true
    }

    override fun canScrollVertically(): Boolean {
        return scrollContainerInfo.canScrollVertically() || parent?.canScrollVertically() == true
    }
}

internal val ModifierLocalScrollContainerInfo = modifierLocalOf<ScrollContainerInfo?> {
    null
}