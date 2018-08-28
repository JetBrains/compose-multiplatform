package com.google.r4a.examples.explorerapp.common.adapters

import android.content.Context
import android.view.View
import com.google.r4a.Ambient
import com.google.r4a.Component
import com.google.r4a.CompositionContext
//
//
//inline fun <reified T: Component> CompositionContext.emitComponent(ctor: () -> T, block: (T) -> Unit) {
//    var el = start(0) as? T
//    if (el == null) {
//        el = ctor()
//        setInstance(el)
//    }
//    block(el)
//    compose()
//    end()
//}
//
//inline fun <reified T: Component> CompositionContext.emitComponent(ctor: () -> T) {
//    var el = start(0) as? T
//    if (el == null) {
//        el = ctor()
//        setInstance(el)
//    }
//    compose()
//    end()
//}
//
//inline fun <reified T: View> CompositionContext.emitView(ctor: (context: Context) -> T, block: (T) -> Unit) {
//    var el = start(0) as? T
//    if (el == null) {
//        el = ctor(context)
//        setInstance(el)
//    }
//    block(el)
//    end()
//}
//
//inline fun <reified T: View> CompositionContext.emitView(ctor: (context: Context) -> T) {
//    var el = start(0) as? T
//    if (el == null) {
//        el = ctor(context)
//        setInstance(el)
//    }
//    end()
//}
//
//inline fun <reified T, V> CompositionContext.set(component: T, value: V, setter: T.(V) -> Unit) {
//    if (updateAttribute(value)) {
//        component.setter(value)
//    }
//}
//
//fun CompositionContext.emitFn(fn: () -> Unit)= emitAnyFn(fn)
//fun <T> CompositionContext.emitFn(fn: (T) -> Unit, arg: T) = emitAnyFn(fn, arg)
//fun <T1, T2> CompositionContext.emitFn(fn: (T1, T2) -> Unit, arg1: T1, arg2: T2) = emitAnyFn(fn, arg1, arg2)
//fun <T1, T2, T3> CompositionContext.emitFn(fn: (T1, T2, T3) -> Unit, arg1: T1, arg2: T2, arg3: T3) = emitAnyFn(fn, arg1, arg2, arg3)
//
//fun CompositionContext.emitAnyFn(component: Any, vararg args: Any?) {
//    start(0)
//    setInstance(component)
//    for (arg in args) {
//        updateAttribute(arg)
//    }
//    compose()
//    end()
//}
//
//inline fun <reified T> CompositionContext.provideAmbient(key: Ambient<T>, value: T, noinline children: () -> Unit) {
//    var el = start(0) as? Ambient<T>.Provider
//    if (el == null) {
//        el = key.Provider(value, children)
//        setInstance(el)
//    }
//    set(el, value) { this.value = it }
//    set(el, children) { this.children = it }
//    compose()
//    end()
//}
//
//inline fun <reified T> CompositionContext.consumeAmbient(key: Ambient<T>, noinline children: (T) -> Unit) {
//    var el = start(0) as? Ambient<T>.Consumer
//    if (el == null) {
//        el = key.Consumer()
//        setInstance(el)
//    }
//    set(el, children) { this.children = it }
//    compose()
//    end()
//}
//
//
//fun CompositionContext.portal(children: (Ambient.Reference) -> Unit) {
//    emitComponent({ Ambient.Portal() }) {
//        set(it, children) { this.children = it }
//    }
//}