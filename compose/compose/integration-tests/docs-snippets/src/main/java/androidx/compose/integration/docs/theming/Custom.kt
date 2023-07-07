// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "RemoveEmptyParenthesesFromLambdaCall"
)

package androidx.compose.integration.docs.theming

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/themes/custom
 *
 * No action required if it's modified.
 */

private object CustomSnippet1 {
    // Use with MaterialTheme.colors.snackbarAction
    val Colors.snackbarAction: Color
        get() = if (isLight) Red300 else Red700

    // Use with MaterialTheme.typography.textFieldInput
    val Typography.textFieldInput: TextStyle
        get() = TextStyle(/* ... */)

    // Use with MaterialTheme.shapes.card
    val Shapes.card: Shape
        get() = RoundedCornerShape(size = 20.dp)
}

private object CustomSnippet234 {
    // Start snippet 2
    @Immutable
    data class ExtendedColors(
        val tertiary: Color,
        val onTertiary: Color
    )

    val LocalExtendedColors = staticCompositionLocalOf {
        ExtendedColors(
            tertiary = Color.Unspecified,
            onTertiary = Color.Unspecified
        )
    }

    @Composable
    fun ExtendedTheme(
        /* ... */
        content: @Composable () -> Unit
    ) {
        val extendedColors = ExtendedColors(
            tertiary = Color(0xFFA8EFF0),
            onTertiary = Color(0xFF002021)
        )
        CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
            MaterialTheme(
                /* colors = ..., typography = ..., shapes = ... */
                content = content
            )
        }
    }

    // Use with eg. ExtendedTheme.colors.tertiary
    object ExtendedTheme {
        val colors: ExtendedColors
            @Composable
            get() = LocalExtendedColors.current
    }
    // End snippet 2

    // Start snippet 3
    @Composable
    fun ExtendedButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ExtendedTheme.colors.tertiary,
                contentColor = ExtendedTheme.colors.onTertiary
                /* Other colors use values from MaterialTheme */
            ),
            onClick = onClick,
            modifier = modifier,
            content = content
        )
    }
    // End snippet 3

    // Start snippet 4
    @Composable
    fun ExtendedApp() {
        ExtendedTheme {
            /*...*/
            ExtendedButton(onClick = { /* ... */ }) {
                /* ... */
            }
        }
    }
    // End snippet 4
}

private object CustomSnippet567 {
    // Start snippet 5
    @Immutable
    data class ReplacementTypography(
        val body: TextStyle,
        val title: TextStyle
    )

    @Immutable
    data class ReplacementShapes(
        val component: Shape,
        val surface: Shape
    )

    val LocalReplacementTypography = staticCompositionLocalOf {
        ReplacementTypography(
            body = TextStyle.Default,
            title = TextStyle.Default
        )
    }
    val LocalReplacementShapes = staticCompositionLocalOf {
        ReplacementShapes(
            component = RoundedCornerShape(ZeroCornerSize),
            surface = RoundedCornerShape(ZeroCornerSize)
        )
    }

    @Composable
    fun ReplacementTheme(
        /* ... */
        content: @Composable () -> Unit
    ) {
        val replacementTypography = ReplacementTypography(
            body = TextStyle(fontSize = 16.sp),
            title = TextStyle(fontSize = 32.sp)
        )
        val replacementShapes = ReplacementShapes(
            component = RoundedCornerShape(percent = 50),
            surface = RoundedCornerShape(size = 40.dp)
        )
        CompositionLocalProvider(
            LocalReplacementTypography provides replacementTypography,
            LocalReplacementShapes provides replacementShapes
        ) {
            MaterialTheme(
                /* colors = ... */
                content = content
            )
        }
    }

    // Use with eg. ReplacementTheme.typography.body
    object ReplacementTheme {
        val typography: ReplacementTypography
            @Composable
            get() = LocalReplacementTypography.current
        val shapes: ReplacementShapes
            @Composable
            get() = LocalReplacementShapes.current
    }
    // End snippet 5

    // Start snippet 6
    @Composable
    fun ReplacementButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            shape = ReplacementTheme.shapes.component,
            onClick = onClick,
            modifier = modifier,
            content = {
                ProvideTextStyle(
                    value = ReplacementTheme.typography.body
                ) {
                    content()
                }
            }
        )
    }
    // End snippet 6

    // Start snippet 7
    @Composable
    fun ReplacementApp() {
        ReplacementTheme {
            /*...*/
            ReplacementButton(onClick = { /* ... */ }) {
                /* ... */
            }
        }
    }
    // End snippet 7
}

private object CustomSnippet89 {
    // Start snippet 8
    @Immutable
    data class CustomColors(
        val content: Color,
        val component: Color,
        val background: List<Color>
    )

    @Immutable
    data class CustomTypography(
        val body: TextStyle,
        val title: TextStyle
    )

    @Immutable
    data class CustomElevation(
        val default: Dp,
        val pressed: Dp
    )

    val LocalCustomColors = staticCompositionLocalOf {
        CustomColors(
            content = Color.Unspecified,
            component = Color.Unspecified,
            background = emptyList()
        )
    }
    val LocalCustomTypography = staticCompositionLocalOf {
        CustomTypography(
            body = TextStyle.Default,
            title = TextStyle.Default
        )
    }
    val LocalCustomElevation = staticCompositionLocalOf {
        CustomElevation(
            default = Dp.Unspecified,
            pressed = Dp.Unspecified
        )
    }

    @Composable
    fun CustomTheme(
        /* ... */
        content: @Composable () -> Unit
    ) {
        val customColors = CustomColors(
            content = Color(0xFFDD0D3C),
            component = Color(0xFFC20029),
            background = listOf(Color.White, Color(0xFFF8BBD0))
        )
        val customTypography = CustomTypography(
            body = TextStyle(fontSize = 16.sp),
            title = TextStyle(fontSize = 32.sp)
        )
        val customElevation = CustomElevation(
            default = 4.dp,
            pressed = 8.dp
        )
        CompositionLocalProvider(
            LocalCustomColors provides customColors,
            LocalCustomTypography provides customTypography,
            LocalCustomElevation provides customElevation,
            content = content
        )
    }

    // Use with eg. CustomTheme.elevation.small
    object CustomTheme {
        val colors: CustomColors
            @Composable
            get() = LocalCustomColors.current
        val typography: CustomTypography
            @Composable
            get() = LocalCustomTypography.current
        val elevation: CustomElevation
            @Composable
            get() = LocalCustomElevation.current
    }
    // End snippet 8

    // Start snippet 9
    @Composable
    fun CustomButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = CustomTheme.colors.component,
                contentColor = CustomTheme.colors.content,
                disabledBackgroundColor = CustomTheme.colors.content
                    .copy(alpha = 0.12f)
                    .compositeOver(CustomTheme.colors.component),
                disabledContentColor = CustomTheme.colors.content
                    .copy(alpha = ContentAlpha.disabled)
            ),
            shape = ButtonShape,
            elevation = ButtonDefaults.elevation(
                defaultElevation = CustomTheme.elevation.default,
                pressedElevation = CustomTheme.elevation.pressed
                /* disabledElevation = 0.dp */
            ),
            onClick = onClick,
            modifier = modifier,
            content = {
                ProvideTextStyle(
                    value = CustomTheme.typography.body
                ) {
                    content()
                }
            }
        )
    }

    val ButtonShape = RoundedCornerShape(percent = 50)
    // End snippet 9
}

/*
Fakes needed for snippets to build:
 */

private val Red300 = Color(0xffff0000)
private val Red700 = Color(0xffff0000)
