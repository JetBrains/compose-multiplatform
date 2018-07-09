package com.google.r4a.examples.explorerapp

import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.calculator.Calculator
import com.google.r4a.examples.explorerapp.ui.screens.ExampleList
import com.google.r4a.examples.explorerapp.ui.screens.LinkListScreen

class ExamplesFragment: ComposeFragment() {
    override fun compose() {
        with(CompositionContext.current) {
            emitComponent(0, ::ExampleList)
        }
    }
}