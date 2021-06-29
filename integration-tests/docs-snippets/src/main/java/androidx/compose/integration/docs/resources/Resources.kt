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
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER",
    "RedundantSuspendModifier", "CascadeIf", "ClassName", "RemoveExplicitTypeArguments",
    "ControlFlowWithEmptyBody", "PropertyName"
)

package androidx.compose.integration.docs.resources

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/resources
 *
 * No action required if it's modified.
 */

@Composable
private fun ResourcesSnippet1() {
    // In the res/values/strings.xml file
    // <string name="compose">Jetpack Compose</string>

    // In your Compose code
    Text(
        text = stringResource(R.string.compose)
    )
}

@Composable
private fun ResourcesSnippet2() {
    // In the res/values/strings.xml file
    // <string name="congratulate">Happy %1$s %2$d</string>

    // In your Compose code
    Text(
        text = stringResource(R.string.congratulate, "New Year", 2021)
    )
}

@Composable
private fun ResourcesSnippet3(quantity: Int) {
    // In the res/strings.xml file
    // <plurals name="runtime_format">
    //    <item quantity="one">%1$d minute</item>
    //    <item quantity="other">%1$d minutes</item>
    // </plurals>

    // In your Compose code
    val resources = LocalContext.current.resources

    Text(
        text = resources.getQuantityString(
            R.plurals.runtime_format, quantity, quantity
        )
    )
}

@Composable
private fun ResourcesSnippet4() {
    // In the res/values/dimens.xml file
    // <dimen name="padding_small">8dp</dimen>

    // In your Compose code
    val smallPadding = dimensionResource(R.dimen.padding_small)
    Text(
        text = "...",
        modifier = Modifier.padding(smallPadding)
    )
}

@Composable
private fun ResourcesSnippet5() {
    // In the res/colors.xml file
    // <color name="colorGrey">#757575</color>

    // In your Compose code
    Divider(color = colorResource(R.color.colorGrey))
}

@Composable
private fun ResourcesSnippet6() {
    // Files in res/drawable folders. For example:
    // - res/drawable-nodpi/ic_logo.xml
    // - res/drawable-xxhdpi/ic_logo.png

    // In your Compose code
    Icon(
        painter = painterResource(id = R.drawable.ic_logo),
        contentDescription = null // decorative element
    )
}

@Composable
private fun ResourcesSnippet8() {
    Icon(Icons.Rounded.Menu, contentDescription = "Localized description")
}

@Suppress("UnnecessaryLambdaCreation")
private object ResourcesSnippet9 {
    // Define and load the fonts of the app
    private val light = Font(R.font.raleway_light, FontWeight.W300)
    private val regular = Font(R.font.raleway_regular, FontWeight.W400)
    private val medium = Font(R.font.raleway_medium, FontWeight.W500)
    private val semibold = Font(R.font.raleway_semibold, FontWeight.W600)

    // Create a font family to use in TextStyles
    private val craneFontFamily = FontFamily(light, regular, medium, semibold)

    // Use the font family to define a custom typography
    val craneTypography = Typography(
        defaultFontFamily = craneFontFamily,
        /* ... */
    )

    // Pass the typography to a MaterialTheme that will create a theme using
    // that typography in the part of the UI hierarchy where this theme is used
    @Composable
    fun CraneTheme(content: @Composable () -> Unit) {
        MaterialTheme(typography = craneTypography) {
            content()
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object color {
        const val colorGrey = 1
    }

    object dimen {
        const val padding_small = 1
    }

    object drawable {
        const val ic_logo = 1
        const val animated_vector = 1
    }

    object font {
        const val raleway_light = 1
        const val raleway_regular = 2
        const val raleway_medium = 3
        const val raleway_semibold = 4
    }

    object plurals {
        const val runtime_format = 1
    }

    object string {
        const val compose = 1
        const val congratulate = 2
    }
}
