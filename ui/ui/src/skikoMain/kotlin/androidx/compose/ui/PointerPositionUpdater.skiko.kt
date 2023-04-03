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

package androidx.compose.ui

import kotlin.js.JsName

/**
 * Updater of the current position of pointer.
 *
 * If something happened with Compose content (it relayouted), we need to send an
 * event to it with the latest pointer position. Otherwise, the content won't be updated
 * by the actual relative position of the pointer.
 *
 * For example, it can be needed when we scroll content without moving the pointer, and we need
 * to highlight the items under the pointer.
 */
internal class PointerPositionUpdater(
    private val onNeedUpdate: () -> Unit,
    private val syntheticEventSender: SyntheticEventSender,
) {
    var needUpdate: Boolean = false
        private set

    fun reset() {
        needUpdate = false
    }

    @JsName("setNeedUpdate")
    fun needSendMove() {
        needUpdate = true
        onNeedUpdate()
    }

    fun update() {
        if (needUpdate) {
            needUpdate = false
            syntheticEventSender.sendSyntheticMove()
        }
    }
}
