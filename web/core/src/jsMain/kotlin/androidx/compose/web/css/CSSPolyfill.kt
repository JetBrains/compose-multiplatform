package androidx.compose.web.css

import org.w3c.dom.Window

@JsModule("css-typed-om")
@JsNonModule
abstract external class CSSTypedOMPolyfill {
    companion object {
        fun default(window: Window)
    }
}

fun StylePropertyMap.clear() {
    throw AssertionError("StylePropertyMap::clear isn't polyfilled")
}
