package com.sample

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
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

@Composable
fun App() {
    Ul {
        Card("IDEA", "3 out of every 4 Java developers choose IntelliJ IDEA", "/idea.svg")
        Card("PyCharm", "The Python IDE for Professional Developers", "/pycharm.svg")
        Card("PHPStorm", "The Lightning-Smart PHP IDE", "/phpstorm.svg")
        Card("WebStorm", "The smartest JavaScript IDE", "/webstorm.png")
        Card("RubyMine", "The most intelligent Ruby IDE", "/rubymine.svg")
        Card("AppCode", "Smart IDE for iOS/macOS development", "/appcode.svg")
        Card("GoLand", "Built specially for Go developers", "/goland.png")
        Card("DataGrip", "Many databases, one tool", "/datagrip.png")
        Card("Datalore", "The online data science notebook", "/datalore.png")
        Card("Clion", "A cross-platform IDE for C and C++", "/clion.png")
    }
}

object AppStyleSheet: StyleSheet() {
    init {
        "ul" style {
            margin(100.px, 0.px)
            display(DisplayStyle.Grid)
            gridTemplateColumns("repeat(var(--columns),1fr")
            backgroundImage("url(\"/background.svg\")")
            backgroundSize("calc(200%/(var(--columns)))")
        }

        "li" style {
            property("grid-column-end", "span 2")
            position(Position.Relative)
            paddingBottom(86.66.percent)
        }

        "div" style {
            position(Position.Absolute)
            width(50.percent)
            property("font-size", "calc(15vw/var(--columns))")
            property("transform", "skewy(-30deg)")
            marginTop(14.percent)
            padding(3.percent)
        }

        "p" style {
            fontSize(2.em)
        }
    }
}


fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStyleSheet)
        App()
    }
}
