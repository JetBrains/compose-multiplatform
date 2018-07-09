package com.google.r4a.examples.explorerapp.common.adapters

import android.support.design.widget.NavigationView
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.r4a.R4a


fun NavigationView.setMenu(resId: Int) = inflateMenu(resId)

fun NavigationView.setHeader(resId: Int) = inflateHeaderView(resId)

fun NavigationView.setHeader(composable: () -> Unit) {
    if (headerCount == 0) {
        addHeaderView(LinearLayout(context))
    }
    val root = getHeaderView(0) as ViewGroup
    // TODO(lmr): find out a good way to pass ambients. If we use context, I think this should just fall out!
    R4a.composeInto(root, null, composable)
}

//fun NavigationView.setHeadser(x: Int {
//    onitem
//}
