package org.jetbrains.compose.web.attributes.builders

internal actual fun createJsWeakMap(): JsWeakMap = js("new WeakMap();").unsafeCast<JsWeakMap>()
