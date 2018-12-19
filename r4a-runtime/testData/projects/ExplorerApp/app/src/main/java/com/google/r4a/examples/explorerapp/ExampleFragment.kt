package com.google.r4a.examples.explorerapp

import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.screens.*


@Suppress("PLUGIN_ERROR")
class ExampleFragment: ComposeFragment() {
    override fun compose() {
        with(composer) {
            call(
                    0,
                    { ExamplePage() },
                    { true },
                    { f: ExamplePage -> f() }
            )
        }
    }
}