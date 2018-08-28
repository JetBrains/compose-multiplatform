//package com.google.r4a.examples.explorerapp.common.data
//
//import android.widget.TextView
//
//fun <T> emit(ctor: () -> T, /* objargs */ setters: T.() -> Unit) {}
//
//
//operator fun meminvoke() {}
//
//class Entity {
//    var text: String = ""
//    var enabled: Boolean = false
//}
//
//fun doStuff() {
//
//    emit(::TextView,
//        text = "foo",
//        enabled = false
//    )
//
//    Entity::(text = "foo", enabled = false) {
//
//    }
//}