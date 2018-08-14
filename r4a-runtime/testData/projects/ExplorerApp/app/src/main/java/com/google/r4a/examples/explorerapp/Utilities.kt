package com.google.r4a.examples.explorerapp

import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import com.google.r4a.Composable
import com.google.r4a.composeInto

fun AppCompatActivity.content(content: @Composable() () -> Unit) {
    val root = LinearLayout(this)
    root.composeInto(content)
    setContentView(root)
}
