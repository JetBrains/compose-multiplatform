package com.google.r4a.examples.explorerapp.ui.screens

import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.adapters.*

class FontList : Component() {

    private var textSize = 20f
    private var color = Color.GRAY

    override fun compose() {
        <LinearLayout orientation=LinearLayout.VERTICAL layoutParams=FILL>
            <Button text="Increase Font Size" onClick={ textSize += 1; recompose() } enabled=(textSize < 100) />
            <Button text="Decrease Font Size" onClick={ textSize -= 1; recompose() } enabled=(textSize > 10) />
            <LinearLayout orientation=LinearLayout.HORIZONTAL>
                <Button text="Red" layoutParams=BUTTON onClick={ color = Color.RED; recompose() } />
                <Button text="Green" layoutParams=BUTTON onClick={ color = Color.GREEN; recompose() } />
                <Button text="Blue" layoutParams=BUTTON onClick={ color = Color.BLUE; recompose() } />
                <Button text="Gray" layoutParams=BUTTON onClick={ color = Color.GRAY; recompose() } />
            </LinearLayout>
            <ChildComponent textSize color />
        </LinearLayout>
    }

//    companion object {
    private val BUTTON = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
    private val FILL = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
//    }

    private class ChildComponent : Component() {
        var textSize: Float = 20f
        var color: Int = Color.RED

        override fun compose() {
            <ScrollView>
                <LinearLayout orientation=LinearLayout.VERTICAL>
                    for (x in 1..21) {
                        <TextView text="Hello Someone $x!" textSize textColor=color />
                    }
                </LinearLayout>
            </ScrollView>
        }
    }
}


