package com.example.compose.common

import androidx.compose.runtime.Composable


@Composable
fun Text(text: String) {
    Text(modifier = Modifier, text = text)
}

@Composable
fun Column(builder: @Composable () -> Unit) {
    Column(modifier = Modifier, builder = builder)
}

@Composable
fun ProgressBar() {
    ProgressBar(modifier = Modifier)
}

@Composable
fun TextField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(modifier = Modifier, label = label, value = value, onValueChange = onValueChange)
}

@Composable
fun Button(text: String, onClick: () -> Unit) {
    Button(modifier = Modifier, text = text, onClick = onClick)
}
