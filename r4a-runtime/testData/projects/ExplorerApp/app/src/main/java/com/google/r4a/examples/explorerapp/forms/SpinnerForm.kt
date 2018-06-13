package com.google.r4a.examples.explorerapp.forms

import android.view.View
import android.widget.*
import com.google.r4a.Component
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.R
import com.google.r4a.examples.explorerapp.data.AllRegions

class SpinnerForm : Component() {
    private val WRAP = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private var selectedIndex = 0

    override fun compose() {
        <LinearLayout orientation={LinearLayout.VERTICAL}>
            <Spinner
                layoutParams={WRAP}
                data={AllRegions}
                controlledSelectedIndex={selectedIndex}
                onSelectedIndexChange={object : Function1<Int, Unit> {
                    override fun invoke(p1: Int) {
                        selectedIndex = p1
                        recomposeSync()
                    }
                }}
            />
        </LinearLayout>
        <TextView text={AllRegions[selectedIndex]} />
    }
}