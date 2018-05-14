package com.google.r4a.examples.explorerapp.calculator

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.r4a.Component
import com.google.r4a.examples.explorerapp.R
import com.google.r4a.AttributeAdapterLocal

class Calculator : Component() {

    var formula = CalculatorFormula()

    private val FILL = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    private val WRAP_HORIZ = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private val WRAP = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun compose() {
        <LinearLayout orientation="vertical" layoutParams={FILL}>
            <LinearLayout
                orientation="vertical"
                layoutParams={WRAP_HORIZ}
                elevation="4dip"
                backgroundColor={Color.WHITE}
            >
                <Toolbar popupTheme={R.style.ThemeOverlay_Popup} layoutParams={WRAP_HORIZ}>
                    <TextView
                        layoutParams={WRAP}
                        allCaps={true}
                        textSize="6sp"
                        typeface={Typeface.create("sans-serif-medium", Typeface.NORMAL)}
                        text="RAD"
                        contentDescription="radian mode"
                    />
                </Toolbar>

                <TextView
                    layoutParams={FILL}
                    ellipsize={TextUtils.TruncateAt.END}
                    maxLines={1}
                    textColor={Color.BLACK}
                    textSize="7dip"
                    typeface={Typeface.create("sans-serif-medium", Typeface.NORMAL)}
                    text={formula.formulaString}
                    bufferType={TextView.BufferType.EDITABLE}
                    cursorVisible={true}
                    gravity={Gravity.BOTTOM or Gravity.RIGHT}
                    focusableInTouchMode={true}
                />
                <TextView
                    layoutParams={FILL}
                    singleLine={true}
                    text={formula.previewString}
                    bufferType={TextView.BufferType.SPANNABLE}
                    typeface={Typeface.create("sans-serif-light", Typeface.NORMAL)}
                    textSize="7dip"
                    paddingHorizontal="3dip"
                    paddingVertical="6dip"
                    gravity={Gravity.BOTTOM or Gravity.RIGHT}
                    cursorVisible={true}
                />
            </LinearLayout>
            <LinearLayout layoutParams={FILL}>
                <GridLayout
                    layoutParams={LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 7f)}
                    rowCount={4}
                    columnCount={3}
                    backgroundColor="#434343"
                >
                    <CalculatorButton formula={formula} value="7" layoutParams={grid(0, 0)} />
                    <CalculatorButton formula={formula} value="8" layoutParams={grid(0, 1)} />
                    <CalculatorButton formula={formula} value="9" layoutParams={grid(0, 2)} />

                    <CalculatorButton formula={formula} value="4" layoutParams={grid(1, 0)} />
                    <CalculatorButton formula={formula} value="5" layoutParams={grid(1, 1)} />
                    <CalculatorButton formula={formula} value="6" layoutParams={grid(1, 2)} />

                    <CalculatorButton formula={formula} value="1" layoutParams={grid(2, 0)} />
                    <CalculatorButton formula={formula} value="2" layoutParams={grid(2, 1)} />
                    <CalculatorButton formula={formula} value="3" layoutParams={grid(2, 2)} />

                    <CalculatorButton formula={formula} value="." layoutParams={grid(3, 0)} />
                    <CalculatorButton formula={formula} value="0" layoutParams={grid(3, 1)} />
                    <CalculatorButton formula={formula} value="=" layoutParams={grid(3, 2)} />
                </GridLayout>
                <GridLayout
                    rowCount={5}
                    columnCount={1}
                    backgroundColor="#636363"
                    layoutParams={LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3f)}
                >
                    <CalculatorButton formula={formula} value={CalculatorFormula.DELETE} layoutParams={grid(0, 0)} />
                    <CalculatorButton formula={formula} value={CalculatorFormula.DIVIDE} layoutParams={grid(1, 0)} />
                    <CalculatorButton formula={formula} value={CalculatorFormula.MULTIPLY} layoutParams={grid(2, 0)} />
                    <CalculatorButton formula={formula} value={CalculatorFormula.SUBTRACT} layoutParams={grid(3, 0)} />
                    <CalculatorButton formula={formula} value={CalculatorFormula.ADD} layoutParams={grid(4, 0)} />
                </GridLayout>
            </LinearLayout>
        </LinearLayout>
    }

    /** Helper function to make it easier to create GridLayoutParams for the calculator buttons **/
    fun grid(row: Int, column: Int) = GridLayout.LayoutParams(
            GridLayout.spec(row, GridLayout.CENTER, 1f),
            GridLayout.spec(column, GridLayout.CENTER, 0f)
    )
}

private class CalculatorButton : Component() {

    lateinit var formula: CalculatorFormula
    lateinit var value: String
    lateinit var layoutParams: GridLayout.LayoutParams

    private val onClick = object: View.OnClickListener {
        override fun onClick(v: View?) {
            formula.append(value)
            recompose()
        }
    }

    override fun compose() {
        // TODO(lmr): for some reason this won't type check correctly if we put it in the attribute...
//        val textSize = if(CalculatorFormula.isOperator(value) || CalculatorFormula.DELETE.equals(value)) "7dip" else "7dip"
        <Button
            text={value}
            layoutParams={layoutParams}
            allCaps={true}
            gravity={Gravity.CENTER}
            includeFontPadding={false}
            typeface={Typeface.create("sans-serif-light", Typeface.NORMAL)}
            textSize="8sp"
            textColor={Color.WHITE}
            backgroundResource={R.drawable.pad_button_background}
            onClickListener={onClick}
        />
    }
}
