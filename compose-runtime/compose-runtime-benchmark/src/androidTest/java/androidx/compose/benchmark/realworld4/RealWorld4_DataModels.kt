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

package androidx.compose.benchmark.realworld4

/**
 * RealWorld4 is a performance test that attempts to simulate a real-world application of reasonably
 * large scale (eg. gmail-sized application).
 */

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.ui.graphics.Color

class RealWorld4_DataModel_09() {
    var f0: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f1: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f2: RealWorld4_DataModel_10 by mutableStateOf(RealWorld4_DataModel_10())
    var f3: Boolean by mutableStateOf(random.nextBoolean())
    var f4: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f5: RealWorld4_DataModel_10 by mutableStateOf(RealWorld4_DataModel_10())
}

class RealWorld4_DataModel_06() {
    var f0: Boolean by mutableStateOf(random.nextBoolean())
    var f1: Int by mutableStateOf(random.nextInt())
    var f2: Boolean by mutableStateOf(random.nextBoolean())
    var f3: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f4: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f5: Int by mutableStateOf(random.nextInt())
    var f6: Int by mutableStateOf(random.nextInt())
    var f7: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f8: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f9: Int by mutableStateOf(random.nextInt())
    var f10: RealWorld4_DataModel_07 by mutableStateOf(RealWorld4_DataModel_07())
    var f11: RealWorld4_DataModel_07 by mutableStateOf(RealWorld4_DataModel_07())
}

class RealWorld4_DataModel_08() {
    var f0: RealWorld4_DataModel_09 by mutableStateOf(RealWorld4_DataModel_09())
    var f1: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f2: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f3: Int by mutableStateOf(random.nextInt())
    var f4: RealWorld4_DataModel_09 by mutableStateOf(RealWorld4_DataModel_09())
    var f5: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
}

class RealWorld4_DataModel_10() {
    var f0: Boolean by mutableStateOf(random.nextBoolean())
    var f1: Int by mutableStateOf(random.nextInt())
    var f2: Boolean by mutableStateOf(random.nextBoolean())
    var f3: Boolean by mutableStateOf(random.nextBoolean())
}

class RealWorld4_DataModel_07() {
    var f0: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f1: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f2: Int by mutableStateOf(random.nextInt())
    var f3: Int by mutableStateOf(random.nextInt())
    var f4: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f5: RealWorld4_DataModel_08 by mutableStateOf(RealWorld4_DataModel_08())
    var f6: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f7: RealWorld4_DataModel_08 by mutableStateOf(RealWorld4_DataModel_08())
}

class RealWorld4_DataModel_05() {
    var f0: RealWorld4_DataModel_06 by mutableStateOf(RealWorld4_DataModel_06())
    var f1: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f2: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f3: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f4: Boolean by mutableStateOf(random.nextBoolean())
    var f5: Boolean by mutableStateOf(random.nextBoolean())
    var f6: RealWorld4_DataModel_06 by mutableStateOf(RealWorld4_DataModel_06())
    var f7: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
}

class RealWorld4_DataModel_00() {
    var f0: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f1: Int by mutableStateOf(random.nextInt())
    var f2: RealWorld4_DataModel_01 by mutableStateOf(RealWorld4_DataModel_01())
    var f3: RealWorld4_DataModel_01 by mutableStateOf(RealWorld4_DataModel_01())
    var f4: Int by mutableStateOf(random.nextInt())
    var f5: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f6: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f7: Int by mutableStateOf(random.nextInt())
    var f8: Int by mutableStateOf(random.nextInt())
}

class RealWorld4_DataModel_02() {
    var f0: Int by mutableStateOf(random.nextInt())
    var f1: RealWorld4_DataModel_03 by mutableStateOf(RealWorld4_DataModel_03())
    var f2: Boolean by mutableStateOf(random.nextBoolean())
    var f3: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f4: Int by mutableStateOf(random.nextInt())
    var f5: Int by mutableStateOf(random.nextInt())
    var f6: RealWorld4_DataModel_03 by mutableStateOf(RealWorld4_DataModel_03())
    var f7: Int by mutableStateOf(random.nextInt())
    var f8: Int by mutableStateOf(random.nextInt())
    var f9: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
}

class RealWorld4_DataModel_04() {
    var f0: RealWorld4_DataModel_05 by mutableStateOf(RealWorld4_DataModel_05())
    var f1_modified: Boolean by mutableStateOf(random.nextBoolean())
    var f2: RealWorld4_DataModel_05 by mutableStateOf(RealWorld4_DataModel_05())
    var f3: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f4: Boolean by mutableStateOf(random.nextBoolean())
    var f5: Boolean by mutableStateOf(random.nextBoolean())
    var f6: Boolean by mutableStateOf(random.nextBoolean())
    var f7: Boolean by mutableStateOf(random.nextBoolean())
    var f8: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f9: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f10: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
}

class RealWorld4_DataModel_01() {
    var f0: RealWorld4_DataModel_02 by mutableStateOf(RealWorld4_DataModel_02())
    var f1: Int by mutableStateOf(random.nextInt())
    var f2: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f3: Boolean by mutableStateOf(random.nextBoolean())
    var f4: Boolean by mutableStateOf(random.nextBoolean())
    var f5: Int by mutableStateOf(random.nextInt())
    var f6: Boolean by mutableStateOf(random.nextBoolean())
    var f7: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f8: String by mutableStateOf(smallRange().map { createSomeText() }.joinToString("\n"))
    var f9: Boolean by mutableStateOf(random.nextBoolean())
    var f10: Int by mutableStateOf(random.nextInt())
    var f11: Int by mutableStateOf(random.nextInt())
    var f12: Boolean by mutableStateOf(random.nextBoolean())
    var f13: Boolean by mutableStateOf(random.nextBoolean())
    var f14: Int by mutableStateOf(random.nextInt())
    var f15: RealWorld4_DataModel_02 by mutableStateOf(RealWorld4_DataModel_02())
}

class RealWorld4_DataModel_03() {
    var f0: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f1: RealWorld4_DataModel_04 by mutableStateOf(RealWorld4_DataModel_04())
    var f2: Int by mutableStateOf(random.nextInt())
    var f3: Color by mutableStateOf(
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    )
    var f4: Boolean by mutableStateOf(random.nextBoolean())
    var f5: RealWorld4_DataModel_04 by mutableStateOf(RealWorld4_DataModel_04())
}
