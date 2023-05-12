package org.jetbrains.compose.web.attributes

value class AutoCompleteWasmImpl(val value: String): AutoComplete {
    override fun asString(): String = value
}

actual inline fun AutoComplete(value: String): AutoComplete {
    return AutoCompleteWasmImpl(value)
}