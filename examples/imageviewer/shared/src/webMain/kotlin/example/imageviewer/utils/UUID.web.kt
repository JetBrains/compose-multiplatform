package example.imageviewer.utils

@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("uuid")
external object UUID {
    fun v4(): String
}