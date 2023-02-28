package org.jetbrains.compose.web.attributes.builders

internal actual fun createJsWeakMap(): JsWeakMap = jsNewWeakMap()

@JsFun("() => new WeakMap()")
private external fun jsNewWeakMap(): JsWeakMap
