package com.google.r4a.examples.explorerapp.ui.screens

import android.widget.LinearLayout
import com.google.r4a.Composable
import androidx.ui.androidview.adapters.dp
import androidx.ui.androidview.adapters.setPaddingBottom
import com.google.r4a.composer
import com.google.r4a.examples.explorerapp.common.data.Link

@Composable
fun LinkHeader(link: Link) {
    <LinearLayout paddingBottom=24.dp>
        <LinkCard link />
    </LinearLayout>
}

