package com.google.r4a.examples.explorerapp.ui.forms

import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.data.AllRegions
import com.google.r4a.examples.explorerapp.common.adapters.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.google.r4a.CompositionContext.Companion.recomposeSync

class SpinnerForm : Component() {
    private var selectedIndex = 0

    override fun compose() {
        <LinearLayout orientation=LinearLayout.VERTICAL>
            <Spinner
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                data=AllRegions
                controlledSelectedIndex=selectedIndex
                onSelectedIndexChange={
                    selectedIndex = it
                    recomposeSync()
                }
            />
        </LinearLayout>
        <TextView text=AllRegions[selectedIndex] />
    }
}