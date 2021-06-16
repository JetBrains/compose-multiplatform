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

package androidx.compose.ui.window

import androidx.compose.desktop.ComposeWindow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import javax.swing.JSeparator

@OptIn(ExperimentalComposeUiApi::class)
class MenuBarTest {
    @Test(timeout = 20000)
    fun `show and hide menu bar`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isMenubarShowing by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    if (isMenubarShowing) {
                        MenuBar {
                            Menu("Menu0") {
                                Item("Item0", onClick = {})
                                Separator()
                            }
                            Menu("Menu1") {}
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window?.jMenuBar).isNotNull()
        window?.jMenuBar!!.apply {
            assertThat(menuCount).isEqualTo(2)
            assertThat(getMenu(0).text).isEqualTo("Menu0")
            assertThat(getMenu(1).text).isEqualTo("Menu1")

            getMenu(0).apply {
                assertThat(itemCount).isEqualTo(2)
                assertThat(getItem(0).text).isEqualTo("Item0")
                assertThat(getMenuComponent(1)).isInstanceOf(JSeparator::class.java)
            }

            getMenu(1).apply {
                assertThat(itemCount).isEqualTo(0)
            }
        }

        isMenubarShowing = false
        awaitIdle()
        assertThat(window!!.jMenuBar).isNull()

        isOpen = false
    }

    @Test(timeout = 20000)
    fun `show and hide menu`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isMenuShowing by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    MenuBar {
                        if (isMenuShowing) {
                            Menu("Menu0") {
                                Item("Item0", onClick = {})
                                Separator()
                            }
                        }
                        Menu("Menu1") {}
                    }
                }
            }
        }

        awaitIdle()

        isMenuShowing = false
        awaitIdle()
        assertThat(window!!.jMenuBar.menuCount).isEqualTo(1)
        window?.jMenuBar!!.apply {
            assertThat(menuCount).isEqualTo(1)
            assertThat(getMenu(0).text).isEqualTo("Menu1")
        }

        isMenuShowing = true
        awaitIdle()
        assertThat(window!!.jMenuBar.menuCount).isEqualTo(2)
        window?.jMenuBar!!.apply {
            assertThat(menuCount).isEqualTo(2)
            assertThat(getMenu(0).text).isEqualTo("Menu0")
            assertThat(getMenu(1).text).isEqualTo("Menu1")
        }

        isOpen = false
    }

    @Test(timeout = 20000)
    fun `show and hide submenu`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isSubmenuShowing by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    MenuBar {
                        Menu("Menu0") {
                            if (isSubmenuShowing) {
                                Menu("Submenu0") {}
                            }
                            Item("Item0", onClick = {})
                            Separator()
                        }
                        Menu("Menu1") {}
                    }
                }
            }
        }

        awaitIdle()

        isSubmenuShowing = false
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).itemCount).isEqualTo(2)
        window?.jMenuBar!!.getMenu(0).apply {
            assertThat(itemCount).isEqualTo(2)
            assertThat(getItem(0).text).isEqualTo("Item0")
            assertThat(getMenuComponent(1)).isInstanceOf(JSeparator::class.java)
        }

        isSubmenuShowing = true
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).itemCount).isEqualTo(3)
        window?.jMenuBar!!.getMenu(0).apply {
            assertThat(itemCount).isEqualTo(3)
            assertThat(getItem(0).text).isEqualTo("Submenu0")
            assertThat(getItem(1).text).isEqualTo("Item0")
            assertThat(getMenuComponent(2)).isInstanceOf(JSeparator::class.java)
        }

        isOpen = false
    }

    @Test(timeout = 20000)
    fun `change parameters`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var menuLabel by mutableStateOf("Menu")
        var menuEnabled by mutableStateOf(true)
        var submenuLabel by mutableStateOf("Submenu")
        var submenuEnabled by mutableStateOf(true)
        var itemLabel by mutableStateOf("Item")
        var itemEnabled by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    MenuBar {
                        Menu(menuLabel, enabled = menuEnabled) {
                            Menu(submenuLabel, enabled = submenuEnabled) {}
                            Item(itemLabel, enabled = itemEnabled, onClick = {})
                        }
                    }
                }
            }
        }

        awaitIdle()

        menuLabel = "Menu2"
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).text).isEqualTo("Menu2")

        menuEnabled = false
        awaitIdle()
        assertThat(!window!!.jMenuBar.getMenu(0).isEnabled).isTrue()

        submenuLabel = "Submenu2"
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).getItem(0).text).isEqualTo("Submenu2")

        submenuEnabled = false
        awaitIdle()
        assertThat(!window!!.jMenuBar.getMenu(0).getItem(0).isEnabled).isTrue()

        itemLabel = "Item2"
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).getItem(1).text).isEqualTo("Item2")

        itemEnabled = false
        awaitIdle()
        assertThat(!window!!.jMenuBar.getMenu(0).getItem(1).isEnabled)

        isOpen = false
    }
}