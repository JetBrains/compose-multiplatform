/*
* Copyright 2021 The Android Open Source Project
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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "LocalVariableName")

package androidx.compose.integration.docs.tooling

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/tooling
 *
 * No action required if it's modified.
 */

private object ToolingSnippet1 {
    @Composable
    fun SimpleComposable() {
        Text("Hello World")
    }
}

private class ToolingSnippet2 {
    @Preview
    @Composable
    fun ComposablePreview() {
        SimpleComposable()
    }
}

private class ToolingSnippet3 {
    @Preview(showBackground = true, backgroundColor = 0xFF00FF00)
    @Composable
    fun WithGreenBackground() {
        Text("Hello World")
    }
}

private class ToolingSnippet4 {
    @Preview(widthDp = 50, heightDp = 50)
    @Composable
    fun SquareComposablePreview() {
        Box(Modifier.background(Color.Yellow)) {
            Text("Hello World")
        }
    }
}

private class ToolingSnippet5 {
    @Preview(locale = "fr-rFR")
    @Composable
    fun DifferentLocaleComposablePreview() {
        Text(text = stringResource(R.string.greetings))
    }
}

private class ToolingSnippet6 {
    @Preview(showSystemUi = true)
    @Composable
    fun DecoratedComposablePreview() {
        Text("Hello World")
    }
}

private class ToolingSnippet7 {
    @Preview
    @Composable
    fun UserProfilePreview(
        @PreviewParameter(UserPreviewParameterProvider::class) user: User
    ) {
        UserProfile(user)
    }

    class UserPreviewParameterProvider : PreviewParameterProvider<User> {
        override val values = sequenceOf(
            User("Elise"),
            User("Frank"),
            User("Julia")
        )
    }
}

private class ToolingSnippet8 {
    @Preview
    @Composable
    fun UserProfilePreview(
        @PreviewParameter(UserPreviewParameterProvider::class, limit = 2) user: User
    ) {
        UserProfile(user)
    }
}

private class ToolingSnippet9 {
    @Preview
    @Composable
    fun PressedSurface() {
        val (pressed, onPress) = remember { mutableStateOf(false) }
        val transition = updateTransition(
            targetState = if (pressed) SurfaceState.Pressed else SurfaceState.Released
        )

        val width by transition.animateDp { state ->
            when (state) {
                SurfaceState.Released -> 20.dp
                SurfaceState.Pressed -> 50.dp
            }
        }
        val surfaceColor by transition.animateColor { state ->
            when (state) {
                SurfaceState.Released -> Color.Blue
                SurfaceState.Pressed -> Color.Red
            }
        }
        val selectedAlpha by transition.animateFloat { state ->
            when (state) {
                SurfaceState.Released -> 0.5f
                SurfaceState.Pressed -> 1f
            }
        }

        Surface(
            color = surfaceColor.copy(alpha = selectedAlpha),
            modifier = Modifier
                .toggleable(value = pressed, onValueChange = onPress)
                .size(height = 200.dp, width = width)
        ) {}
    }
}

private fun SimpleComposable() {}
private data class User(val name: String)

@Composable
private fun UserProfile(user: User) {
    Text(user.name)
}

private class UserPreviewParameterProvider : PreviewParameterProvider<User> {
    override val values = emptySequence<User>()
}

private enum class SurfaceState { Released, Pressed }

private object R {
    object string {
        const val greetings = 1
    }
}