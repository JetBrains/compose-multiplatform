package com.google.r4a.examples.explorerapp

import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*

class SettingsFragment: ComposeFragment() {
    override fun compose() {
        with(composer) {
            emit(
                    0,
                    { c -> TextView(c) },
                    { set("SettingsFragment") { text = it } }
            )
        }
    }
}