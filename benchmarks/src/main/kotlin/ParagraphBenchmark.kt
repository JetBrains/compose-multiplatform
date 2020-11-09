package test

import androidx.compose.desktop.initCompose
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.fontFamily
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.text.platform.font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
class ParagraphBenchmark {
    private lateinit var fontLoader: Font.ResourceLoader
    val italicFont = fontFamily(font("Noto Italic", "NotoSans-Italic.ttf"))
    val text = "The quick brown fox \uD83E\uDD8A ate a zesty hamburgerfons \uD83C\uDF54.\n" +
            "The \uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67 laughed.\n"

    @Setup
    fun setUp() {
        initCompose()
        fontLoader = FontLoader()
    }

    @Benchmark
    fun builderBenchmark(): Paragraph {
        return Paragraph(
                text = text,
                style = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontFamily = italicFont
                ),
                spanStyles = listOf(
                        AnnotatedString.Range(
                                SpanStyle(
                                        color = Color(0xff964B00),
                                        shadow = Shadow(Color.Green, offset = Offset(1f, 1f))
                                ),
                                10, 19
                        ),
                        AnnotatedString.Range(
                                SpanStyle(
                                        background = Color.Yellow
                                ),
                                19, 29
                        ),
                        AnnotatedString.Range(
                                SpanStyle(
                                        fontSize = 2.em
                                ),
                                29, 48
                        ),
                        AnnotatedString.Range(
                                SpanStyle(
                                        color = Color.Green
                                ),
                                25, 35
                        ),
                ),
                width = 200f,
                density = Density(1f),
                resourceLoader = fontLoader
        )
    }

}