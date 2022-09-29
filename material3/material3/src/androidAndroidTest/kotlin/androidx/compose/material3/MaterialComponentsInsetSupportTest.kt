/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.compose.material3

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialComponentsInsetSupportTest {
    @get:Rule
    val rule = createAndroidComposeRule<MaterialWindowInsetsActivity>()

    @Before
    fun setup() {
        rule.activity.createdLatch.await(1, TimeUnit.SECONDS)
    }

    @Test
    fun topAppBar_respectsInsetsDefault() {
        var contentPadding: WindowInsets? = null
        var expected: WindowInsets? = null
        rule.setContent {
            expected = WindowInsets.systemBars
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            contentPadding = TopAppBarDefaults.windowInsets
        }

        rule.runOnIdle {
            assertThat(contentPadding).isEqualTo(expected)
        }
    }

    @Test
    fun bottomAppBar_respectsInsetsDefault() {
        var contentPadding: WindowInsets? = null
        var expected: WindowInsets? = null
        rule.setContent {
            expected = WindowInsets.systemBars
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            contentPadding = BottomAppBarDefaults.windowInsets
        }

        rule.runOnIdle {
            // only checking bottom as bottom app bar has special optional padding on the sides
            assertThat(contentPadding).isEqualTo(expected)
        }
    }

    @Test
    fun drawerSheets_respectsInsetsDefault() {
        var contentPadding: WindowInsets? = null
        var expected: WindowInsets? = null
        rule.setContent {
            expected = WindowInsets.systemBars
                .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            contentPadding = DrawerDefaults.windowInsets
        }

        rule.runOnIdle {
            assertThat(contentPadding).isEqualTo(expected)
        }
    }

    @Test
    fun navigationBar_respectsInsetsDefault() {
        var contentPadding: WindowInsets? = null
        var expected: WindowInsets? = null
        rule.setContent {
            expected = WindowInsets.systemBars
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            contentPadding = NavigationBarDefaults.windowInsets
        }

        rule.runOnIdle {
            assertThat(contentPadding).isEqualTo(expected)
        }
    }

    @Test
    fun NavRail_respectsInsetsDefault() {
        var contentPadding: WindowInsets? = null
        var expected: WindowInsets? = null
        rule.setContent {
            expected = WindowInsets.systemBars
                .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            contentPadding = NavigationRailDefaults.windowInsets
        }

        rule.runOnIdle {
            assertThat(contentPadding).isEqualTo(expected)
        }
    }

    @Test
    fun scaffold_providesInsets() {
        var contentPadding: PaddingValues? = null
        var expected: PaddingValues? = null
        var layoutDirection: LayoutDirection? = null
        rule.setContent {
            layoutDirection = LocalLayoutDirection.current
            expected = WindowInsets.systemBars
                .asPaddingValues(LocalDensity.current)
            Scaffold { paddingValues ->
                contentPadding = paddingValues
            }
        }

        rule.runOnIdle {
            assertThat(contentPadding?.calculateBottomPadding())
                .isEqualTo(expected?.calculateBottomPadding())
            assertThat(contentPadding?.calculateTopPadding())
                .isEqualTo(expected?.calculateTopPadding())
            assertThat(contentPadding?.calculateLeftPadding(layoutDirection!!))
                .isEqualTo(expected?.calculateLeftPadding(layoutDirection!!))
            assertThat(contentPadding?.calculateRightPadding(layoutDirection!!))
                .isEqualTo(expected?.calculateRightPadding(layoutDirection!!))
        }
    }
}
