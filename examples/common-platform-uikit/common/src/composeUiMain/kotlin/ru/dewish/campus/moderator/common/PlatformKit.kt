package com.example.compose.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.compose.common.Alignment as CommonAlignment

@Composable
actual fun Text(modifier: Modifier, text: String) {
    androidx.compose.material.Text(
        modifier = modifier.composeModifier,
        text = text
    )
}

@Composable
actual fun Column(modifier: Modifier, builder: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.composeModifier
    ) {
        builder()
    }
}

@Composable
actual fun Box(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.composeModifier.fillMaxSize()) {
        val scope = object : BoxScope {
            override fun Modifier.align(alignment: CommonAlignment): Modifier {
                val composeAlignment = when (alignment) {
                    CommonAlignment.Center -> Alignment.Center
                }
                return compose { this.align(composeAlignment) }
            }
        }
        scope.content()
    }
}

@Composable
actual fun ProgressBar(modifier: Modifier) {
    CircularProgressIndicator(modifier = modifier.composeModifier)
}

@Composable
actual fun TextField(modifier: Modifier, label: String, value: String, onValueChange: (String) -> Unit) {
    androidx.compose.material.TextField(
        modifier = modifier.composeModifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) }
    )
}

@Composable
actual fun Button(modifier: Modifier, text: String, onClick: () -> Unit) {
    androidx.compose.material.Button(
        modifier = modifier.composeModifier,
        onClick = onClick
    ) {
        androidx.compose.material.Text(text = text)
    }
}

actual interface Modifier {
    val composeModifier: androidx.compose.ui.Modifier

    fun compose(block: androidx.compose.ui.Modifier.() -> androidx.compose.ui.Modifier): Modifier {
        val result = composeModifier.block()
        return object : Modifier {
            override val composeModifier: androidx.compose.ui.Modifier = result
        }
    }

    actual companion object : Modifier {
        override val composeModifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
    }
}

actual fun Modifier.padding(top: Int, bottom: Int, start: Int, end: Int): Modifier {
    return compose { this.padding(start = start.dp, end = end.dp, top = top.dp, bottom = bottom.dp) }
}

actual fun Modifier.width(value: Int): Modifier {
    return compose { this.width(value.dp) }
}

actual fun Modifier.height(value: Int): Modifier {
    return compose { this.height(value.dp) }
}

actual fun Modifier.fillMaxSize(): Modifier {
    return compose { this.fillMaxSize() }
}

actual fun Modifier.fillMaxWidth(): Modifier {
    return compose { this.fillMaxWidth() }
}
