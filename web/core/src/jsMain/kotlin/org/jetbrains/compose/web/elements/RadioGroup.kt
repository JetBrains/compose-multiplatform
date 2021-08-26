package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.builders.InputAttrsBuilder
import org.jetbrains.compose.web.attributes.name

/**
 * @param [value] - sets `value` attribute
 * @param [id] - sets `id` attribute
 * @param [attrs] - builder to set any eligible attribute
 */
@Composable
@NonRestartableComposable
@ExperimentalComposeWebApi
fun <T> RadioGroupScope<T>.RadioInput(
    value: T,
    id: String? = null,
    attrs: (InputAttrsBuilder<Boolean>.() -> Unit)? = null
) {
    val checkedValue = getCompositionLocalRadioGroupCheckedValue()
    val radioGroupName = getCompositionLocalRadioGroupName()

    Input(
        type = InputType.Radio,
        attrs = {
            attrs?.invoke(this)
            if (id != null) id(id)
            name(radioGroupName)

            val valueString = value.toString()
            checked(checkedValue == valueString)
            value(valueString)
        }
    )
}

/**
 * @param [checkedValue] - value of a radio input that has to be checked
 * @param [name] - radio group name. It has to be unique among all radio groups.
 * If it's null during first composition, radio group will use a generated name.
 * @param [content] - is a composable lambda that contains any number of [RadioInput]
 */
@Composable
@NonRestartableComposable
@ExperimentalComposeWebApi
fun <E, T : Enum<E>?> RadioGroup(
    checkedValue: T,
    name: String? = null,
    content: @Composable RadioGroupScope<T>.() -> Unit
) {
    val radioGroupName = remember { name ?: generateNextRadioGroupName() }

    CompositionLocalProvider(
        radioGroupCompositionLocalValue provides checkedValue.toString(),
        radioGroupCompositionLocalName provides radioGroupName,
        content = {
            // normal cast would fail here!
            // this is to specify the type of the values for radio inputs
            content(RadioGroupScopeImpl.unsafeCast<RadioGroupScope<T>>())
        }
    )
}

/**
 * @param [checkedValue] - value of a radio input that has to be checked
 * @param [name] - radio group name. It has to be unique among all radio groups.
 * If it's null during first composition, radio group will use a generated name.
 * @param [content] - is a composable lambda that contains any number of [RadioInput]
 */
@Composable
@NonRestartableComposable
@ExperimentalComposeWebApi
fun RadioGroup(
    checkedValue: String?,
    name: String? = null,
    content: @Composable RadioGroupScope<String>.() -> Unit
) {
    val radioGroupName = remember { name ?: generateNextRadioGroupName() }

    CompositionLocalProvider(
        radioGroupCompositionLocalValue provides checkedValue,
        radioGroupCompositionLocalName provides radioGroupName,
        content = {
            // normal cast would fail here!
            // this is to specify the type of the values for radio inputs
            content(RadioGroupScopeImpl.unsafeCast<RadioGroupScope<String>>())
        }
    )
}

@ExperimentalComposeWebApi
open class RadioGroupScope<T> internal constructor()

@OptIn(ExperimentalComposeWebApi::class)
private object RadioGroupScopeImpl : RadioGroupScope<Any>()

private var generatedRadioGroupNamesCounter = 0

private fun generateNextRadioGroupName(): String {
    return "\$compose\$generated\$radio\$group-${generatedRadioGroupNamesCounter++}"
}

internal val radioGroupCompositionLocalValue = compositionLocalOf<String?> {
    error("No radio group checked value provided")
}
internal val radioGroupCompositionLocalName = compositionLocalOf<String> {
    error("No radio group name provided")
}

@Composable
internal fun getCompositionLocalRadioGroupCheckedValue(): String? {
    return radioGroupCompositionLocalValue.current
}

@Composable
internal fun getCompositionLocalRadioGroupName(): String {
    return radioGroupCompositionLocalName.current
}
