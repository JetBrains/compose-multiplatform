package com.google.r4a.examples.explorerapp

import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.ComposeFragment
import com.google.r4a.examples.explorerapp.ui.screens.LinkDetailScreen
import com.google.r4a.examples.explorerapp.ui.screens.LinkListScreen

class LinkListFragment: ComposeFragment() {
    override fun compose() {
        with(composer) {
            call(
                    0,
                    { LinkListScreen() },
                    { true },
                    { f -> f() }
            )
        }
    }
}