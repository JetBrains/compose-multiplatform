package com.google.r4a

import android.content.Context
import android.view.View
import android.view.ViewGroup

private class Slot(val sourceHash: Int, val key: Any?) {
    lateinit var parent: Slot

    var instance: Any? = null
    var attributes = mutableMapOf<String, Any?>()
    var child: Slot? = null
    var prevSibling: Slot? = null
    var nextSibling: Slot? = null
    var open = true
    var index = 0
    // TODO: store inc of children?
    // TODO: store map of children? lazily?
    fun print(indent: Int) {
        val whiteSpace = " ".repeat(indent)

        println("$whiteSpace $this")

        val child = child
        if (child != null) {
            child.print(indent + 2)
        }

        val nextSibling = nextSibling
        if (nextSibling != null) {
            nextSibling.print(indent)
        }
    }

    override fun toString(): String {
        val inst = instance
        val suffix = "<$sourceHash-$key> (open: $open, index: $index)"
        val name = when {
            parent == this -> "<ROOT>"
            inst == null -> "(EMPTY)"
            else -> inst.javaClass.getSimpleName()
        }

        return "$name$suffix"
    }
}

private class Container {
    lateinit var view: ViewGroup
    var index: Int = 0
    var parent: Container = this
    var children = mutableMapOf<Slot, Container>()
}


class CompositionContextImpl: CompositionContext() {

    companion object {
        val factory = object: Function3<Context, ViewGroup, Component, CompositionContext> {
            override fun invoke(context: Context, root: ViewGroup, component: Component): CompositionContext {
                val result = CompositionContextImpl()
                result.ROOT_CONTAINER.view = root
                result.context = context
                result.setInstance(component)
                return result
            }
        }
        val DUMMY = CompositionContextImpl()
    }

    override lateinit var context: Context

    private val ROOT_CONTAINER = Container()
    private var currentContainer: Container = ROOT_CONTAINER

    private val ROOT_SLOT = Slot(0, 0)
    private var currentSlot: Slot = ROOT_SLOT

    init {
        ROOT_SLOT.parent = ROOT_SLOT
    }

    fun printSlots() {
        ROOT_SLOT.print(0)
    }

    override fun recomposeFromRoot() {
        currentSlot = ROOT_SLOT
        currentContainer = ROOT_CONTAINER
        currentContainer.index = 0

        val prevContext = CompositionContext.current
        CompositionContext.current = this
        compose()
        // restore previous context to whatever it was
        CompositionContext.current = prevContext
    }

    private fun swapChildren(parent: ViewGroup, i: Int, j: Int) {
        val a = parent.getChildAt(i)
        val b = parent.getChildAt(j)
        parent.removeView(a)
        parent.removeView(b)
        if (i < j) {
            parent.addView(b, i)
            parent.addView(a, j)
        } else {
            parent.addView(a, j)
            parent.addView(b, i)
        }
    }

    // finds a slot with matching hash/key and
    private fun findOrCreate(
        start: Slot?,
        sourceHash: Int,
        key: Any?
    ): Slot {
        // common case: mounting on first pass
        if (start == null) {
            val next = Slot(sourceHash, key)
            initializeSlot(next)
            return next
        }

        // common case: recompose but structure didn't change
        if (start.sourceHash == sourceHash && start.key == key) {
            reuseSlot(start)
            return start
        }

        // we didn't find the slot to reuse immediately, so we have to search all siblings to
        // preserve the instance if possible
        val startPrevSibling = start.prevSibling
        var next: Slot? = start.nextSibling
        while (next != null) {
            if (next.sourceHash == sourceHash && next.key == key) {

                // cut next out of the chain
                val nextNextSibling = next.nextSibling
                nextNextSibling?.prevSibling = next.prevSibling
                next.prevSibling?.nextSibling = nextNextSibling

                // insert it in front of start
                if (startPrevSibling == null) {
                    // it was first in chain, so we have to replace the parent's child reference
                    // with this one
                    start.parent.child = next
                } else {
                    startPrevSibling.nextSibling = next
                }
                start.prevSibling = next
                next.prevSibling = startPrevSibling
                next.nextSibling = start

                // slot moved. we need to
                moveSlot(start, next)
                reuseSlot(next)

                return next
            }
            next = next.nextSibling
        }
        next = Slot(sourceHash, key)
        initializeSlot(next)
        return next
    }

    private fun reuseSlot(it: Slot) {
        val container = currentContainer
        // update on top of existing slot
        it.open = true
        // push container stack if its a
        val index = container.index
        if (it.instance is View) {
            container.index++
        }
        if (it.instance is ViewGroup) {
            val nextContainer = container.children[it]!!
            nextContainer.index = 0
            currentContainer = nextContainer
        }
        it.index = index
    }

    private fun moveSlot(a: Slot, b: Slot) {
        val container = currentContainer
        val index = container.index

        val ai = a.index
        val bi = b.index

        a.index = bi
        b.index = ai

        // the number of views separated these two are. since they are futher down the tree
        // and haven't been recomposed yet, we can expect the difference from the last
        // compose to be accurate
        val offset = bi - ai

        // TODO(lmr): this seems fragile... what if the slots are composite components that
        // are rendering multiple things?
        swapChildren(currentContainer.view, index, index + offset)
    }

    private fun initializeSlot(it: Slot) {
        val current = currentSlot
        if (current.open) {
            val currentChild = current.child
            current.child = it
            it.nextSibling = currentChild
            it.parent = current
        } else {
            val currentNextSibling = current.nextSibling
            current.nextSibling = it
            it.nextSibling = currentNextSibling
            it.prevSibling = current
            it.parent = current.parent
        }
    }

    override fun start(sourceHash: Int): Any? = start(sourceHash, null)
    override fun start(sourceHash: Int, key: Any?): Any? {
        val current = currentSlot
        val start = if (current.open) current.child else current.nextSibling
        val next = findOrCreate(start, sourceHash, key)
        currentSlot = next
        return next.instance
    }

    override fun end() {
        val current = currentSlot
        val container = currentContainer
        // end of current...
        if (current.open) {
            current.open = false
        } else {
            // we are moving up the tree
            currentSlot = current.parent

            if (currentSlot.instance is ViewGroup) {
                currentContainer = container.parent
            }
            currentSlot.open = false

            val next = current.nextSibling
            if (next != null) {
                unmountTail(next, container.view)
            }
        }
    }

    override fun setInstance(instance: Any) {
        val slot = currentSlot
        val container = currentContainer
        val index = container.index
        if (instance is View) {
            // add to current container
            container.view.addView(instance, index)
            container.index++
        }
        if (instance is ViewGroup) {
            // push this as container to the stack
            val next = Container()
            next.view = instance
            next.parent = container
            container.children[slot] = next
            currentContainer = next
        }
        if (instance is Component) {
            CompositionContext.associate(instance, this)
        }
        slot.instance = instance
        slot.index = index
    }

    override fun updAttr(key: String, value: Any?): Boolean {
        if (!currentSlot.attributes.contains(key)) {
            currentSlot.attributes[key] = value
            return true
        } else {
            val current = currentSlot.attributes[key]
            currentSlot.attributes[key] = value
            return current != value
        }
    }

    override fun compose() {
        val component = currentSlot.instance as Component
        component.compose()
    }

    private fun unmountTail(slot: Slot, container: ViewGroup) {
        val instance = slot.instance
        val child = slot.child
        val prevSibling = slot.prevSibling
        val nextSibling = slot.nextSibling


        slot.parent = slot

        if (prevSibling != null) {
            prevSibling.nextSibling = null
            slot.prevSibling = null
        }

        if (instance is ViewGroup) {
            // TODO(lmr): remove slot from container.children
            slot.child = null
            if (child != null) {
                unmountTail(child, instance)
            }
            container.removeView(instance)
        } else if (instance is View) {
            container.removeView(instance)
        } else {
            slot.child = null
            if (child != null) {
                unmountTail(child, container)
            }
        }

        if (nextSibling != null) {
            unmountTail(nextSibling, container)
            nextSibling.prevSibling = prevSibling
            slot.nextSibling = null
        }
    }
}