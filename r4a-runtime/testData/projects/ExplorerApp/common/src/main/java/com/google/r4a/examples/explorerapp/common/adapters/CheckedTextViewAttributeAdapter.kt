package com.google.r4a.examples.explorerapp.common.adapters

import android.widget.CheckedTextView

fun CheckedTextView.setCheckmarkGravity(gravity: Int) {
    // NOTE(lmr): Somewhat surprisingly, this is the first example i've found of an attribute that's assignable from
    // XML but not programmatically. We can get around it with Reflection here but provide a real method in the
    // framework?
    val cls = CheckedTextView::class.java
    val field = cls.getDeclaredField("mCheckMarkGravity")
    field.setAccessible(true)
    field.setInt(this, gravity)
}