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
package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class StaticServerSetupExtension :
    BeforeAllCallback,
    ExtensionContext.Store.CloseableResource {

    companion object {
        private const val STORE_KEY = "STOP_SERVER_HOOK"
    }

    override fun beforeAll(context: ExtensionContext?) {
        val hook = context!!.root.getStore(ExtensionContext.Namespace.GLOBAL).get(STORE_KEY)

        if (hook == null) {
            ServerLauncher.startServer(this)
            context.root.getStore(ExtensionContext.Namespace.GLOBAL).put(STORE_KEY, this)
        }
    }

    override fun close() {
        ServerLauncher.stopServer(this)
    }
}
