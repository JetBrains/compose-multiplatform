@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web

import kotlinx.browser.JsAny
import kotlinx.browser.JsNumber
import kotlinx.browser.JsString
import kotlinx.browser.toDouble
import kotlinx.browser.toJsString
import kotlinx.coroutines.await
import kotlin.js.Promise

private external interface HydrationFixtureResponse : JsAny {
    val ok: Boolean
    val status: Int
    fun text(): Promise<JsString>
}

private external interface WasmHydrationTestState : JsAny {
    val label: JsString
    val count: JsNumber
}

@JsName("fetch")
private external fun fetchHydrationFixture(input: String): Promise<HydrationFixtureResponse>

@JsFun("(input) => JSON.parse(input)")
private external fun parseHydrationTestState(input: JsString): WasmHydrationTestState

internal actual suspend fun fetchHydrationFixtureText(input: String): String {
    val response = fetchHydrationFixture(input).await<HydrationFixtureResponse>()
    check(response.ok) { "Fetching $input failed with HTTP ${response.status}" }
    return response.text().await<JsString>().toString()
}

internal actual fun decodeHydrationTestState(json: String): HydrationTestState {
    val state = parseHydrationTestState(json.toJsString())
    return HydrationTestState(state.label.toString(), state.count.toDouble().toInt())
}
