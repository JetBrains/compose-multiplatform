package androidx.compose.mock

import org.junit.Assert

interface MockViewValidator {
    val view: View
    fun next(): Boolean
}

class MockViewListValidator(private val views: List<View>) :
    MockViewValidator {
    override lateinit var view: View

    override fun next(): Boolean {
        if (iterator.hasNext()) {
            view = iterator.next()
            return true
        }
        return false
    }

    private val iterator by lazy { views.iterator() }

    fun validate(block: (MockViewValidator.() -> Unit)?) {
        if (block != null) {
            this.block()
            val hasNext = next()
            Assert.assertEquals("Expected children but none found", false, hasNext)
        } else {
            Assert.assertEquals("Not expecting children but some found", 0, views.size)
        }
    }
}

fun MockViewValidator.view(name: String, block: (MockViewValidator.() -> Unit)? = null) {
    val hasNext = next()
    Assert.assertEquals(true, hasNext)
    Assert.assertEquals(name, view.name)
    MockViewListValidator(view.children).validate(block)
}

fun <T> MockViewValidator.repeat(of: Iterable<T>, block: MockViewValidator.(value: T) -> Unit) {
    for (value in of) {
        block(value)
    }
}

fun MockViewValidator.linear() = view("linear", null)
fun MockViewValidator.linear(block: MockViewValidator.() -> Unit) = view("linear", block)
fun MockViewValidator.box(block: MockViewValidator.() -> Unit) = view("box", block)
fun MockViewValidator.text(value: String) {
    view("text")
    Assert.assertEquals(value, view.attributes["text"])
}
fun MockViewValidator.edit(value: String) {
    view("edit")
    Assert.assertEquals(value, view.attributes["value"])
}

fun MockViewValidator.selectBox(selected: Boolean, block: MockViewValidator.() -> Unit) {
    if (selected) {
        box {
            block()
        }
    } else {
        block()
    }
}

fun MockViewValidator.skip(times: Int = 1) {
    repeat(times) {
        val hasNext = next()
        Assert.assertEquals(true, hasNext)
    }
}

fun validate(root: View, block: MockViewValidator.() -> Unit) {
    MockViewListValidator(root.children).validate(block)
}
