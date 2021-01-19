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

import androidx.inspection.Connection
import androidx.inspection.Inspector
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol

private const val LAYOUT_INSPECTION_ID = "layoutinspector.compose.inspection"

// created by java.util.ServiceLoader
class ComposeLayoutInspectorFactory :
    InspectorFactory<ComposeLayoutInspector>(LAYOUT_INSPECTION_ID) {
    override fun createInspector(
        connection: Connection,
        environment: InspectorEnvironment
    ): ComposeLayoutInspector {
        return ComposeLayoutInspector(connection)
    }
}

class ComposeLayoutInspector(
    connection: Connection,
) : Inspector(connection) {

    override fun onReceiveCommand(data: ByteArray, callback: CommandCallback) {
        // TODO: Actually reply with a real response
        callback.reply(LayoutInspectorComposeProtocol.Response.getDefaultInstance().toByteArray())
    }
}