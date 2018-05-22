package com.google.r4a.examples.explorerapp

import android.app.Fragment
import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.infinitescroll.NewsFeed
import com.google.r4a.examples.explorerapp.screens.Reordering

class MainComponent : Component() {
    override fun compose() {
        // <Reordering />
//        <FontList />
//        <Calculator />
        <NewsFeed />
    }
}