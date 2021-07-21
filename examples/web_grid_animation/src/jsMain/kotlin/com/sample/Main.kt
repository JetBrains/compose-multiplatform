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


        "li:nth-child(2n-1)" style {
            property("grid-column-start", 2)
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

        "li > img" {
            position(Position.Absolute)
            left(50.percent)
            property("transform", "translateX(-50%)")
            width(62.percent)
            property("bottom", "-10%")
            property("filter", "drop-shadow(0 50px 20px rgba(0, 0, 0, 0.20))")
            property("transition-property", "bottom, filter")
            property("transition-duration", ".3s")
        }

        "li > img:hover" style  {
            bottom(0.px)
            property("filter", "drop-shadow(0 80px 30px rgba(0, 0, 0, 0.20))")
        }

        media(mediaMinWidth(600.px)) {
            "li:nth-child(2n-1)" style {
                property("grid-column-start", "auto")
            }

            "li:nth-child(4n-3)" style {
                property("grid-column-start", "2")

            }
        }

        media(mediaMinWidth(900.px)) {
            "li:nth-child(4n-3)" style {
                property("grid-column-start", "auto")
            }

            "li:nth-child(6n-5)" style {
                property("grid-column-start", "2")

            }
        }

        media(mediaMinWidth(1200.px)) {
            "li:nth-child(6n-5)" style {
                property("grid-column-start", "auto")
            }

            "li:nth-child(8n-7)" style {
                property("grid-column-start", "2")
            }
        }

        media(mediaMinWidth(1500.px)) {
            "li:nth-child(8n-7)" style {
                property("grid-column-start", "auto")
            }

            "li:nth-child(10n-9)" style {
                property("grid-column-start", "2")

            }
        }

        media(mediaMinWidth(1800.px)) {
            "li:nth-child(10n-9)" style {
                property("grid-column-start", "auto")
            }

            "li:nth-child(12n-11)" style {
                property("grid-column-start", "2")

            }
        }

        media(mediaMinWidth(2100.px)) {
            "li:nth-child(12n-11)" style {
                property("grid-column-start", "auto")
            }

            "li:nth-child(14n-12)" style {
                property("grid-column-start", "2")

            }
        }
    }
}


fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStyleSheet)
        App()
    }
}
