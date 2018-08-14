package com.google.r4a.examples.explorerapp

import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*

class MainComponent : Component() {
    lateinit var selected: (String) -> Unit

    override fun compose() {
        <LinearLayout orientation="vertical">
            <TextView textSize=10.pt text="Select Example" paddingBottom=6.pt />


            for (example in examples) {
                <Button text=example onClick={ selected(example) } />
            }
        </LinearLayout>
    }
}