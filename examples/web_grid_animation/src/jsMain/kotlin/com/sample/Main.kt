package com.sample

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

@Composable
fun Card(title: String, subTitle: String, imgUrl: String) {
    Li {
        Div {
            H2 { Text(title) }
            H4 { Text(subTitle) }
        }
        Img(src = imgUrl, alt = "")
    }
}


fun main() {
    renderComposable(rootElementId = "root") {
        Card(
            "IDEA",
            "3 out of every 4 Java developers choose IntelliJ IDEA",
            "/idea.svg"
        )
        Card(
            "PyCharm",
            "The Python IDE for Professional Developers",
            "/pycharm.svg"
        )
        Card(
            "PHPStorm",
            "The Lightning-Smart PHP IDE",
            "/phpstorm.svg"
        )
        Card("WebStorm", "The smartest JavaScript IDE", "/webstorm.png")
        Card(
            "RubyMine",
            "The most intelligent Ruby IDE",
            "/rubymine.svg"
        )
        Card(
            "AppCode",
            "Smart IDE for iOS/macOS development",
            "/appcode.svg"
        )
        Card("GoLand", "Built specially for Go developers", "/goland.png")
        Card("DataGrip", "Many databases, one tool", "/datagrip.png")
        Card("Datalore", "The online data science notebook", "/datalore.png")
        Card("Clion", "A cross-platform IDE for C and C++", "/clion.png")
    }
}
