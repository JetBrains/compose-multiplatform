package com.google.r4a

import java.util.ArrayList
import java.util.Stack
import kotlin.reflect.KClass


class R4aElementBuilderDSL : MarkupBuilder() {
    public var stack = Stack<Element>()
    public var renderResults: MutableList<Element> = ArrayList()

    override fun component(type: Class<*>) {
        startComponent(type)
        endComponent()
    }

    override fun component(type: KClass<*>) {
        component(type.java)
    }

    override fun component(type: String) {
        startComponent(type)
        endComponent()
    }

    override fun component(type: Class<*>, body: Function1<R4aElementBuilderDSL, kotlin.Unit>) {
        startComponent(type)
        body.invoke(this)
        endComponent()
    }

    override fun component(type: KClass<*>, body: Function1<R4aElementBuilderDSL, kotlin.Unit>) {
        component(type.java, body)
    }

    override fun component(type: String, body: Function1<R4aElementBuilderDSL, kotlin.Unit>) {
        startComponent(type)
        body.invoke(this)
        endComponent()
    }

    fun attribute(key: String, value: Any?) {
        if (stack.size < 1) throw IllegalStateException("Unable to set `attribute` without cooresponding `component` block")
        stack.peek().putAttribute(key, value)
    }

    fun startComponent(type: Class<*>) {
        stack.push(Element(type))
    }

    fun startComponent(type: String) {
        stack.push(Element(type))
    }

    fun endComponent() {
        val element = stack.pop()
        if (stack.size > 0)
            stack.peek().addChild(element)
        else
            renderResults.add(element)
    }

    fun appendElements(elements: List<Element>) {
        if (stack.size > 0) {
            val topOfStack = stack.peek()
            for (element in elements) topOfStack.addChild(element)
        } else
            renderResults.addAll(elements)
    }
}
