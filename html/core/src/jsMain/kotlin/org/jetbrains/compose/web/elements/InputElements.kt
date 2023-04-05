package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.attributes.*

private fun InputAttrsScope<String>.applyAttrsWithStringValue(
    value: String,
    attrs: InputAttrsScope<String>.() -> Unit
) {
    value(value)
    attrs()
}

/**
 * It's a controlled Input of [InputType.Checkbox].
 * Controlled input means that its state is always equal [checked] value.
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun CheckboxInput(checked: Boolean = false, attrs: InputAttrsScope<Boolean>.() -> Unit = {}) {
    Input(
        type = InputType.Checkbox,
        attrs = {
            checked(checked)
            this.attrs()
        }
    )
}

/**
 * It's a controlled Input of [InputType.Date].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun DateInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Date, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.DateTimeLocal].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun DateTimeLocalInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.DateTimeLocal, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Email].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun EmailInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Email, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.File].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun FileInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.File, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Hidden].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun HiddenInput(attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Hidden, attrs = attrs)
}

/**
 * It's a controlled Input of [InputType.Month].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun MonthInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Month, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Number].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun NumberInput(
    value: Number? = null,
    min: Number? = null,
    max: Number? = null,
    attrs: InputAttrsScope<Number?>.() -> Unit = {}
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

/**
 * It's a controlled Input of [InputType.Password].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun PasswordInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Password, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Radio].
 * Controlled input means that its state is always equal [checked] value.
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun RadioInput(checked: Boolean = false, attrs: InputAttrsScope<Boolean>.() -> Unit = {}) {
    Input(
        type = InputType.Radio,
        attrs = {
            checked(checked)
            attrs()
        }
    )
}

/**
 * It's a controlled Input of [InputType.Range].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun RangeInput(
    value: Number? = null,
    min: Number? = null,
    max: Number? = null,
    step: Number = 1,
    attrs: InputAttrsScope<Number?>.() -> Unit = {}
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

/**
 * It's a controlled Input of [InputType.Search].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun SearchInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Search, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Submit].
 * If you need an uncontrolled behaviour, see [Input].
 */
@Composable
@NonRestartableComposable
fun SubmitInput(attrs: InputAttrsScope<Unit>.() -> Unit = {}) {
    Input(type = InputType.Submit, attrs = attrs)
}

/**
 * It's a controlled Input of [InputType.Tel].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun TelInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Tel, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Text].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun TextInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Text, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Time].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun TimeInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Time, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Url].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun UrlInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Url, attrs = { applyAttrsWithStringValue(value, attrs) })
}

/**
 * It's a controlled Input of [InputType.Week].
 * Controlled input means that its state is always equal [value].
 * If you need an uncontrolled behaviour, see [Input].
 *
 * @see [Input] for more details on controlled and uncontrolled modes.
 */
@Composable
@NonRestartableComposable
fun WeekInput(value: String = "", attrs: InputAttrsScope<String>.() -> Unit = {}) {
    Input(type = InputType.Week, attrs = { applyAttrsWithStringValue(value, attrs) })
}
