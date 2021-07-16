package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import org.jetbrains.compose.web.attributes.builders.InputAttrsBuilder
import org.jetbrains.compose.web.attributes.*

private fun InputAttrsBuilder<String>.applyAttrsWithStringValue(
    value: String,
    attrs: InputAttrsBuilder<String>.() -> Unit
) {
    value(value)
    attrs()
}

@Composable
@NonRestartableComposable
fun CheckboxInput(checked: Boolean = false, attrs: InputAttrsBuilder<Boolean>.() -> Unit = {}) {
    Input(
        type = InputType.Checkbox,
        attrs = {
            if (checked) checked()
            this.attrs()
        }
    )
}

@Composable
@NonRestartableComposable
fun DateInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Date, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun DateTimeLocalInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.DateTimeLocal, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun EmailInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Email, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun FileInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.File, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun HiddenInput(attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Hidden, attrs = attrs)
}

@Composable
@NonRestartableComposable
fun MonthInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Month, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun NumberInput(
    value: Number? = null,
    min: Number? = null,
    max: Number? = null,
    attrs: InputAttrsBuilder<Number?>.() -> Unit = {}
) {
    Input(
        type = InputType.Number,
        attrs = {
            if (value != null) value(value.toString())
            if (min != null) min(min.toString())
            if (max != null) max(max.toString())
            attrs()
        }
    )
}

@Composable
@NonRestartableComposable
fun PasswordInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Password, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun RadioInput(checked: Boolean = false, attrs: InputAttrsBuilder<Boolean>.() -> Unit = {}) {
    Input(
        type = InputType.Radio,
        attrs = {
            if (checked) checked()
            attrs()
        }
    )
}

@Composable
@NonRestartableComposable
fun RangeInput(
    value: Number? = null,
    min: Number? = null,
    max: Number? = null,
    step: Number = 1,
    attrs: InputAttrsBuilder<Number?>.() -> Unit = {}
) {
    Input(
        type = InputType.Range,
        attrs = {
            if (value != null) value(value.toString())
            if (min != null) min(min.toString())
            if (max != null) max(max.toString())
            step(step)
            attrs()
        }
    )
}

@Composable
@NonRestartableComposable
fun SearchInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Search, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun SubmitInput(attrs: InputAttrsBuilder<Unit>.() -> Unit = {}) {
    Input(type = InputType.Submit, attrs = attrs)
}

@Composable
@NonRestartableComposable
fun TelInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Tel, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun TextInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Text, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun TimeInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Time, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun UrlInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Url, attrs = { applyAttrsWithStringValue(value, attrs) })
}

@Composable
@NonRestartableComposable
fun WeekInput(value: String = "", attrs: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Week, attrs = { applyAttrsWithStringValue(value, attrs) })
}
