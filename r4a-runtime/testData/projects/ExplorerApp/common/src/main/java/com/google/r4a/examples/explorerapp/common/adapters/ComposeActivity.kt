package com.google.r4a.examples.explorerapp.common.adapters

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import com.google.r4a.*

// TODO(lmr): ComposeNavigationActivity should potentially either subclass this, or should replace this class
abstract class ComposeActivity: AppCompatActivity() {
    abstract fun compose()
    private lateinit var root: LinearLayout

    @Suppress("PLUGIN_WARNING")
    private val rootCompose = {
        val cc = CompositionContext.current

        cc.provideAmbient(Ambients.Activity, this) {
            cc.provideAmbient(Ambients.Context, this) {
                cc.provideAmbient(Ambients.Application, application) {
                    cc.provideAmbient(Ambients.Configuration, this.resources.configuration) {
                        cc.group(0) { compose() }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        root.composeInto(rootCompose)
        setContentView(root)
    }


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

}