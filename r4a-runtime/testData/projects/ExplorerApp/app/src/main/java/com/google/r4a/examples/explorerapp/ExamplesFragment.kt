package com.google.r4a.examples.explorerapp

import com.google.r4a.composer
import com.google.r4a.examples.explorerapp.common.adapters.ComposeFragment

class ExamplesFragment: ComposeFragment() {
    override fun compose() {
        with(composer) {
            call(
                    0,
                    { true },
                    { ExampleList() }
            )
        }
    }
}