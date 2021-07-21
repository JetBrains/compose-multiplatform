package com.sample

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

@Composable
fun Card(className: String, title: String, subTitle: String, imgUrl: String) {
    Li({ classes(className) }) {
        Div {
            H2 { Text(title) }
            P { Text(subTitle) }
        }
        Img(src = imgUrl, alt = "")
    }
}


fun main() {
    renderComposable(rootElementId = "root") {
        Card("adidas", "IDEA", "£99.95", "https://upload.wikimedia.org/wikipedia/commons/9/9c/IntelliJ_IDEA_Icon.svg")
        Card("tiger", "PyCharm", "£95.00", "https://upload.wikimedia.org/wikipedia/commons/1/1d/PyCharm_Icon.svg")
        Card("adidas", "PHPStorm", "£109.95", "https://upload.wikimedia.org/wikipedia/commons/c/c9/PhpStorm_Icon.svg")
        Card("tiger", "WebStorm", "£75.00", "/webstorm.png")
        Card("adidas", "RubyMine", ">£109.95", "https://upload.wikimedia.org/wikipedia/commons/9/95/RubyMine_Icon.svg")
        Card("tiger", "AppCode", "£80.00", "https://upload.wikimedia.org/wikipedia/commons/5/5f/AppCode_Icon.svg")
        Card("adidas", "GoLand", "£149.95", "/goland.png")
        Card("tiger", "DataGrip", "£75.00", "/datagrip.png")
        Card("adidas", "Datalore", "£74.95", "/datalore.png")
        Card("tiger", "Clion", "£70.00", "/clion.png")
    }
}
