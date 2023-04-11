package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class IntegrationTestsSetup:
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
        Drivers.dispose()
    }
}
