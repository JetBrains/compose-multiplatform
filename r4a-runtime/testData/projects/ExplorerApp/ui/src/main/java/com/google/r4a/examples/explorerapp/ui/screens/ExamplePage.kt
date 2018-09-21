package com.google.r4a.examples.explorerapp.ui.screens

import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.Ambients
import com.google.r4a.examples.explorerapp.ui.calculator.Calculator
import com.google.r4a.examples.explorerapp.ui.forms.EditForm
import com.google.r4a.examples.explorerapp.ui.forms.SpinnerForm
import com.google.r4a.examples.explorerapp.ui.infinitescroll.NewsFeed

public const val EXAMPLE_NAME = "com.google.r4a.exampleName"

const val CALCULATOR = "Calculator"
const val EDIT_FORM = "Edit Form"
const val NEWS_FEED = "News Feed"
const val FONT_LIST = "Font List"
const val SPINNER_FORM = "Spinner Form"
const val REORDERING = "Reordering"

val EXAMPLES = arrayOf(CALCULATOR, EDIT_FORM, NEWS_FEED, FONT_LIST, SPINNER_FORM, REORDERING)

class ExamplePage : Component() {
    override fun compose() {
        val fragment = CompositionContext.current.getAmbient(Ambients.Fragment)
        val name = fragment.arguments?.getString(EXAMPLE_NAME)
        when (name) {
            CALCULATOR -> { <Calculator /> }
            EDIT_FORM -> { <EditForm /> }
            NEWS_FEED -> { <NewsFeed /> }
            FONT_LIST -> { <FontList /> }
            SPINNER_FORM -> { <SpinnerForm /> }
            REORDERING -> { <Reordering /> }
            else -> {
                <TextView text="ERROR: Unknown example '$name'" />
            }
        }
    }
}