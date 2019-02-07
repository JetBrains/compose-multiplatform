package com.google.r4a.examples.explorerapp.ui.screens

import android.os.Bundle
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.examples.explorerapp.common.adapters.Ambients
import com.google.r4a.examples.explorerapp.common.data.Link
import com.google.r4a.examples.explorerapp.ui.R
import com.google.r4a.unaryPlus

@Composable
fun PostListItem(link: Link) {
    val navigator = +ambient(Ambients.NavController)
    <LinkCard
        link=link
        onClick={
            val bundle = Bundle()

            bundle.putString("id", link.id)
            bundle.putParcelable("initialLink", link)

            navigator.navigate(R.id.nav_list_to_detail, bundle)
        }
    />
}