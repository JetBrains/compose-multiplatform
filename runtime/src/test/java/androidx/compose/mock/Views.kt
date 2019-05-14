package androidx.compose.mock

class SourceLocation(val name: String) {
    override fun toString(): String = "SL $name"
}

val repeat = SourceLocation("repeat")
inline fun <T : Any> MockViewComposition.repeat(
    of: Iterable<T>,
    crossinline block: MockViewComposition.(value: T) -> Unit
) {
    for (value in of) {
        cc.startGroup(cc.joinKey(repeat, value))
        block(value)
        cc.endGroup()
    }
}

val linear = SourceLocation("linear")
fun MockViewComposition.linear(block: Compose) {
    emit(linear, { View().apply { name = "linear" } }, block)
}

fun MockViewComposition.linear(key: Any, block: Compose) {
    emit(key, { View().apply { name = "linear" } }, block)
}

val text = SourceLocation("text")
fun MockViewComposition.text(value: String) {
    emit(text, { View().apply { name = "text" } }, value, { attribute("text", it) })
}

val edit = SourceLocation("edit")
fun MockViewComposition.edit(value: String) {
    emit(edit, { View().apply { name = "edit" } }, value, { attribute("value", it) })
}

val box = SourceLocation("box")
fun MockViewComposition.selectBox(selected: Boolean, block: Compose) {
    if (selected) {
        emit(box, { View().apply { name = "box" } }, block)
    } else {
        block()
    }
}

fun MockViewComposition.skip(key: Any, block: Compose) {
    call(key, { false }) { block() }
}