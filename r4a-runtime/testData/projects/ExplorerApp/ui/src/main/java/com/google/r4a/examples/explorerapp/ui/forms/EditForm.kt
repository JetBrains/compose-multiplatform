package com.google.r4a.examples.explorerapp.ui.forms

import android.widget.*
import com.google.r4a.*
import com.google.r4a.adapters.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.common.data.AllRegions
import com.google.r4a.examples.explorerapp.ui.R
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

class EditForm : Component() {
    private var name: String = ""

    private var checked: Boolean = false

    private var progress: Int = 15

    private var checkedId: Int = R.id.rb_one
    private var region: String = ""
    private var hour: Int = 6
    private var minute: Int = 22

    override fun compose() {
        <ScrollView layoutWidth=MATCH_PARENT layoutHeight=MATCH_PARENT>
        <LinearLayout orientation=LinearLayout.VERTICAL>

            <Spinner
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                data=AllRegions
            />

            // TODO(lmr): we should create a version of this where the items are rendered as children
            <AutoCompleteTextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                controlledText=region
                onTextChange={ value ->
                    region = value
                    recomposeSync()
                }
                data=AllRegions
                composeItem={
                    <TextView text=(it as String) />
                }
            />

            <TimePicker
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                hour=hour
                minute=minute
                onTimeChanged={ _, hourOfDay, minuteOfHour->
                    hour = hourOfDay
                    minute = minuteOfHour
                    recompose()
                }
            />


            <EditText
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                controlledText=name
                onTextChange={ value ->
                    name = value.substring(0, Math.min(value.length, 10))
                    recomposeSync()
                }
            />
            <TextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                text=name
            />

            <ToggleButton
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                controlledChecked=checked
                onCheckedChange={
                    checked = it
                    recomposeSync()
                }
            />
            <TextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                text="is checked: $checked"
            />


            <SeekBar
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                controlledProgress=progress
                onProgressChange={
                    progress = Math.min(it, 70)
                    recomposeSync()
                }
            />
            <TextView
                layoutWidth=MATCH_PARENT
                layoutHeight=WRAP_CONTENT
                text="value: $progress"
            />


            // TODO(lmr): we could build a better RadioGroup component
            <RadioGroup
                controlledCheckedId=checkedId
                onCheckedIdChange={
                    checkedId = it
                    recomposeSync()
                }
            >
                <RadioButton id=R.id.rb_one text="Choice one"/>
                <RadioButton id=R.id.rb_two text="Choice two"/>
                <RadioButton id=R.id.rb_three text="Choice three can't be selected"/>
            </RadioGroup>
        </LinearLayout>
        </ScrollView>
    }
}