package com.google.r4a.examples.explorerapp.ui.screens

import android.widget.LinearLayout
import com.google.r4a.Component
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.data.Link

class LinkHeader(var link: Link) : Component() {
    override fun compose() {
        <LinearLayout paddingBottom=24.dp>
            <LinkCard link />
        </LinearLayout>
    }
}

