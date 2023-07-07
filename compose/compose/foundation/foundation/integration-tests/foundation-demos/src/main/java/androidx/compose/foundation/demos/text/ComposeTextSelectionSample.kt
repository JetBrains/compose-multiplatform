/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val commonStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF9e9e9e), lineHeight = 22.sp)
val header = TextStyle(fontSize = 22.sp, color = Color(0xFF707070), lineHeight = 36.sp)
val header2 = TextStyle(fontSize = 18.sp, color = Color(0xFF707070), lineHeight = 30.sp)
val link = SpanStyle(color = Color(0xFF03a9f4))
val highlight = SpanStyle(background = Color(0xFFefefef))
val rectColor = Color(0xFFffb74d)

val langContent = listOf(
    arrayOf(
        "Jetpack يؤلف أساسيات",
        "Jetpack Compose عبارة عن مجموعة أدوات حديثة لبناء واجهة مستخدم " +
            "Android الأصلية. يعمل Jetpack Compose على تبسيط وتسريع " +
            "تطوير واجهة المستخدم على نظام Android باستخدام " +
            "رموز أقل وأدوات قوية وواجهات برمجة تطبيقات Kotlin البديهية."
    ),
    arrayOf(
        "添加文本元素",
        "Jetpack Compose是用于构建本机Android UI的现代工具包。 Jetpack " +
            "Compose使用更少的代码，强大的工具和直观的Kotlin API简化并加速了Android上的UI开发。"
    ),
    arrayOf(
        "एक पाठ तत्व जोड़ें",
        "रचना योग्य कार्यों को केवल अन्य रचना कार्यों के " +
            "दायरे में से बुलाया जा सकता है। किसी फंक्शन को " +
            "कंपोजिटेबल बनाने के लिए, @ कम्\u200Dपोजिट " +
            "एनोटेशन जोड़ें।"
    ),
    arrayOf(
        "ข้อมูลพื้นฐานเกี่ยวกับการเขียน Jetpack",
        "ฟังก์ชั่น Composable สามารถเรียกใช้ได้จากภายในขอบเขตของฟังก์ชั่นอื่น ๆ เท่านั้น " +
            "ในการสร้างฟังก์ชั่นคอมโพสิตให้เพิ่มคำอธิบายประกอบ @Composable"
    )
)

@Composable
fun TextSelectionSample() {
    SelectionContainer {
        LazyColumn(Modifier.padding(12.dp)) {
            item {
                Basics()
            }
            item {
                AddTextElement()
            }
            items(langContent) {
                MultiLanguage(it[0], it[1])
            }
            item {
                Basics()
            }
            item {
                MultiParagraph()
            }
            item {
                AddTextElement()
            }
        }
    }
}

@Composable
private fun Basics() {
    Text(
        text = "Jetpack Compose Basics",
        style = commonStyle.merge(header)
    )
    Row {
        Box(Modifier.padding(8.dp).size(48.dp).background(rectColor))
        Text(
            text = "Jetpack Compose is a modern toolkit for building native Android UI." +
                " Jetpack Compose simplifies and accelerates UI development on Android " +
                "with less code, powerful tools, and intuitive Kotlin APIs.",
            modifier = Modifier.weight(1f),
            style = commonStyle
        )
    }
}

@Composable
private fun AddTextElement() {
    Text(
        text = "Add a text element",
        style = commonStyle.merge(header2)
    )
    Row {
        Column(Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append("To begin, follow the")
                    withStyle(link) {
                        append(" Jetpack Compose setup instructions ")
                    }
                    append(
                        ", and create an app using the Empty Compose Activity template. Then " +
                            "add a text element to your blank activity. You do this by " +
                            "defining a content block, and calling the Text() function."
                    )
                },
                style = commonStyle
            )
        }
        Box(Modifier.padding(8.dp).size(48.dp).background(rectColor))
    }
    Box(
        Modifier
            .padding(top = 20.dp, bottom = 20.dp)
            .size(200.dp, 60.dp)
            .background(rectColor)
    )
    Text(
        text = buildAnnotatedString {
            withStyle(commonStyle.toSpanStyle()) {
                append(
                    "The setContent block defines the activity's layout. Instead of " +
                        "defining the layout contents with an XML file, we call " +
                        "composable functions. Jetpack Compose uses a custom " +
                        "Kotlin compiler plugin to transform these composable " +
                        "functions into the app's UI elements. For example, the"
                )
                withStyle(highlight) {
                    append(" Text() ")
                }
                append(
                    " function is defined by the Compose UI library; you call that " +
                        "function to declare a text element in your app."
                )
            }
        }
    )
}

@Composable
private fun MultiParagraph() {
    Text(
        text = "Define a composable function (Multi Paragraph)",
        style = commonStyle.merge(header2)
    )
    Text(
        text = buildAnnotatedString {
            withStyle(commonStyle.toSpanStyle()) {
                withStyle(commonStyle.toParagraphStyle()) {
                    append(
                        "Composable functions can only be called from within the scope of " +
                            "other composable functions. To make a function composable, add " +
                            "the @Composable annotation. "
                    )
                }
                withStyle(commonStyle.toParagraphStyle()) {
                    append(
                        "To try this out, define a Greeting() function which is passed a " +
                            "name, and uses that name to configure the text element."
                    )
                }
                withStyle(highlight) {
                    append(" Text() ")
                }
                append(
                    " function is defined by the Compose UI library; you call that " +
                        "function to declare a text element in your app."
                )
            }
        }
    )
}

@Composable
private fun MultiLanguage(title: String, content: String) {
    Text(
        text = title,
        style = commonStyle.merge(header)
    )
    Row {
        Box(Modifier.padding(8.dp).size(48.dp).background(rectColor))
        Text(
            text = content,
            modifier = Modifier.weight(1f),
            style = commonStyle
        )
    }
}
