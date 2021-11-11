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

package androidx.compose.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.isLinux
import androidx.compose.ui.isWindows
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.unit.dp
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@OptIn(InternalTestApi::class)
class DesktopSvgResourcesTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("compose/ui/ui-desktop/res")

    @Test
    fun `load SVG with specified size`() {
        assumeTrue(isLinux || isWindows)

        val snapshot = renderComposeScene(width = 200, height = 200) {
            Image(
                painterResource("androidx/compose/ui/res/star-size-100.svg"),
                contentDescription = "Star"
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun `load SVG with unspecified size`() {
        assumeTrue(isLinux || isWindows)

        val snapshot = renderComposeScene(width = 200, height = 200) {
            Image(
                painterResource("androidx/compose/ui/res/star-size-unspecified.svg"),
                contentDescription = "Star"
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun `load SVG with unspecified viewbox`() {
        assumeTrue(isLinux || isWindows)

        val snapshot = renderComposeScene(width = 200, height = 200) {
            Image(
                painterResource("androidx/compose/ui/res/star-viewbox-unspecified.svg"),
                contentDescription = "Star"
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun `load SVG with custom size`() {
        assumeTrue(isLinux || isWindows)

        val snapshot = renderComposeScene(width = 200, height = 200) {
            Image(
                painterResource("androidx/compose/ui/res/star-size-100.svg"),
                contentDescription = "Star",
                modifier = Modifier.size(50.dp)
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun `load SVG and fill bounds`() {
        assumeTrue(isLinux || isWindows)

        val snapshot = renderComposeScene(width = 200, height = 300) {
            Image(
                painterResource("androidx/compose/ui/res/star-size-100.svg"),
                contentDescription = "Star",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        screenshotRule.write(snapshot)
    }

    @Test
    fun `load SVG with unspecified viewbox and fill bounds`() {
        assumeTrue(isLinux || isWindows)

        val snapshot = renderComposeScene(width = 200, height = 300) {
            Image(
                painterResource("androidx/compose/ui/res/star-viewbox-unspecified.svg"),
                contentDescription = "Star",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        screenshotRule.write(snapshot)
    }
}