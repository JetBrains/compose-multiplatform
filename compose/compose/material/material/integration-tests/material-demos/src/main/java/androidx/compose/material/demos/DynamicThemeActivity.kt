/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.demos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Colors
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

/**
 * Demo activity that animates the primary, secondary, and background colours in the [MaterialTheme]
 * as the user scrolls. This has the effect of going from a 'light' theme to a 'dark' theme.
 */
class DynamicThemeActivity : ComponentActivity() {
    private val scrollFraction = mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val palette = interpolateTheme(scrollFraction.value)
            val darkenedPrimary = palette.darkenedPrimary
            window.statusBarColor = darkenedPrimary
            window.navigationBarColor = darkenedPrimary

            DynamicThemeApp(scrollFraction, palette)
        }
    }

    private val Colors.darkenedPrimary: Int
        get() {
            return with(primary) {
                copy(
                    red = red * 0.75f,
                    green = green * 0.75f,
                    blue = blue * 0.75f
                )
            }.toArgb()
        }
}

private typealias ScrollFraction = MutableState<Float>

private val LazyListState.scrollOffset: Float get() {
    val total = layoutInfo.totalItemsCount
    if (total == 0 || layoutInfo.visibleItemsInfo.isEmpty()) {
        return 0f
    } else {
        val itemSize = layoutInfo.visibleItemsInfo.first().size
        val currentOffset = firstVisibleItemIndex * itemSize +
            firstVisibleItemScrollOffset
        return (currentOffset.toFloat() / (total * itemSize)).coerceIn(0f, 1f)
    }
}

@Composable
private fun DynamicThemeApp(scrollFraction: ScrollFraction, palette: Colors) {
    MaterialTheme(palette) {
        val state = rememberLazyListState()
        LaunchedEffect(state) {
            snapshotFlow { state.scrollOffset }.collect {
                scrollFraction.value = it
            }
        }
        Scaffold(
            topBar = { TopAppBar({ Text("Scroll down!") }) },
            bottomBar = { BottomAppBar(cutoutShape = CircleShape) {} },
            floatingActionButton = { Fab(scrollFraction) },
            floatingActionButtonPosition = FabPosition.Center,
            isFloatingActionButtonDocked = true,
            content = { innerPadding ->
                LazyColumn(state = state, contentPadding = innerPadding) {
                    items(20) {
                        Card(it)
                    }
                }
            }
        )
    }
}

@Composable
private fun Fab(scrollFraction: ScrollFraction) {
    val fabText = emojiForScrollFraction(scrollFraction.value)
    ExtendedFloatingActionButton(
        text = { Text(fabText, style = MaterialTheme.typography.h5) },
        onClick = {}
    )
}

@Composable
private fun Card(index: Int) {
    val shapeColor = lerp(Color(0xFF303030), Color.White, index / 19f)
    val textColor = lerp(Color.White, Color(0xFF303030), index / 19f)
    // TODO: ideally this would be a Card but currently Surface consumes every
    // colour from the Material theme to work out text colour, so we end up doing a
    // large amount of work here when the top level theme changes
    Box(
        Modifier.padding(25.dp).fillMaxWidth().height(150.dp)
            .background(shapeColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Card ${index + 1}", color = textColor)
    }
}

private fun interpolateTheme(fraction: Float): Colors {
    val interpolatedFraction = FastOutSlowInEasing.transform(fraction)

    val primary = lerp(Color(0xFF6200EE), Color(0xFF303030), interpolatedFraction)
    val secondary = lerp(Color(0xFF03DAC6), Color(0xFFBB86FC), interpolatedFraction)
    val background = lerp(Color.White, Color(0xFF121212), interpolatedFraction)

    return lightColors(
        primary = primary,
        secondary = secondary,
        background = background
    )
}

/**
 * 'Animate' the emoji in the FAB from 'sun' to 'moon' as we darken the theme
 */
private fun emojiForScrollFraction(fraction: Float): String {
    return when {
        // Sun
        fraction < 1 / 7f -> "\u2600"
        // Sun behind small cloud
        fraction < 2 / 7f -> "\uD83C\uDF24"
        // Sun behind cloud
        fraction < 3 / 7f -> "\uD83C\uDF25"
        // Cloud
        fraction < 4 / 7f -> "\u2601"
        // Cloud with rain
        fraction < 5 / 7f -> "\uD83C\uDF27"
        // Cloud with lightning
        fraction < 6 / 7f -> "\uD83C\uDF29"
        // Moon
        else -> "\uD83C\uDF15"
    }
}
