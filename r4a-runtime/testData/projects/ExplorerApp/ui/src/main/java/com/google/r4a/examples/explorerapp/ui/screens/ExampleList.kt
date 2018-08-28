package com.google.r4a.examples.explorerapp.ui.screens

import android.os.Bundle
import com.google.r4a.*
import com.google.r4a.adapters.*
import android.widget.*
import android.widget.LinearLayout.VERTICAL
import com.google.r4a.examples.explorerapp.common.adapters.Ambients
import com.google.r4a.examples.explorerapp.ui.R

class ExampleList : Component() {
    private val navigator get() = CompositionContext.getAmbient(Ambients.NavController, this)
    override fun compose() {
        <LinearLayout orientation=VERTICAL>
            <TextView
                textSize=10.sp
                text="Select Example"
                paddingBottom=6.pt
            />

            for (example in EXAMPLES) {
                <Button
                    text=example
                    onClick={
                        val bundle = Bundle()
                        bundle.putString(EXAMPLE_NAME, example)
                        navigator.navigate(R.id.nav_to_example, bundle)
                    }
                />
            }
        </LinearLayout>
    }
}