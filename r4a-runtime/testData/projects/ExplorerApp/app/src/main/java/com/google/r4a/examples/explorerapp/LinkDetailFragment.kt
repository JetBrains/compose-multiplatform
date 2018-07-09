package com.google.r4a.examples.explorerapp

import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.ComposeFragment
import com.google.r4a.examples.explorerapp.common.data.Link
import com.google.r4a.examples.explorerapp.ui.screens.LinkDetailScreen

class LinkDetailFragment: ComposeFragment() {
    override fun compose() {
        // TODO(lmr): Grab more properties off of the bundle to pass into the detail screen so that we can render
        // a preview of the link synchronously before making a network request
        val id = arguments?.getString("id")!!
        val initialLink = arguments?.getParcelable<Link>("initialLink")
        with(CompositionContext.current) {
            emitComponent(0, ::LinkDetailScreen) {
                // TODO(lmr): for some reason the property setter version of this broke IR. It's not entirely clear why
                set(id) { setId(it) }
                set(initialLink) { setInitialLink(it) }
            }
        }
    }
}