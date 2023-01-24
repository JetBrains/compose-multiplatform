/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

// TODO: External preview image.
// TODO: Introduce a rememberDateInputState once we allow switching between modes.
/**
 * <a href="https://m3.material.io/components/date-pickers/overview" class="external" target="_blank">Material Design date input</a>.
 *
 * Date pickers let people input a date, and preferably should be embedded into Dialogs.
 * See [DatePickerDialog].
 *
 * A simple DateInput looks like:
 * @sample androidx.compose.material3.samples.DateInputSample
 *
 * @param dateInputState state of the date input. See [rememberDatePickerState].
 * @param modifier the [Modifier] to be applied to this date input
 * @param dateFormatter a [DatePickerFormatter] that provides formatting skeletons for dates display
 * @param dateValidator a lambda that takes a date timestamp and return true if the date is a valid
 * one for input. Invalid dates will be indicate with an error at the UI.
 * @param title the title to be displayed in the date input
 * @param headline the headline to be displayed in the date input
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date input
 * in different states. See [DatePickerDefaults.colors].
 */
@ExperimentalMaterial3Api
@Composable
fun DateInput(
    dateInputState: DatePickerState,
    modifier: Modifier = Modifier,
    dateFormatter: DatePickerFormatter = remember { DatePickerFormatter() },
    dateValidator: (Long) -> Boolean = { true },
    title: (@Composable () -> Unit)? = { DateInputDefaults.DateInputTitle() },
    headline: @Composable () -> Unit = {
        DateInputDefaults.DateInputHeadline(
            dateInputState,
            dateFormatter
        )
    },
    colors: DatePickerColors = DatePickerDefaults.colors()
) {
    Column(modifier = modifier.padding(DatePickerHorizontalPadding)) {
        // Reusing the same header that is used by the DatePicker.
        DatePickerHeader(
            modifier = Modifier,
            title = title,
            titleContentColor = colors.titleContentColor,
            headlineContentColor = colors.headlineContentColor
        ) {
            headline()
        }
        Divider()
        DateInputTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InputTextFieldPadding),
            dateInputState = dateInputState,
            dateFormatter = dateFormatter,
            dateValidator = dateValidator
        )
    }
}

/**
 * Contains default values used by the date input.
 */
@ExperimentalMaterial3Api
@Stable
object DateInputDefaults {

    /** A default date input title composable. */
    @Composable
    fun DateInputTitle() = Text(getString(string = Strings.DateInputTitle))

    /**
     * A default date input headline composable lambda that displays a default headline text when
     * there is no date selection, and an actual date string when there is.
     *
     * @param state a [DatePickerState] that will help determine the title's headline
     * @param dateFormatter a [DatePickerFormatter]
     */
    @Composable
    fun DateInputHeadline(state: DatePickerState, dateFormatter: DatePickerFormatter) {
        val defaultLocale = defaultLocale()
        val formattedDate = dateFormatter.formatDate(
            date = state.selectedDate,
            calendarModel = state.calendarModel,
            locale = defaultLocale
        )
        val verboseDateDescription = dateFormatter.formatDate(
            date = state.selectedDate,
            calendarModel = state.calendarModel,
            locale = defaultLocale,
            forContentDescription = true
        ) ?: getString(Strings.DateInputNoInputHeadlineDescription)

        val headlineText = formattedDate ?: getString(string = Strings.DateInputHeadline)
        val headlineDescription =
            getString(Strings.DateInputHeadlineDescription).format(verboseDateDescription)

        Text(
            text = headlineText,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = headlineDescription
            },
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInputTextField(
    modifier: Modifier,
    dateInputState: DatePickerState,
    dateFormatter: DatePickerFormatter,
    dateValidator: (Long) -> Boolean
) {
    // Obtain the DateInputFormat for the default Locale.
    val defaultLocale = defaultLocale()
    val dateInputFormat = remember(defaultLocale) {
        dateInputState.calendarModel.getDateInputFormat(defaultLocale)
    }
    var errorText by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = with(dateInputState) {
                    selectedDate?.let {
                        calendarModel.formatWithPattern(
                            it.utcTimeMillis,
                            dateInputFormat.patternWithoutDelimiters,
                            defaultLocale
                        )
                    } ?: ""
                },
                TextRange(0, 0)
            )
        )
    }

    // Holds a string for displaying an error message when an input does not match the expected date
    // pattern. The string expects a date pattern string as an argument to be formatted into it.
    val errorDatePattern = getString(Strings.DateInputInvalidForPattern)
    // Holds a string for displaying an error message when an input date exceeds the year-range
    // defined at the DateInput's state. The string expects a start and end year as arguments to
    // be formatted into it.
    val errorDateOutOfYearRange = getString(Strings.DateInputInvalidYearRange)
    // Holds a string for displaying an error message when an input date does not pass the
    // DateInput's validator check. The string expects a date argument to be formatted into it.
    val errorInvalidNotAllowed = getString(Strings.DateInputInvalidNotAllowed)

    // Validates the input. Sets an error message at the errorText, or return a non-null
    // CalendarDate that represents a validated date.
    fun validate(input: TextFieldValue): CalendarDate? {
        val dateInputText = input.text.trim()
        if (dateInputText.isEmpty() ||
            dateInputText.length < dateInputFormat.patternWithoutDelimiters.length
        ) {
            errorText = ""
            return null
        }
        val parsedDate = dateInputState.calendarModel.parse(
            dateInputText,
            dateInputFormat.patternWithoutDelimiters
        )
        if (parsedDate == null) {
            errorText = errorDatePattern.format(dateInputFormat.patternWithDelimiters.uppercase())
            return null
        }
        // Check that the date is within the valid range of years.
        if (!dateInputState.yearRange.contains(parsedDate.year)) {
            errorText = errorDateOutOfYearRange.format(
                dateInputState.yearRange.first,
                dateInputState.yearRange.last
            )
            return null
        }
        // Check that the provided date validator allows this date to be selected.
        if (!dateValidator.invoke(parsedDate.utcTimeMillis)) {
            errorText = errorInvalidNotAllowed.format(
                dateFormatter.formatDate(
                    date = parsedDate,
                    calendarModel = dateInputState.calendarModel,
                    locale = defaultLocale
                )
            )
            return null
        }
        return parsedDate
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            if (input.text.length <= dateInputFormat.patternWithoutDelimiters.length &&
                input.text.all { it.isDigit() }
            ) {
                text = input
                dateInputState.selectedDate = validate(input)
            }
        },
        modifier = modifier
            // Add bottom padding when there is no error. Otherwise, remove it as the error text
            // will take additional height.
            .padding(
                bottom = if (errorText.isNotBlank()) {
                    0.dp
                } else {
                    InputTextNonErroneousBottomPadding
                }
            )
            .semantics {
                if (errorText.isNotBlank()) error(errorText)
            },
        label = { Text(getString(string = Strings.DateInputLabel)) },
        placeholder = { Text(dateInputFormat.patternWithDelimiters.uppercase()) },
        supportingText = { if (errorText.isNotBlank()) Text(errorText) },
        isError = errorText.isNotBlank(),
        visualTransformation = DateVisualTransformation(dateInputFormat),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        singleLine = true
    )
}

/**
 * A [VisualTransformation] for date input. The transformation will automatically display the date
 * delimiters provided by the [DateInputFormat] as the date is being entered into the text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class DateVisualTransformation(private val dateInputFormat: DateInputFormat) :
    VisualTransformation {

    private val firstDelimiterOffset: Int =
        dateInputFormat.patternWithDelimiters.indexOf(dateInputFormat.delimiter)
    private val secondDelimiterOffset: Int =
        dateInputFormat.patternWithDelimiters.lastIndexOf(dateInputFormat.delimiter)
    private val dateFormatLength: Int = dateInputFormat.patternWithoutDelimiters.length

    private val dateOffsetTranslator = object : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int {
            return when {
                offset < firstDelimiterOffset -> offset
                offset < secondDelimiterOffset -> offset + 1
                offset <= dateFormatLength -> offset + 2
                else -> dateFormatLength + 2 // 10
            }
        }

        override fun transformedToOriginal(offset: Int): Int {
            return when {
                offset <= firstDelimiterOffset - 1 -> offset
                offset <= secondDelimiterOffset - 1 -> offset - 1
                offset <= dateFormatLength + 1 -> offset - 2
                else -> dateFormatLength // 8
            }
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val trimmedText =
            if (text.text.length > dateFormatLength) {
                text.text.substring(0 until dateFormatLength)
            } else {
                text.text
            }
        var transformedText = ""
        trimmedText.forEachIndexed { index, char ->
            transformedText += char
            if (index + 1 == firstDelimiterOffset || index + 2 == secondDelimiterOffset) {
                transformedText += dateInputFormat.delimiter
            }
        }
        return TransformedText(AnnotatedString(transformedText), dateOffsetTranslator)
    }
}

private val InputTextFieldPadding = PaddingValues(
    start = 12.dp,
    end = 12.dp,
    top = 10.dp
)

// An optional padding that will only be added to the bottom of the date input text field when it's
// not showing an error message.
private val InputTextNonErroneousBottomPadding = 16.dp
