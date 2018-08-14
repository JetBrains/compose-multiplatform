package com.google.r4a.examples.explorerapp.forms

import android.support.annotation.RequiresApi
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.R
import com.google.r4a.examples.explorerapp.data.AllRegions

class EditForm : Component() {
    private val FILL = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    private val WRAP = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    private var name: String = ""

    private var checked: Boolean = false

    private var progress: Int = 15

    private var checkedId: Int = R.id.rb_one
    private var region: String = ""
    private var hour: Int = 6
    private var minute: Int = 22

    override fun compose() {
        <ScrollView layoutParams=FILL>
        <LinearLayout orientation="vertical">

            <Spinner
                layoutParams=WRAP
                data=AllRegions
            />

            // TODO(lmr): we should create a version of this where the items are rendered as children
            <AutoCompleteTextView
                layoutParams=WRAP
                controlledText=region
                onTextChange={ region = it; recomposeSync() }
                data=AllRegions
                composeItem={ <TextView text=(it as String) /> }
            />

            <TimePicker
                layoutParams=WRAP
                hour=hour
                minute=minute
                onTimeChanged={_, hourOfDay, minuteOfHour ->
                    hour = hourOfDay
                    minute = minuteOfHour
                    recompose()
                }
            />


            <EditText
                layoutParams=WRAP
                controlledText=name
                onTextChange={ name = it.substring(0, Math.min(it.length, 10)); recomposeSync(); }
            />
            <TextView layoutParams=WRAP text=name />

            <ToggleButton
                layoutParams=WRAP
                controlledChecked=checked
                onCheckedChange={ checked = it; recomposeSync() }

            />
            <TextView layoutParams=WRAP text="is checked: $checked" />


            <SeekBar
                layoutParams=WRAP
                controlledProgress=progress
                onProgressChange={ progress = Math.min(it, 70); recomposeSync() }
            />
            <TextView layoutParams=WRAP text="value: $progress" />


            // TODO(lmr): we could build a better RadioGroup component
            <RadioGroup
                controlledCheckedId=checkedId
                onCheckedIdChange={ checkedId = it; recomposeSync() }>
                <RadioButton id=R.id.rb_one text="Choice one"/>
                <RadioButton id=R.id.rb_two text="Choice two"/>
                <RadioButton id=R.id.rb_three text="Choice three can't be selected"/>
            </RadioGroup>
        </LinearLayout>
        </ScrollView>
    }
}