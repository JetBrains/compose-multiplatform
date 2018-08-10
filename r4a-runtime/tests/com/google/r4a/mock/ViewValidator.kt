package com.google.r4a.mock

import org.junit.Assert

interface ViewValidator {
    val view: View
    fun next(): Boolean
}

class ViewListValidator(private val views: List<View>) : ViewValidator {
    override lateinit var view: View

    override fun next(): Boolean {
        if (iterator.hasNext()) {
            view = iterator.next()
            return true
        }
        return false
    }

    private val iterator by lazy { views.iterator() }

    fun validate(block: (ViewValidator.() -> Unit)?) {
        if (block != null) {
            Assert.assertNotEquals(0, views.size)
            this.block()
            val hasNext = next()
            Assert.assertEquals(false, hasNext)
        } else {
            Assert.assertEquals(0, views.size)
        }

    }
}

fun ViewValidator.view(name: String, block: (ViewValidator.() -> Unit)? = null) {
    val hasNext = next()
    Assert.assertEquals(true, hasNext)
    Assert.assertEquals(name, view.name)
    ViewListValidator(view.children).validate(block)
}

fun <T> ViewValidator.repeat(of: Iterable<T>, block: ViewValidator.(value: T) -> Unit) {
    for (value in of) {
        block(value)
    }
}

fun ViewValidator.linear() = view("linear", null)
fun ViewValidator.linear(block: ViewValidator.() -> Unit) = view("linear", block)
fun ViewValidator.box(block: ViewValidator.() -> Unit) = view("box", block)
fun ViewValidator.text(value: String) {
    view("text")
    Assert.assertEquals(value, view.attributes["text"])
}
fun ViewValidator.edit(value: String) {
    view("edit")
    Assert.assertEquals(value, view.attributes["value"])
}

fun ViewValidator.selectBox(selected: Boolean, block: ViewValidator.() -> Unit) {
    if (selected) {
        box {
            block()
        }
    } else {
        block()
    }
}

fun ViewValidator.skip(times: Int = 1) {
    repeat(times) {
        val hasNext = next()
        Assert.assertEquals(true, hasNext)
    }
}

fun validate(root: View, block: ViewValidator.() -> Unit) {
    ViewListValidator(root.children).validate(block)
}
