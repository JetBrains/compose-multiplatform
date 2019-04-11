package com.google.r4a.examples.explorerapp.ui.screens

import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.*
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.ScrollView
import android.widget.TextView
import com.google.r4a.*
import androidx.ui.androidview.adapters.*

@Model
class FontListModel(
    var textSize: Int = 20,
    var color: Int = Color.GRAY
)

@Composable
fun FontList(model: FontListModel = FontListModel()) {
    <Observe>
        <LinearLayout
            orientation=VERTICAL
            layoutWidth=MATCH_PARENT
            layoutHeight=MATCH_PARENT
            layoutWeight=1f
        >
            <Button
                text="Increase Font Size"
                onClick={ model.textSize += 1 }
                enabled=( model.textSize < 100 ) />
            <Button
                text="Decrease Font Size"
                onClick={ model.textSize -= 1 }
                enabled=( model.textSize > 10 ) />
            <LinearLayout orientation=HORIZONTAL>
                <Button
                    text="Red"
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutWeight=1f
                    onClick={ model.color = Color.RED } />
                <Button
                    text="Green"
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutWeight=1f
                    onClick={ model.color = Color.GREEN } />
                <Button
                    text="Blue"
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutWeight=1f
                    onClick={ model.color = Color.BLUE } />
                <Button
                    text="Gray"
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    layoutWeight=1f
                    onClick={ model.color = Color.GRAY } />
            </LinearLayout>
            <ChildComponent textSize=model.textSize.sp color=model.color />
        </LinearLayout>
    </Observe>
}

@Composable
private fun ChildComponent(textSize: Dimension = 20.sp, color: Int = Color.RED) {
    <ScrollView>
        <LinearLayout orientation=VERTICAL>
            for (x in 1..21) {
                <TextView text="Hello Someone $x!" textSize textColor=color />
            }
        </LinearLayout>
    </ScrollView>
}


