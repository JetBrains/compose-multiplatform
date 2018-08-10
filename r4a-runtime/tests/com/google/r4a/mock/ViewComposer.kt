package com.google.r4a.mock

import com.google.r4a.*

interface ViewComposition {
    val cc: Composition<View>
}

abstract class ViewComponent : Recomposable, ViewComposition {
    private var recomposer: (() -> Unit)? = null
    private lateinit var _composition: Composition<View>
    @PublishedApi
    internal fun setComposition(value: Composition<View>) {
        _composition = value
    }

    override val cc: Composition<View> get() = _composition
    override fun setRecompose(recompose: () -> Unit) {
        recomposer = recompose
    }

    fun recompose() {
        recomposer?.let { it() }
    }
}

typealias Compose = ViewComposition.() -> Unit

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
object ViewApplierAdapter : ApplyAdapter<View> {
    override fun View.insertAt(index: Int, instance: View) = addAt(index, instance)
    override fun View.removeAt(index: Int, count: Int) = removeAt(index, count)
    override fun View.move(from: Int, to: Int, count: Int) = moveAt(from, to, count)
}

class ViewComposer(val root: View) : Composer<View>(SlotTable(), Applier(root, ViewApplierAdapter)) {
    private val rootComposer: ViewComposition by lazy {
        object : ViewComposition {
            override val cc: Composition<View> get() = this@ViewComposer
        }
    }

    fun compose(composition: ViewComposition.() -> Unit) {

        composeRoot {
            rootComposer.composition()
        }
    }
}

inline fun <V> ViewComposition.remember(block: () -> V): V = cc.remember(block)
inline fun <V, reified P1> ViewComposition.remember(p1: P1, block: () -> V) = cc.remember(p1, block)
inline fun <V, reified P1, reified P2> ViewComposition.remember(p1: P1, p2: P2, block: () -> V) = cc.remember(p1, p2, block)
inline fun <V, reified P1, reified P2, reified P3> ViewComposition.remember(p1: P1, p2: P2, p3: P3, block: () -> V) = cc.remember(p1, p2, p3, block)
inline fun <V, reified P1, reified P2, reified P3, reified P4> ViewComposition.remember(p1: P1, p2: P2, p3: P3, p4: P4, block: () -> V) = cc.remember(p1, p2, p3, p4, block)
inline fun <V> ViewComposition.remember(vararg args: Any, block: () -> V): V = cc.remember(*args, block = block)

inline fun <reified P1> ViewComposition.memoize(key: Any, p1: P1, block: ViewComposition.(p1: P1) -> Unit) {
    cc.startGroup(key)
    if (!cc.changed(p1)) cc.skipGroup()
    else block(p1)
    cc.endGroup()
}


inline fun <V : View> ViewComposition.emit(key: Any, noinline factory: () -> V, block: ViewComposition.() -> Unit) {
    cc.startNode(key)
    cc.emitNode(factory)
    block()
    cc.endNode()
}

inline fun <V : View, reified A1> ViewComposition.emit(key: Any, noinline factory: () -> V, a1: A1, noinline set1: V.(A1) -> Unit) {
    cc.startNode(key)
    cc.emitNode(factory)
    if (cc.changed(a1)) {
        cc.apply(a1, set1)
    }
    cc.endNode()
}


inline fun <reified C : ViewComponent, reified A1> ViewComposition.composeComponent(
    key: Any,
    crossinline factory: () -> C,
    a1: A1, set1: C.(A1) -> Unit
) {
    val myCC = cc
    myCC.startGroup(key)
    val component = myCC.remember { factory().apply { setComposition(myCC) } }
    var skip = true
    if (myCC.changed(a1)) {
        apply { component.set1(a1) }
        skip = false
    }
    myCC.startCompose(skip, component)
    if (!skip) component.compose()
    myCC.doneCompose(skip)
    myCC.endGroup()
}

inline fun <reified C : ViewComponent, reified A1, reified A2> ViewComposition.composeComponent(
    key: Any,
    crossinline factory: () -> C,
    a1: A1, set1: C.(A1) -> Unit,
    a2: A2, set2: C.(A2) -> Unit
) {
    val myCC = cc
    myCC.startGroup(key)
    val component = myCC.remember { factory().apply { setComposition(myCC) } }
    component.setComposition(cc)
    var skip = true
    if (myCC.changed(a1)) {
        apply { component.set1(a1) }
        skip = false
    }
    if (myCC.changed(a2)) {
        apply { component.set2(a2) }
        skip = false
    }
    myCC.startCompose(skip, component)
    if (!skip) component.compose()
    myCC.doneCompose(skip)
    myCC.endGroup()
}

inline fun <reified C : ViewComponent, reified A1, reified A2, reified A3> ViewComposition.composeComponent(
    key: Any,
    crossinline factory: () -> C,
    a1: A1, set1: C.(A1) -> Unit,
    a2: A2, set2: C.(A2) -> Unit,
    a3: A3, set3: C.(A3) -> Unit
) {
    val myCC = cc
    myCC.startGroup(key)
    val component = myCC.remember { factory().apply { setComposition(myCC) } }
    var skip = true
    if (myCC.changed(a1)) {
        apply { component.set1(a1) }
        skip = false
    }
    if (myCC.changed(a2)) {
        apply { component.set2(a2) }
        skip = false
    }
    if (myCC.changed(a3)) {
        apply { component.set3(a3) }
        skip = false
    }
    myCC.startCompose(skip, component)
    if (!skip) component.compose()
    myCC.doneCompose(skip)
    myCC.endGroup()
}

interface ComponentUpdater<C>: ViewComposition {
    val component: C
    var changed: Boolean
}

inline fun <C, reified V> ComponentUpdater<C>.set(value: V, noinline block: C.(V) -> Unit) {
    if (cc.changed(value)) {
        cc.apply<V, C>(value) { component.block(it) }
        component.block(value)
        changed = true
    }
}

inline fun <C, reified V> ComponentUpdater<C>.update(value: V, noinline block: C.(V) -> Unit) {
    if (cc.applyNeeded(value)) {
        cc.apply<V, C>(value) { component.block(it) }
        component.block(value)
        changed = true
    }
}

inline fun <C, V> ComponentUpdater<C>.change(value: V, block: C.(V) -> Unit) {
    component.block(value)
    changed = true
}

inline fun <reified C:ViewComponent> ViewComposition.composeComponent(key: Any, crossinline factory: () -> C, crossinline block: ComponentUpdater<C>.() -> Unit) {
    val myCC = cc
    myCC.startGroup(key)
    val component = myCC.remember { factory().apply { setComposition(myCC)} }
    val updater = object : ComponentUpdater<C> {
        override val cc = myCC
        override var changed: Boolean = false
        override val component = component
    }
    if (myCC.inserting) updater.changed = true
    updater.block()
    val skip = !updater.changed
    myCC.startCompose(skip, component)
    if (!skip) component.compose()
    myCC.doneCompose(skip)
    myCC.endGroup()
}