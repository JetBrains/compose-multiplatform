package androidx.compose.mock

fun indent(indent: Int) {
    repeat(indent) { print(' ') }
}

open class View {
    var name: String = ""
    val children = mutableListOf<View>()
    val attributes = mutableMapOf<String, Any>()

    fun render(indent: Int = 0) {
        indent(indent)
        print("<$name$attributesAsString")
        if (children.size > 0) {
            println(">")
            children.forEach { it.render(indent + 2) }
            indent(indent)
            println("</$name>")
        } else {
            println(" />")
        }
    }

    fun addAt(index: Int, view: View) { children.add(index, view) }
    fun removeAt(index: Int, count: Int) {
        if (index < children.count()) {
            if (count == 1) {
                children.removeAt(index)
            } else {
                children.subList(index, index + count).clear()
            }
        }
    }
    fun moveAt(from: Int, to: Int, count: Int) {
        if (count == 1) {
            val insertLocation = if (from > to) to else (to - 1)
            children.add(insertLocation, children.removeAt(from))
        } else {
            val insertLocation = if (from > to) to else (to - count)
            val itemsToMove = children.subList(from, from + count)
            val copyOfItems = itemsToMove.map { it }
            itemsToMove.clear()
            children.addAll(insertLocation, copyOfItems)
        }
    }

    fun attribute(name: String, value: Any) { attributes[name] = value }

    private val attributesAsString get() =
        if (attributes.isEmpty()) ""
        else attributes.map { " ${it.key}='${it.value}'" }.joinToString()
    private val childrenAsString: String get() =
        children.map { it.toString() }.joinToString(" ")

    override fun toString() = "<$name$attributesAsString>$childrenAsString</$name>"
}
