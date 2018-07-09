package com.google.r4a.examples.explorerapp

import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.screens.*


class ExampleFragment: ComposeFragment() {
    override fun compose() {
        with(CompositionContext.current) {
            emitComponent(0, ::ExamplePage)
        }
    }
}