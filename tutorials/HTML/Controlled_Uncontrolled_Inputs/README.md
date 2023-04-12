# Controlled and Uncontrolled inputs

Input components have two modes: Controlled and Uncontrolled.

### Controlled vs Uncontrolled behaviour

Let's create two inputs using different modes and compare them:

``` kotlin

// Uncontrolled
Input(type = InputType.Text) {
    defaultValue("Initial Value") // optional
    onInput { event -> println(event.value) }
}

// Controlled
Input(type = InputType.Text) {
    value("Some value") // calling value(...) is necessary to make input "Controlled"
    onInput { event -> println(event.value) }
}
```

If you try running these snippets you'll see following behaviour:

- Uncontrolled text input will show "Initial value". Typing will make corresponding changes to the input's state.
- Controlled text input will show "Some value". But typing will not cause any changes.
- Both inputs will receive an `event` in `onInput { }` handler

In the example above, we set hardcoded `value` -  `value("Some value")`. Therefore, typing does nothing.
Under the hood, controlled input "restores" its state according to last known `value`.


### Using MutableState with Controlled Input

To make Controlled Input more useful we can use `MutableState<*>` to keep input's value:

``` kotlin 
val inputState = remember { mutableStateOf("Some Text") }

Input(type = InputType.Text) {
    value(inputState.value)
    onInput { event -> println(event.value) }
}
```

We can see that, `inputState` never mutates. If we run such an example as is, we'll see the same behaviour 
as when `value(...)` was hardcoded. But if we had some code that updates `inputState`, then Input would recompose and new value would be shown.

In most cases, `inputState` needs to be changed in `onInput` event handler:
``` kotlin
val inputState = remember { mutableStateOf("Some Text") }

Input(type = InputType.Text) {
    value(inputState.value)
    onInput { event -> inputState.value = event.value }
}
```

## Conclusion  

Uncontrolled input changes its content independently 
while Controlled input's content can be changed only by external state (such as MutableState).

#### In most cases Controlled input is the default choice.


## Convenient controlled inputs

Here is a list of Composable functions which represent controlled inputs of different types:

- CheckboxInput
- DateInput
- DateTimeLocalInput
- EmailInput
- FileInput
- MonthInput
- NumberInput
- PasswordInput
- RadioInput
- RangeInput
- SearchInput
- TelInput
- TextInput
- TimeInput
- UrlInput
- WeekInput

#### Example:

``` kotlin
val inputState = remember { mutableStateOf("Some Text") }

TextInput(value = inputState.value) {
    onInput { event -> inputState.value = event.value }
}
```
