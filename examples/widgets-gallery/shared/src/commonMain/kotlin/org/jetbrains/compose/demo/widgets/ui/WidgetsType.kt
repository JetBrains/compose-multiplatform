package org.jetbrains.compose.demo.widgets.ui

enum class WidgetsType(private val customTitle: String? = null) {
    APP_BARS,
    BUTTONS,
    CHIPS,
    LOADERS,
    SNACK_BARS,
    TEXT_VIEWS,
    TEXT_INPUTS,
    TOGGLES,
    UI_CARDS("UI Cards");

    private val readableName: String by lazy {
        name.split("_")
            .map { it.lowercase() }
            .mapIndexed { i, it ->
                if (i == 0) it.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                } else it
            }.joinToString(" ")
    }

    val title: String
        get() = customTitle ?: readableName

    val testTag: String
        get() = name.lowercase()

    companion object {
        val sortedValues: List<WidgetsType> by lazy {
            entries.sortedBy { it.name }
        }
    }
}