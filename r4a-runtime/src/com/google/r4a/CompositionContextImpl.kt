package com.google.r4a

import android.content.Context
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import java.util.*


// NOTE(lmr): we use data class to get the hashCode() implementation, but we use identity for equality. The data class properties are all
// final, so hashCode() should be stable.
internal data class Slot(
    val sourceHash: Int,
    val key: Any?,
    val depth: Int
) {
    override fun equals(other: Any?) = other === this
    // TODO(lmr): we could probably replace parent with a stack
    lateinit var parent: Slot

    var container: Container? = null
    var instance: Any? = null
    var attributes = mutableListOf<Any?>()
    var child: Slot? = null
    // TODO(lmr): I think we can get rid of prevSibling
    var prevSibling: Slot? = null
    var nextSibling: Slot? = null
    var open = true
    var argIndex = 0
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
            parent === this -> "<ROOT>"
            inst == null -> "(EMPTY)"
            else -> inst.javaClass.getSimpleName()
        }

        return "$name$suffix"
    }
}

internal class Container {
    lateinit var view: ViewGroup
    var index: Int = 0
    var parent: Container = this
    var children = mutableMapOf<Slot, Container>()
}


internal class CompositionContextImpl: CompositionContext() {

    companion object {
        private val COMPONENTS_TO_SLOTS = WeakHashMap<Component, Slot>()
        val factory = object: Function3<Context, ViewGroup, Component, CompositionContext> {
            override fun invoke(context: Context, root: ViewGroup, component: Component): CompositionContext {
                val result = CompositionContextImpl()
                result.ROOT_CONTAINER.view = root
                result.context = context
                result.setInstance(component)
                return result
            }
        }
        private val SLOT_DEPTH_COMPARATOR = { a: Slot, b: Slot -> a.depth - b.depth }
    }

    override lateinit var context: Context

    internal val ROOT_CONTAINER = Container()
    internal var currentContainer: Container = ROOT_CONTAINER

    internal val ROOT_SLOT = Slot(0, 0, 0)
    internal var currentSlot: Slot = ROOT_SLOT

    init {
        ROOT_SLOT.parent = ROOT_SLOT
        ROOT_SLOT.container = ROOT_CONTAINER
    }

    override fun debug() {
        ROOT_SLOT.print(0)
    }

    private var hasPendingFrame = false
    private var isComposing = false

    /**
     * We keep a queue of slots that need to be recomposed. You can think of calling `recompose(Component)` as similar
     * to `invalidate()` or `requestLayout()`. We essentially mark the slot to be recomposed. It will either happen
     * synchronously or on the next frame. Those slots are stored in this TreeSet, where they are ordered based on tree
     * depth. The highest in the tree will be recomposed first, which may end up recomposing elements that are in the
     * set, which will then get removed during iteration. Additionally, slots may get added to this set during recompose
     * in the case of Ambient values changing. As a result, the iterator() of this object should not be used, and
     * instead it should only be iterated over using `while (isNotEmpty()) { pollFirst() }`
     */
    private val composeQueue = ComposeQueue<Slot>(SLOT_DEPTH_COMPARATOR)

    private val frameCallback = Choreographer.FrameCallback {
        hasPendingFrame = false
        recomposePending()
    }

    private fun recomposePending() {
        val queue = composeQueue
        if (isComposing) return
        try {
            isComposing = true
            // NOTE(lmr): we are not checking whether or not this results in an infinite recursion where recomposing a
            // slot always results in itself being added to the queue again. Perhaps we should...
            while (queue.isNotEmpty()) {
                queue.pop()?.let { recomposeFrom(it) }
            }
        } finally {
            isComposing = false
        }
    }

    override fun recompose(component: Component) {
        val slot = COMPONENTS_TO_SLOTS[component] ?: error("tried to recompose but could not find slot for component")
        composeQueue.add(slot)

        // if we're not currently composing and a frame hasn't been scheduled, we want to schedule it
        if (!isComposing && !hasPendingFrame) {
            hasPendingFrame = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    // NOTE(lmr): when we move to a multi-threaded model, this will have to do some concurrency management in the case
    // that a recompose() is called from the main thread while a recompose is occurring on the background thread. Leaving
    // this out for now as it doesn't really make sense until we move into the multi-threaded model
    override fun recomposeSync(component: Component) {
        val slot = COMPONENTS_TO_SLOTS[component] ?: error("tried to recompose but could not find slot for component")
        composeQueue.add(slot)
        if (!isComposing) {
            recomposePending()
        }
    }

    private fun recomposeFrom(slot: Slot) {
        currentSlot = slot
        currentContainer = slot.container ?: throw Exception("slot has no container")
        // TODO(lmr): this won't be quite good enough... as index could be out of sync if a sibling
        // updated and changed the number of children that it created
        currentContainer.index = slot.index


        val prevContext = CompositionContext.current
        CompositionContext.current = this
        // NOTE: It's important that we open the slot before composing. We could also potentially
        // call start(...) and end(...) here, but I think that that may not really be what we want.
        slot.open = true
        compose()
        end()
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
        key: Any?,
        depth: Int
    ): Slot {
        // common case: mounting on first pass
        if (start == null) {
            val next = Slot(sourceHash, key, depth)
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
        next = Slot(sourceHash, key, depth)
        initializeSlot(next)
        return next
    }

    private fun reuseSlot(it: Slot) {
        val container = currentContainer
        // update on top of existing slot
        it.open = true
        it.argIndex = 0
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
            if (currentChild != null) {
                it.nextSibling = currentChild
                currentChild.prevSibling = it
            }
            it.parent = current
        } else {
            val currentNextSibling = current.nextSibling
            current.nextSibling = it
            if (currentNextSibling != null) {
                it.nextSibling = currentNextSibling
                currentNextSibling.prevSibling = it
            }
            it.prevSibling = current
            it.parent = current.parent
        }
    }

    override fun <T> getAmbient(key: Ambient<T>): T = getAmbient(key, currentSlot)

    override fun <T> getAmbient(key: Ambient<T>, component: Component): T {
        val slot = COMPONENTS_TO_SLOTS[component] ?: error("Couldn't find slot for compoenent")
        return getAmbient(key, slot)
    }

    private fun <T> getAmbient(key: Ambient<T>, slot: Slot): T {
        var slot = slot
        while (slot.key !== key && slot.parent !== slot) {
            slot = slot.parent
        }
        if (slot.parent === slot) {
            // we made it to the root of the tree... return the default value
            return key.defaultValue
        }
        val instance = slot.parent.instance as? Ambient<*>.Provider ?: error("Expected Ambient<>.Provider")
        instance.subscribers.add(slot)
        return instance.value as T
    }

    // TODO(lmr): we could add an int that specifies the number of attributes on the element so we can
    // initialize an array of that length to store the attributes since that information is known at compile time
    // and is constant
    override fun start(sourceHash: Int): Any? = start(sourceHash, null)
    override fun start(sourceHash: Int, key: Any?): Any? {
        val current = currentSlot
        val start = if (current.open) current.child else current.nextSibling
        val depth = if (current.open) current.depth + 1 else current.depth
        val next = findOrCreate(start, sourceHash, key, depth)
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
            currentSlot.open = false

            val next = current.nextSibling
            if (next != null) {
                unmountTail(next, container.view)
            }
        }
        if (currentSlot.instance is ViewGroup) {
            currentContainer = container.parent
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
            slot.container = container
            COMPONENTS_TO_SLOTS[instance] = slot
        }
        slot.instance = instance
        slot.index = index
    }

    override fun updateAttribute(value: Any?): Boolean {
        val slot = currentSlot
        val i = slot.argIndex
        slot.argIndex++
        if (slot.attributes.size == i) {
            currentSlot.attributes.add(value)
            return true
        } else {
            val current = slot.attributes[i]
            slot.attributes[i] = value
            return current != value
        }
    }

    override fun compose() {
        val slot = currentSlot
        val instance = slot.instance
        when (instance) {
            is Component -> {
                // if it had a pending recompose, we are recomposing it now so we can remove
                composeQueue.remove(slot)
                instance.compose()
            }
            else -> {
                // TODO(lmr): we should *really* use something other than reflection here, but I am having a lot of trouble
                // getting the IR code to work in a reasonable way. This is good enough for prototyping etc, but we should
                // aim to get back to this if we can.
                if (instance == null) {
                    error("instance was null!")
                }
                val args = slot.attributes
                val klass = instance::class.java
                val method = klass.methods.firstOrNull { it.name == "invoke" }
                // NOTE(lmr): we remove instance now so that compose always updates/sets the instance afterwards
                slot.instance = null
                if (method != null) {
                    method.invoke(instance, *args.toTypedArray())
                } else {
                    error("couldnt find invoke method!")
                }
            }
        }
    }

    private fun unmountTail(slot: Slot, container: ViewGroup) {
        val instance = slot.instance
        val child = slot.child
        val prevSibling = slot.prevSibling
        val nextSibling = slot.nextSibling

        // if the slot had a pending recompose, we cancel it now since it is being unmounted
        composeQueue.remove(slot)


        slot.parent = slot

        if (prevSibling != null) {
            prevSibling.nextSibling = null
            slot.prevSibling = null
        }

        if (instance is ViewGroup) {
            slot.container?.children?.remove(slot)
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