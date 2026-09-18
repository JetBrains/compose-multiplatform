@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.testutils

import kotlinx.coroutines.await
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise

private external interface TestResourceResponse : JsAny {
    val ok: Boolean
    val status: Int
    fun text(): Promise<JsString>
}

@JsName("fetch")
private external fun fetchTestResource(input: String): Promise<TestResourceResponse>

@ComposeWebExperimentalTestsApi
@Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") // JsString is distinct from String on Wasm.
suspend fun fetchTestResourceText(input: String): String {
    val response = fetchTestResource(input).await<TestResourceResponse>()
    check(response.ok) { "Fetching $input failed with HTTP ${response.status}" }
    return response.text().await<JsString>().toString()
}
