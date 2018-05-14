package com.google.r4a.examples.explorerapp.screens

import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.r4a.Component

class FontList : Component() {

    private var textSize = 20f
    private var color = Color.GRAY

    override fun compose() {
        <LinearLayout orientation={LinearLayout.VERTICAL} layoutParams={FILL}>
            <Button text="Increase Font Size" onClickListener={SetFontSizeHandler(textSize+1, this)} enabled={textSize < 100} />
            <Button text="Decrease Font Size" onClickListener={SetFontSizeHandler(textSize-1, this)} enabled={textSize > 10} />
            <LinearLayout orientation={LinearLayout.HORIZONTAL}>
                <Button text="Red" layoutParams={BUTTON} onClickListener={SetColorHandler(Color.RED, this)} />
                <Button text="Green" layoutParams={BUTTON} onClickListener={SetColorHandler(Color.GREEN, this)} />
                <Button text="Blue" layoutParams={BUTTON} onClickListener={SetColorHandler(Color.BLUE, this)} />
                <Button text="Gray" layoutParams={BUTTON} onClickListener={SetColorHandler(Color.GRAY, this)} />
            </LinearLayout>
            <ChildComponent textSize={textSize} color={color} />
        </LinearLayout>
    }

    class SetFontSizeHandler(val fontSize: Float, val component: FontList) : View.OnClickListener {
        override fun onClick(v: View?){
            component.textSize = fontSize
            component.recompose()
        }
    }
    class SetColorHandler(val color: Int, val component: FontList) : View.OnClickListener {
        override fun onClick(v: View?) {
            component.color = color
            component.recompose()
        }
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
                <LinearLayout orientation={LinearLayout.VERTICAL}>
                    for (x in 1..21) {
                        <TextView text="Hello Someone $x!" textSize={textSize} textColor={color} />
                    }
                </LinearLayout>
            </ScrollView>
        }
    }
}


