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

package androidx.compose.ui.unit

import android.app.Activity
import android.util.TypedValue
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class SpDeviceTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    private lateinit var activity: Activity

    @Before
    fun setup() {
        activity = activityTestRule.activity
    }

    @Test
    fun convertSpPx() {
        val dm = activity.resources.displayMetrics
        val sp10InPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, dm)
        with(Density(activity)) {
            assertEquals(sp10InPx, 10.sp.toPx(), 0.01f)
            assertEquals(10f, sp10InPx.toSp().value, 0.01f)
        }
    }

    @Test
    fun convertSpDp() {
        with(Density(activity)) {
            val px10InSp = 10f.toSp()
            val px10InDp = 10f.toDp()
            assertEquals(px10InDp.value, px10InSp.toDp().value, 0.01f)
            assertEquals(px10InSp.value, px10InDp.toSp().value, 0.01f)
        }
    }

    companion object {
        class TestActivity : Activity()
    }
}