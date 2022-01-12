/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.skiko

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeCanvas
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLScriptElement
import kotlin.js.Promise

private class ScriptInserter() {
    private val requested = mutableMapOf<String, Promise<String>>()

    private fun attach(src: String, callback: () -> Unit) {
        val script = document.createElement("script") as HTMLScriptElement
        script.src = src

        script.type = "text/javascript"
        script.async = true

        val root = document.documentElement?.firstChild

        script.asDynamic().onerror = {
            console.error("failed to attach script ${src}")
            requested.remove(src)
        }

        script.onload = {
            callback()
        }

        root?.appendChild(script)
    }

    fun load(src: String): Promise<String> {
        return requested.getOrPut(src) {
            Promise { resolve, reject ->
                attach(src) {
                    resolve(src)
                }
            }
        }
    }
}

private val scriptInserter = ScriptInserter()

@Composable
fun ElementScope<HTMLCanvasElement>.skiko(block: @Composable () -> Unit) {
    DomSideEffect { canvas ->
        scriptInserter.load("/skiko.js").then { src ->
            onWasmReady {
                var skikoCanvas: ComposeCanvas? = null

                onWasmReady {
                    skikoCanvas = ComposeCanvas(canvas)
                    skikoCanvas?.setContent(block)
                }

                onDispose {
                    skikoCanvas?.dispose()
                }

            }
        }
    }
}