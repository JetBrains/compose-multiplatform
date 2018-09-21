package com.google.r4a.examples.explorerapp.ui.components

import android.view.Gravity
import android.view.View
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.Colors
import com.google.r4a.examples.explorerapp.ui.R
import com.makeramen.roundedimageview.RoundedImageView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class RedditDrawer : Component() {
    private var children: (() -> Unit)? = null
    fun setChildrenBlock(fn: () -> Unit) {
        children = fn
    }

    private val drawerRef = Ref<DrawerLayout>()

    override fun compose() {
        val children = children ?: error("expected children")
        <DrawerLayout
            ref=drawerRef
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
            fitsSystemWindows=true
        >
            <children />

            val navigator = CompositionContext.current.getAmbient(Ambients.NavController)
            <NavigationView
                layoutWidth=WRAP_CONTENT
                layoutHeight=MATCH_PARENT
                layoutGravity=Gravity.START
                header={
                    <LinearLayout
                        orientation=LinearLayout.VERTICAL
                        layoutWidth=MATCH_PARENT
                        layoutHeight=192.dp
                        backgroundColor=Colors.PRIMARY
                        padding=16.dp
                        gravity=Gravity.BOTTOM
                    >
                        <RoundedImageView
                            layoutWidth=64.dp
                            layoutHeight=64.dp
                            uri="https://avatars1.githubusercontent.com/u/1885623?s=460&v=4"
                            cornerRadius=32.dp
                            borderColor=Colors.WHITE
                            borderWidth=2.dp />
                        <TextView
                            layoutWidth=MATCH_PARENT
                            layoutHeight=WRAP_CONTENT
                            paddingTop=8.dp
                            textColor=Colors.WHITE
                            textSize=8.sp
                            text="lrichardson" />
                        <TextView
                            layoutWidth=MATCH_PARENT
                            layoutHeight=WRAP_CONTENT
                            textColor=Colors.WHITE
                            textSize=6.sp
                            fontStyle=Typeface.ITALIC
                            text="Logged In" />
                    </LinearLayout>
                }
                menu=R.menu.drawer_view
                onNavigationItemSelected={ item ->
                    val bundle = Bundle()
                    val drawerLayout = drawerRef.value ?: error("expected drawerLayout to be there")
                    when (item.getItemId()) {
                        R.id.nav_home -> {
                            drawerLayout.closeDrawer(Gravity.START, true)
                            navigator.navigate(R.id.nav_to_home, bundle)
                            true
                        }
                        R.id.nav_ui_examples -> {
                            drawerLayout.closeDrawer(Gravity.START, true)
                            navigator.navigate(R.id.nav_to_examples, bundle)
                            true
                        }
                        R.id.nav_settings -> {
                            drawerLayout.closeDrawer(Gravity.START, true)
                            navigator.navigate(R.id.nav_to_settings, bundle)
                            true
                        }
                        R.id.nav_log_out -> {
                            // TODO(lmr): when we set up authentication and stuff, we'll want to add something here
                            false
                        }
                        else -> false
                    }
                } />
        </DrawerLayout>
    }
}
