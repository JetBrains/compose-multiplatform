/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwnersAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit = emptyContent()
) {
    PopupLayout {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .tapGestureFilter {
                    if (isFocusable) {
                        onDismissRequest?.invoke()
                    }
                }
                .padding(start = offset.x.dp, top = offset.y.dp),
            alignment = alignment
        ) {
            Box(
                modifier = Modifier.tapGestureFilter {}
            ) {
                children()
            }
        }
    }
}

@Composable
private fun PopupLayout(children: @Composable () -> Unit) {
    val owners = DesktopOwnersAmbient.current
    val density = AmbientDensity.current
    val owner = remember {
        val owner = DesktopOwner(owners, density)
        owner.setContent(children)
        owner
    }
    owner.density = density
    onDispose {
        owner.dispose()
    }
}
