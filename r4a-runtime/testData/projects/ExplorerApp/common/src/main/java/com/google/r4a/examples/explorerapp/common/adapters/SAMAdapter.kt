package com.google.r4a.examples.explorerapp.common.adapters

import android.support.design.widget.NavigationView
import android.support.design.widget.TextInputEditText
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.TimePicker

fun View.setOnClick(cb: ((v: View?) -> Unit)?)
    = setOnClickListener(cb)
fun NavigationView.setOnNavigationItemSelected(cb: ((MenuItem) -> Boolean)?)
    = setNavigationItemSelectedListener(cb)
fun TextInputEditText.setOnEditorAction(cb: (v: TextView?, actionId: Int, event: KeyEvent?) -> Boolean)
    = setOnEditorActionListener(cb)
fun TimePicker.setOnTimeChanged(cb: (view: TimePicker?, hourOfDay: Int, minuteOfHour: Int) -> Unit)
    = setOnTimeChangedListener(cb)













