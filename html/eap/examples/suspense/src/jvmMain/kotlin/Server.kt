/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.Desktop
import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.web.HtmlStream
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.composeHtmlToStreamAsync
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.*

// JVM only. Replace the mock with a database or repository call.
suspend fun loadUser(id: Long): User {
    println("[JVM] Starting user lookup for $id")
    delay(1_000) // Simulate a one-second database call.
    require(id > 0) { "User $id does not exist" }
    println("[JVM] Completed user lookup for $id")
    return User(
        id = id,
        name = "Maya Chen",
        role = "Design engineer",
        location = "Amsterdam",
        bio = "Making thoughtful things for the web. Interested in the space between design and code.",
    )
}

suspend fun CoroutineScope.renderUserPage(id: Long, load: suspend (Long) -> User = ::loadUser): HtmlStream =
    composeHtmlToStreamAsync {
        Html(attrs = { lang("en") }) {
            Head {
                Meta(attrs = { attr("charset", "utf-8") })
                Meta(attrs = {
                    attr("name", "viewport")
                    attr("content", "width=device-width, initial-scale=1")
                })
                Title { Text("Compose HTML / A Suspense user page") }
                Style(PageStyles)
            }
            Body {
                UserPage(id, load)
                // Load application scripts in the shell. Scripts in replacement content do not run.
                Script(attrs = {
                    type(ScriptType.Module)
                    src("/app.js")
                })
            }
        }
    }

fun main(args: Array<String>) {
    val assets = File(args[0]).canonicalFile
    val port = args.getOrNull(1)?.toInt() ?: 8080
    val executor = Executors.newCachedThreadPool()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    server.executor = executor
    server.createContext("/") { exchange -> handleRequest(exchange, assets) }
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(0)
        executor.shutdownNow()
    })
    server.start()
    val url = URI("http://localhost:$port/?id=42")
    println("Suspense example: $url")
    println("Each refresh starts a one-second JVM lookup. Use ?id=0 to show an error.")
    openInBrowser(url)
}

private fun openInBrowser(url: URI) {
    val opened = runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return@runCatching false
        desktop.browse(url)
        true
    }.getOrDefault(false)

    if (!opened) {
        println("Open $url in a browser to view it.")
    }
}

private fun handleRequest(exchange: HttpExchange, assets: File) {
    try {
        when {
            exchange.requestMethod != "GET" -> exchange.sendResponseHeaders(405, -1)
            exchange.requestURI.path == "/" -> serveUserPage(exchange)
            else -> serveAsset(exchange, assets)
        }
    } catch (failure: Exception) {
        System.err.println("Request ended: ${failure.message}")
    } finally {
        exchange.close()
    }
}

private fun serveAsset(exchange: HttpExchange, assets: File) {
    val asset = File(assets, exchange.requestURI.path.removePrefix("/")).canonicalFile
    if (!asset.toPath().startsWith(assets.toPath()) || !asset.isFile) {
        exchange.sendResponseHeaders(404, -1)
        return
    }

    exchange.responseHeaders.set("Content-Type", contentType(asset))
    exchange.sendResponseHeaders(200, asset.length())
    asset.inputStream().use { it.copyTo(exchange.responseBody) }
}

private fun contentType(asset: File): String = when (asset.extension) {
    "js", "mjs" -> "text/javascript; charset=utf-8"
    "wasm" -> "application/wasm"
    "map", "json" -> "application/json"
    else -> "application/octet-stream"
}

private fun serveUserPage(exchange: HttpExchange) = runBlocking {
    val id = exchange.requestURI.rawQuery.orEmpty().split('&')
        .firstOrNull { it.startsWith("id=") }?.substringAfter('=')?.toLongOrNull() ?: 42L
    withTimeout(10_000) {
        val stream = renderUserPage(id)
        try {
            writePage(exchange, stream)
        } finally {
            stream.cancel()
        }
    }
}

private suspend fun writePage(exchange: HttpExchange, stream: HtmlStream) {
    exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
    exchange.responseHeaders.set("Cache-Control", "no-store")
    exchange.sendResponseHeaders(200, 0) // Send each chunk without buffering the whole page.
    exchange.responseBody.write("<!doctype html>".toByteArray(Charsets.UTF_8))
    stream.chunks.collect { chunk ->
        exchange.responseBody.write(chunk.toByteArray(Charsets.UTF_8))
        exchange.responseBody.flush()
    }
}
