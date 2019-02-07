package com.google.r4a.examples.explorerapp.common.adapters

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.util.Log
import com.google.r4a.*

fun <T> subscribe(data: LiveData<T>) = effectOf<T?> {
    val current = +stateFor(data) { data.value }

    +onCommit(data) {
        val observer = Observer<T> {
            current.value = data.value
        }
        data.observeForever(observer)
        onDispose {
            data.removeObserver(observer)
        }
    }

    current.value
}