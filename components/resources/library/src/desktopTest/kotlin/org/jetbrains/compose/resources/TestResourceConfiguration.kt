/*
 * Copyright 2020-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import java.io.File
import java.net.URL
import java.net.URLClassLoader

class TestResourceConfiguration {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun test() = runComposeUiTest {
        val jar = this.javaClass.classLoader.getResource("resource-sample-shared-desktop.jar")
        val jarClassLoader = URLClassLoader(
            arrayOf(jar),
            null,
        )
        setContent {
            ProvideResourceConfiguration(ResourceConfiguration(jarClassLoader)) {
                val appNameStringRes = StringResource("string:app_name", "app_name", setOf(
                    ResourceItem(setOf(), "composeResources/components.resources.demo.shared.generated.resources/values/strings.commonMain.cvr", 150, 44),
                ))
                Text(
                    text = stringResource(
                    // Res.string.app_name:
                        appNameStringRes
                    ),
                    modifier = Modifier.testTag("appNameText")
                )
            }
        }

        onNodeWithTag("appNameText")
            .assertTextEquals("Compose Resources App")
            .assertExists()
    }
}
