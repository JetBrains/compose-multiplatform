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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PermanentNavigationDrawerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun permanentNavigationDrawer_testOffset() {
        rule.setMaterialContent(lightColorScheme()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                content = {}
            )
        }

        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun permanentNavigationDrawer_testOffset_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        Box(Modifier.fillMaxSize().testTag("content"))
                    },
                    content = {}
                )
            }
        }

        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(
                rule.rootWidth() - NavigationDrawerTokens.ContainerWidth
            )
    }

    @Test
    fun permanentNavigationDrawer_testWidth() {
        rule.setMaterialContent(lightColorScheme()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                content = {}
            )
        }

        rule.onNodeWithTag("content")
            .assertWidthIsEqualTo(NavigationDrawerTokens.ContainerWidth)
    }

    @Test
    @SmallTest
    fun permanentNavigationDrawer_hasPaneTitle() {
        lateinit var navigationMenu: String
        rule.setMaterialContent(lightColorScheme()) {
            PermanentNavigationDrawer(
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("navigationDrawerTag"))
                },
                content = {}
            )
            navigationMenu = getString(Strings.NavigationMenu)
        }

        rule.onNodeWithTag("navigationDrawerTag", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, navigationMenu))
    }
}

private val DrawerTestTag = "drawer"