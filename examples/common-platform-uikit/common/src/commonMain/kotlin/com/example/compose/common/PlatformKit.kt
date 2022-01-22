package com.example.compose.common

import androidx.compose.runtime.Composable

@Composable
expect fun Text(modifier: Modifier, text: String)

@Composable
expect fun Column(modifier: Modifier, builder: @Composable () -> Unit)

@Composable
expect fun Box(modifier: Modifier, content: @Composable BoxScope.() -> Unit)

@Composable
expect fun ProgressBar(modifier: Modifier)

@Composable
expect fun TextField(modifier: Modifier, label: String, value: String, onValueChange: (String) -> Unit)

@Composable
expect fun Button(modifier: Modifier, text: String, onClick: () -> Unit)

expect interface Modifier {
    companion object : Modifier
}

expect fun Modifier.padding(top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0): Modifier

expect fun Modifier.width(value: Int): Modifier

expect fun Modifier.height(value: Int): Modifier

expect fun Modifier.fillMaxSize(): Modifier

expect fun Modifier.fillMaxWidth(): Modifier

interface BoxScope {
    fun Modifier.align(alignment: Alignment): Modifier
}

enum class Alignment {
    Center
}
