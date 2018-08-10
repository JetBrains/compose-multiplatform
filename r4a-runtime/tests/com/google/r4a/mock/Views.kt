/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a.mock

class SourceLocation(val name: String) {
    override fun toString(): String = "SL $name"
}

val repeat = SourceLocation("repeat")
inline fun <T:Any> ViewComposition.repeat(of: Iterable<T>, crossinline block: ViewComposition.(value: T) -> Unit) {
    for (value in of) {
        cc.startGroup(cc.joinKey(repeat, value))
        block(value)
        cc.endGroup()
    }
}

val linear = SourceLocation("linear")
fun ViewComposition.linear(block: Compose) {
    emit(linear, {View().apply { name = "linear"} }, block)
}

val text = SourceLocation("text")
fun ViewComposition.text(value: String) {
    emit(text, { View().apply { name = "text"} }, value, { attribute("text", it ) })
}

val edit = SourceLocation("edit")
fun ViewComposition.edit(value: String) {
    emit(edit, { View().apply { name = "edit"} }, value, { attribute("value", it ) })
}

val box = SourceLocation("box")
fun ViewComposition.selectBox(selected: Boolean, block: Compose) {
    if (selected) {
        emit(box, { View().apply { name = "box"} }, block)
    } else {
        block()
    }
}

