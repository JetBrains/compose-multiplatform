package com.google.r4a.examples.explorerapp

import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*

@Suppress("PLUGIN_ERROR")
class ExampleFragment: ComposeFragment() {
    override fun compose() {
        with(composer) {
            call(
                    0,
                    { true },
                    { ExamplePage() }
            )
        }
    }
}