package com.google.r4a.examples.explorerapp

import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.screens.LinkListScreen

class SettingsFragment: ComposeFragment() {
    override fun compose() {
        with(CompositionContext.current) {
            emitView(0, ::TextView) {
                set("SettingsFragment") { text = it }
            }
        }
    }
}