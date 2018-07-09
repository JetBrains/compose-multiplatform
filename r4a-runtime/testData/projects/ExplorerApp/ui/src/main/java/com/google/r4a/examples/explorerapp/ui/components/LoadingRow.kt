package com.google.r4a.examples.explorerapp.ui.components

import android.view.Gravity
import com.google.r4a.*
import com.google.r4a.adapters.*
import android.widget.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class LoadingRow : Component() {
    override fun compose() {
        <ProgressBar
            indeterminate=true
            layoutWidth=MATCH_PARENT
            layoutHeight=WRAP_CONTENT
            layoutGravity=Gravity.CENTER_HORIZONTAL
            paddingVertical=24.dp
        />
    }
}