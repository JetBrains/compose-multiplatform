package org.jetbrains.compose.web

import kotlinx.coroutines.await
import kotlin.js.JSON
import kotlin.js.Promise

private external interface HydrationFixtureResponse {
    val ok: Boolean
    val status: Int
    fun text(): Promise<String>
}

private external interface JsHydrationTestState {
    val label: String
    val count: Int
}

@JsName("fetch")
private external fun fetchHydrationFixture(input: String): Promise<HydrationFixtureResponse>

internal actual suspend fun fetchHydrationFixtureText(input: String): String {
    val response = fetchHydrationFixture(input).await()
    check(response.ok) { "Fetching $input failed with HTTP ${response.status}" }
    return response.text().await()
}

internal actual fun decodeHydrationTestState(json: String): HydrationTestState {
    val state = JSON.parse<JsHydrationTestState>(json)
    return HydrationTestState(state.label, state.count)
}
