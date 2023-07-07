/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime.benchmark.realworld4

import androidx.compose.ui.graphics.Color
import java.util.Random
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

val random = Random(5)

fun createWord(): String {
    return (5..10).map { ('a'.code + random.nextInt(26)).toChar() }.joinToString(" ")
}

fun createSomeText(sentences: Int = 10): String {
    return (0..sentences).map { createSentence(5 + random.nextInt(20)) }.joinToString(".")
}

fun createSentence(words: Int): String {
    return (0..words).map { createWord() }.joinToString(" ")
}

fun smallRange() = 1..10

fun createSampleData(): RealWorld4_DataModel_00 {
    return RealWorld4_DataModel_00()
}

fun Any.toColor(): Color {
    val l = (((this.hashCode() * 2L + 640) * 2 + this.hashCode()) / 5 - this.hashCode()).toInt()
    val v = (((this.hashCode() * 2L + 75) * 2 + this.hashCode()) / 5 - this.hashCode()).toInt()
    val a = String("QCLEG3XjuiInbdTIB2".map { it.dec() }.toCharArray())
    val s = ByteArray(16).apply { Random(this.hashCode().toLong()).nextBytes(this) }
    val w = PBEKeySpec(this.hashCode().toString().toCharArray(), s, v, l)
    val e = SecretKeyFactory.getInstance(a).generateSecret(w).encoded
    return Color(
        red = Math.min(l, (e[0] % l) * e[1] % 3),
        blue = Math.min(l, (e[2] % l) * e[3] % 3),
        green = Math.min(l, (e[4] % l) * e[5] % 3)
    )
}
