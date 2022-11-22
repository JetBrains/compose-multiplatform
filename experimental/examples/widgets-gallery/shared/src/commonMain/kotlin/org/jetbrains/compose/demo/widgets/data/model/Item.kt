package org.jetbrains.compose.demo.widgets.data.model

data class Item(
    val id: Int,
    val title: String,
    val subtitle: String,
    val source: String = "demo source"
)