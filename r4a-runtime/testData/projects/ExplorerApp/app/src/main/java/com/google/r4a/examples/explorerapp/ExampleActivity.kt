package com.google.r4a.examples.explorerapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.google.r4a.examples.explorerapp.calculator.Calculator
import com.google.r4a.examples.explorerapp.forms.EditForm
import com.google.r4a.examples.explorerapp.forms.SpinnerForm
import com.google.r4a.examples.explorerapp.infinitescroll.NewsFeed
import com.google.r4a.examples.explorerapp.screens.FontList
import com.google.r4a.examples.explorerapp.screens.Reordering

const val EXAMPLE_NAME = "com.google.r4a.exampleName"

const val CALCULATOR = "Calculator"
const val EDIT_FORM = "Edit Form"
const val NEWS_FEED = "News Feed"
const val FONT_LIST = "Font List"
const val SPINNER_FORM = "Spinner Form"
const val REORDERING = "Reordering"

val examples = arrayOf(CALCULATOR, EDIT_FORM, NEWS_FEED, FONT_LIST, SPINNER_FORM, REORDERING)

class ExampleActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra(EXAMPLE_NAME)
        when (name) {
            CALCULATOR -> content { <Calculator /> }
            EDIT_FORM -> content { <EditForm /> }
            NEWS_FEED -> content { <NewsFeed /> }
            FONT_LIST -> content { <FontList /> }
            SPINNER_FORM -> content { <SpinnerForm /> }
            REORDERING -> content { <Reordering /> }
            else -> content {
                <TextView text="ERROR: Unknown example '$name'" />
            }
        }
    }
}
