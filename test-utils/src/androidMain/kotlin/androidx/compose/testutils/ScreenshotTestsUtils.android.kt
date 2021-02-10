/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.testutils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.screenshot.ScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.MSSIMMatcher

/**
 * Asserts this bitmap against the golden identified by the given name.
 *
 * Note: The golden identifier should be unique per your test module (unless you want multiple tests
 * to match the same golden). The name must not contain extension. You should also avoid adding
 * strings like "golden", "image" and instead describe what is the golder referring to.
 *
 * @param rule The screenshot test rule that provides the comparison and reporting.
 * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
 * @param matcher The algorithm to be used to perform the matching. By default [MSSIMMatcher]
 * is used.
 */
fun ImageBitmap.assertAgainstGolden(
    rule: ScreenshotTestRule,
    goldenIdentifier: String,
    matcher: BitmapMatcher = MSSIMMatcher()
) = asAndroidBitmap().assertAgainstGolden(rule, goldenIdentifier, matcher)