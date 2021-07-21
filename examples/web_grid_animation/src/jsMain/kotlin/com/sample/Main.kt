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
        Img(src = imgUrl)
    }
}


fun main() {
    renderComposable(rootElementId = "root") {
        Card("adidas", "I-5923 RUNNER PRIDE", "£99.95", "https://upload.wikimedia.org/wikipedia/commons/9/9c/IntelliJ_IDEA_Icon.svg")
        Card("tiger", "TIGER ALLY", "£95.00", "/pycharm.png")
        Card("adidas", "NMD_R1", "£109.95", "https://upload.wikimedia.org/wikipedia/commons/c/c9/PhpStorm_Icon.svg")
        Card("tiger", "CALIFORNIA 78", "£75.00", "/webstorm.png")
        Card("adidas", "TUBULAR DUSK PRIMEKNIT", ">£109.95", "https://upload.wikimedia.org/wikipedia/commons/9/95/RubyMine_Icon.svg")
        Card("tiger", "GSM", "£80.00", "https://upload.wikimedia.org/wikipedia/commons/5/5f/AppCode_Icon.svg")
        Card("adidas", "NMD_CS2 PRIMEKNIT", "£149.95", "/goland.png")
        Card("tiger", "MEXICO 66", "£75.00", "/datagrip.png")
        Card("adidas", "STAN SMITH", "£74.95", "/datalore.png")
        Card("tiger", "ALVARADO", "£70.00", "/clion.png")
    }
}
