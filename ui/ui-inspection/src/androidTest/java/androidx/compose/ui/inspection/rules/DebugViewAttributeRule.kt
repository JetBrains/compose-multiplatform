/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.inspection.rules

import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Assume.assumeTrue
import org.junit.rules.ExternalResource

private const val TEST_PACKAGE = "androidx.compose.ui.inspection.test"
private const val SETTINGS_TIMEOUT = 5000 // 5 seconds
private const val DEBUG_VIEW_ATTRIBUTES = "debug_view_attributes_application_package"

class DebugViewAttributeRule : ExternalResource() {

    override fun before() {
        getInstrumentation().uiAutomation.executeShellCommand(
            "settings put global $DEBUG_VIEW_ATTRIBUTES $TEST_PACKAGE"
        )
        assumeDebugViewAttributes(TEST_PACKAGE)
    }

    override fun after() {
        getInstrumentation().uiAutomation.executeShellCommand(
            "settings delete global $DEBUG_VIEW_ATTRIBUTES"
        )
        assumeDebugViewAttributes(null)
    }

    private fun assumeDebugViewAttributes(expected: String?) {
        val timeout = SETTINGS_TIMEOUT / 1000F
        val contentResolver = getInstrumentation().targetContext.contentResolver
        assumeTrue(
            "Assumed $DEBUG_VIEW_ATTRIBUTES would be $expected within $timeout seconds",
            busyWait {
                Settings.Global.getString(contentResolver, DEBUG_VIEW_ATTRIBUTES) == expected
            }
        )
    }

    private fun busyWait(predicate: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + SETTINGS_TIMEOUT

        do {
            if (predicate()) {
                return true
            }
            Thread.sleep(50)
        } while (System.currentTimeMillis() < deadline)

        return false
    }
}
