package com.google.r4a.examples.explorerapp.ui.calculator

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.r4a.*
import androidx.ui.androidview.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class Calculator {

    var formula = CalculatorFormula()

    private fun fontSize(display: String): Int {
        return when {
            display.length <= 8 -> 32
            display.length <= 11 -> 24
            else -> 12
        }
    }

    @Composable
    operator fun invoke() {
        <Recompose> recompose ->
            val onClick = { it: String -> formula.append(it);  recompose() }
            <LinearLayout orientation=LinearLayout.VERTICAL layoutWidth=MATCH_PARENT layoutHeight=MATCH_PARENT>
                <LinearLayout
                    orientation=LinearLayout.VERTICAL
                    layoutWidth=MATCH_PARENT
                    layoutHeight=WRAP_CONTENT
                    elevation=4.dip
                    backgroundColor=Color.WHITE
                >
                    <Toolbar popupTheme=R.style.ThemeOverlay_Popup layoutWidth=MATCH_PARENT layoutHeight=WRAP_CONTENT>
                        <TextView
                            layoutWidth=WRAP_CONTENT
                            layoutHeight=WRAP_CONTENT
                            allCaps=true
                            textSize=15.sp
                            typeface=Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            text="RAD"
                            contentDescription="radian mode"
                        />
                    </Toolbar>

                    <TextView
                        layoutWidth=MATCH_PARENT layoutHeight=MATCH_PARENT
                        ellipsize=TextUtils.TruncateAt.END
                        maxLines=1
                        textColor=Color.BLACK
                        textSize=18.sp
                        typeface=Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        text=formula.formulaString
                        bufferType=TextView.BufferType.EDITABLE
                        cursorVisible=true
                        gravity=(Gravity.BOTTOM or Gravity.RIGHT)
                        focusableInTouchMode=true
                    />
                    <TextView
                        layoutWidth=MATCH_PARENT layoutHeight=MATCH_PARENT
                        singleLine=true
                        text=formula.previewString
                        bufferType=TextView.BufferType.SPANNABLE
                        typeface=Typeface.create("sans-serif-light", Typeface.NORMAL)
                        textSize=18.sp
                        paddingHorizontal=3.dip
                        paddingVertical=6.dip
                        gravity=(Gravity.BOTTOM or Gravity.RIGHT)
                        cursorVisible=true
                    />
                </LinearLayout>
                <LinearLayout layoutWidth=MATCH_PARENT layoutHeight=MATCH_PARENT>
                    <GridLayout
                        layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 7f)
                        layoutWidth=0.dp
                        layoutHeight=MATCH_PARENT
                        layoutWeight=7f
                        rowCount=4
                        columnCount=3
                        backgroundColor="#434343"
                    >
                        listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0", "=").forEachIndexed { index, value ->
                            <CalculatorButton
                                value=value
                                onClick
                                layoutParams=grid(index / 3, index % 3)
                            />
                        }
                    </GridLayout>
                    <GridLayout
                        rowCount=5
                        columnCount=1
                        backgroundColor="#636363"
                        layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3f)
                    >
                        listOf(CalculatorFormula.DELETE, CalculatorFormula.DIVIDE, CalculatorFormula.MULTIPLY,
                                CalculatorFormula.SUBTRACT, CalculatorFormula.ADD).forEachIndexed { index, value ->
                            <CalculatorButton value onClick layoutParams=grid(index, 0) />
                        }
                    </GridLayout>
                </LinearLayout>
            </LinearLayout>
        </Recompose>
    }

    /** Helper function to make it easier to create GridLayoutParams for the calculator buttons **/
    fun grid(row: Int, column: Int) = GridLayout.LayoutParams(
            GridLayout.spec(row, GridLayout.CENTER, 1f),
            GridLayout.spec(column, GridLayout.CENTER, 0f)
    )
}


@Composable
private fun CalculatorButton(
    value: String,
    layoutParams: GridLayout.LayoutParams,
    onClick: (op: String) -> Unit
) {
    <Button
        text=value
        layoutParams=layoutParams
        allCaps=true
        gravity=Gravity.CENTER
        includeFontPadding=false
        typeface=Typeface.create("sans-serif-light", Typeface.NORMAL)
        textSize=21.sp
        textColor=Color.WHITE
        backgroundResource=R.drawable.pad_button_background
        onClick={ onClick(value) }
    />
}
