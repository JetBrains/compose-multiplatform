package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.web.attributes.InputAttrsBuilder
import org.jetbrains.compose.web.attributes.*

private fun InputAttrsBuilder<String>.applyAttrsWithStringValue(
    value: String,
    attrsBuilder: InputAttrsBuilder<String>.() -> Unit
) {
    if (value.isNotEmpty()) value(value)
    attrsBuilder()
}

@Composable
@NonRestartableComposable
fun CheckboxInput(checked: Boolean = false, attrsBuilder: InputAttrsBuilder<Boolean>.() -> Unit = {}) {
    Input(
        type = InputType.Checkbox,
        attrs = {
            if (checked) checked()
            this.attrsBuilder()
        }
    )
}

@Composable
@NonRestartableComposable
fun DateInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Date, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun DateTimeLocalInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.DateTimeLocal, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun EmailInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Email, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun FileInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.File, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun HiddenInput(attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Hidden, attrs = attrsBuilder)
}

@Composable
@NonRestartableComposable
fun MonthInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Month, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun NumberInput(
    value: Number? = null,
    min: Number? = null,
    max: Number? = null,
    attrsBuilder: InputAttrsBuilder<Number?>.() -> Unit = {}
) {
    Input(
        type = InputType.Number,
        attrs = {
            if (value != null) value(value.toString())
            if (min != null) min(min.toString())
            if (max != null) max(max.toString())
            attrsBuilder()
        }
    )
}

@Composable
@NonRestartableComposable
fun PasswordInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Password, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun RadioInput(checked: Boolean = false, attrsBuilder: InputAttrsBuilder<Boolean>.() -> Unit = {}) {
    Input(
        type = InputType.Radio,
        attrs = {
            if (checked) checked()
            attrsBuilder()
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
    attrsBuilder: InputAttrsBuilder<Number?>.() -> Unit = {}
) {
    Input(
        type = InputType.Range,
        attrs = {
            if (value != null) value(value.toString())
            if (min != null) min(min.toString())
            if (max != null) max(max.toString())
            step(step)
            attrsBuilder()
        }
    )
}

@Composable
@NonRestartableComposable
fun SearchInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Search, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun SubmitInput(attrsBuilder: InputAttrsBuilder<Unit>.() -> Unit = {}) {
    Input(type = InputType.Submit, attrs = attrsBuilder)
}

@Composable
@NonRestartableComposable
fun TelInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Tel, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun TextInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Text, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun TimeInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Time, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun UrlInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Url, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
@NonRestartableComposable
fun WeekInput(value: String = "", attrsBuilder: InputAttrsBuilder<String>.() -> Unit = {}) {
    Input(type = InputType.Week, attrs = { applyAttrsWithStringValue(value, attrsBuilder) })
}

@Composable
fun <K> Input(
    type: InputType<K>,
    attrs: InputAttrsBuilder<K>.() -> Unit
) {
    TagElement(
        elementBuilder = ElementBuilder.Input,
        applyAttrs = {
            val inputAttrsBuilder = InputAttrsBuilder(type)
            inputAttrsBuilder.type(type)
            inputAttrsBuilder.attrs()
            this.copyFrom(inputAttrsBuilder)
        },
        content = null
    )
}

@Composable
fun <K> Input(type: InputType<K>) {
    TagElement(
        elementBuilder = ElementBuilder.Input,
        applyAttrs = {
            val inputAttrsBuilder = InputAttrsBuilder(type)
            inputAttrsBuilder.type(type)
            this.copyFrom(inputAttrsBuilder)
        },
        content = null
    )
}
