package com.google.r4a.examples.explorerapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import com.google.r4a.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.composeInto(object: Function0<Unit> {
            override fun invoke() {
                <MainComponent />
            }
        })
        setContentView(root)
    }
}
