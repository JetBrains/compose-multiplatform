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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

@LargeTest
@OptIn(ExperimentalBaselineProfilesApi::class)
@SdkSuppress(minSdkVersion = 28)
class SmallListBaselineProfile {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateProfile() {
        baselineProfileRule.collectBaselineProfile(
            packageName = PACKAGE_NAME
        ) {
            val intent = Intent()
            intent.apply {
                setPackage(packageName)
                action =
                    "androidx.compose.integration.macrobenchmark.target.LAZY_COLUMN_ACTIVITY"
                putExtra("ITEM_COUNT", 200)
            }
            startActivityAndWait(intent)
            val lazyColumn =
                device.findObject(By.desc(CONTENT_DESCRIPTION))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            lazyColumn.setGestureMargin(device.displayWidth / 5)
            for (i in 1..10) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                lazyColumn.drag(Point(0, lazyColumn.visibleCenter.y / 3))
                device.wait(
                    Until.findObject(By.desc(COMPOSE_IDLE)),
                    3000
                )
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.compose.integration.macrobenchmark.target"
        private const val CONTENT_DESCRIPTION = "IamLazy"
        private const val COMPOSE_IDLE = "COMPOSE-IDLE"
    }
}
