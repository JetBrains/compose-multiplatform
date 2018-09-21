package com.google.r4a.examples.explorerapp.ui.screens

import android.os.Bundle
import com.google.r4a.Component
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.Link
import com.google.r4a.examples.explorerapp.ui.R

class PostListItem(var link: Link) : Component() {
    override fun compose() {
        CompositionContext.current.consumeAmbient(Ambients.NavController) { navigator ->
            <LinkCard
                link=link
                onClick={
                    val bundle = Bundle()

                    bundle.putString("id", link.id)
                    bundle.putParcelable("initialLink", link)

                    navigator.navigate(R.id.nav_list_to_detail, bundle)
                } />
        }
    }
}