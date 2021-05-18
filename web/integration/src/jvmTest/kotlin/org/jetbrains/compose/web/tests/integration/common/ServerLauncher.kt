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

import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.http.content.staticRootFolder
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

object ServerLauncher {
    private lateinit var server: ApplicationEngine

    private var lock: Any? = null
    val port = 7777

    private fun log(message: String) {
        println("[ServerLauncher] $message")
    }

    /**
     * @param lock - guarantees that a server is started only once
     */
    fun startServer(lock: Any) {
        if (ServerLauncher.lock != null) return
        ServerLauncher.lock = lock

        val homePath = System.getProperty("COMPOSE_WEB_INTEGRATION_TESTS_DISTRIBUTION")

        log(
            "Starting localhost:$port using files in $homePath. " +
                "Initiated by ${lock::class.java.name}"
        )

        server = embeddedServer(Netty, port = port) {
            routing {
                static {
                    staticRootFolder = File(homePath)
                    files(".")
                    default("index.html")
                }
            }
        }.start()
    }

    /**
     * @param lock - guarantees that a server is stopped only by the same caller that started it
     */
    fun stopServer(lock: Any) {
        if (ServerLauncher.lock != lock) return
        ServerLauncher.lock = null
        log("Stopping server. Initiated by ${lock::class.java.name}")
        server.stop(1000, 1000)
    }
}
