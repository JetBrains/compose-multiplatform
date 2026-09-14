@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.core.tests.svg

import kotlinx.coroutines.await
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise

private external interface SvgFixtureResponse : JsAny {
    val ok: Boolean
    val status: Int
    fun text(): Promise<JsString>
}

@JsName("fetch")
private external fun fetchSvgFixture(input: String): Promise<SvgFixtureResponse>

internal actual suspend fun fetchSvgFixtureText(input: String): String {
    val response = fetchSvgFixture(input).await<SvgFixtureResponse>()
    check(response.ok) { "Fetching $input failed with HTTP ${response.status}" }
    return response.text().await<JsString>().toString()
}
