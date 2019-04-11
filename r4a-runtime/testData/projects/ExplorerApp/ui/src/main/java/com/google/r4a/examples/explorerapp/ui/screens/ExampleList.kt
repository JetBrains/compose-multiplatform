package com.google.r4a.examples.explorerapp.ui.screens

import android.os.Bundle
import com.google.r4a.*
import androidx.ui.androidview.adapters.*
import android.widget.*
import android.widget.LinearLayout.VERTICAL
import com.google.r4a.examples.explorerapp.common.adapters.Ambients
import com.google.r4a.examples.explorerapp.ui.R

@Composable
fun ExampleList() {
    <LinearLayout orientation=VERTICAL>
        <TextView
            textSize=26.sp
            text="Select Example"
            paddingBottom=6.pt
        />

        val navigator = +ambient(Ambients.NavController)
        for (example in EXAMPLES) {
            <Button
                text=example
                onClick={
                    val bundle = Bundle()
                    bundle.putString(EXAMPLE_NAME, example)
                    navigator.navigate(R.id.nav_to_example, bundle)
                } />
        }
    </LinearLayout>
}