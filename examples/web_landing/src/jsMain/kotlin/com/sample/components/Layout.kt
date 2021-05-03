package com.sample.components

import androidx.compose.runtime.Composable
import androidx.compose.web.css.*
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Main
import androidx.compose.web.elements.Section
import com.sample.style.WtContainer
import com.sample.style.WtOffsets
import com.sample.style.WtSections

@Composable
fun Layout(content: @Composable () -> Unit) {
    Div(
        style = {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            height(100.percent)
            margin(0.px)
            property("box-sizing", StylePropertyValue("border-box"))
        }
    ) {
        content()
    }
}

@Composable
fun MainContentLayout(content: @Composable () -> Unit) {
    Main(
        style = {
            property("flex", value("1 0 auto"))
            property("box-sizing", value("border-box"))
        }
    ) {
        content()
    }
}

@Composable
fun ContainerInSection(sectionThemeStyleClass: String? = null, content: @Composable () -> Unit) {
    Section(attrs = {
        if (sectionThemeStyleClass != null) {
            classes(WtSections.wtSection, sectionThemeStyleClass)
        } else {
            classes(WtSections.wtSection)
        }
    }) {
        Div(
            attrs = {
                classes(WtContainer.wtContainer, WtOffsets.wtTopOffset96)
            }
        ) {
            content()
        }
    }
}