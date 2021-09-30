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

package androidx.compose.foundation.gestures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

@Composable
internal actual fun rememberOverScrollController(): OverScrollController {
    return remember {
        DesktopEdgeEffectOverScrollController()
    }
}

internal actual fun Modifier.overScroll(
    overScrollController: OverScrollController
): Modifier = Modifier

private class DesktopEdgeEffectOverScrollController() : OverScrollController {

    override fun release() {
    }

    override fun refreshContainerInfo(size: Size, isContentScrolls: Boolean) {
        // nothing yet
    }

    override fun DrawScope.drawOverScroll() {}

    override fun stopOverscrollAnimation(): Boolean = false

    override fun consumePreScroll(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset = Offset.Zero

    override fun consumePostScroll(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ) {
    }

    override fun consumePreFling(
        velocity: Velocity
    ): Velocity = Velocity.Zero

    override fun consumePostFling(velocity: Velocity) {
    }
}