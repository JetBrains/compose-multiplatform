package com.google.r4a.examples.explorerapp

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.r4a.Component
import com.google.r4a.CompositionContext
import com.google.r4a.CompositionContextImpl

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CompositionContext.factory = CompositionContextImpl.factory
        CompositionContext.current = CompositionContextImpl.DUMMY
        setContentView(MainComponent.createInstance(this))
    }
}
