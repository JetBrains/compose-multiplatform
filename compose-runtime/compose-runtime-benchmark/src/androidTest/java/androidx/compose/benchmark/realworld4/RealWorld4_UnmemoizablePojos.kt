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

import androidx.ui.graphics.Color

class RealWorld4_UnmemoizablePojo_0() {
    var f1: Int = random.nextInt()
    var f2: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: Boolean = random.nextInt(1) > 0
    var f5: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
}

class RealWorld4_UnmemoizablePojo_1() {
    var f1: Int = random.nextInt()
    var f2: Boolean = random.nextInt(1) > 0
    var f3: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f4: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f5: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
}

class RealWorld4_UnmemoizablePojo_2() {
    var f1: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f2: Boolean = random.nextInt(1) > 0
    var f3: Int = random.nextInt()
    var f4: Int = random.nextInt()
    var f5: Boolean = random.nextInt(1) > 0
}

class RealWorld4_UnmemoizablePojo_3() {
    var f1: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f2: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f5: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f6: Boolean = random.nextInt(1) > 0
    var f7: String = smallRange().map { createSomeText() }.joinToString("\n")
}

class RealWorld4_UnmemoizablePojo_4() {
    var f1: Int = random.nextInt()
    var f2: Boolean = random.nextInt(1) > 0
    var f3: Boolean = random.nextInt(1) > 0
    var f4: Int = random.nextInt()
    var f5: Int = random.nextInt()
    var f6: Boolean = random.nextInt(1) > 0
    var f7: Boolean = random.nextInt(1) > 0
    var f8: Boolean = random.nextInt(1) > 0
    var f9: Int = random.nextInt()
    var f10: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f11: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f12: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f13: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
}

class RealWorld4_UnmemoizablePojo_5() {
    var f1: Boolean = random.nextInt(1) > 0
    var f2: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f3: Boolean = random.nextInt(1) > 0
    var f4: Boolean = random.nextInt(1) > 0
    var f5: String = smallRange().map { createSomeText() }.joinToString("\n")
}

class RealWorld4_UnmemoizablePojo_6() {
    var f1: Int = random.nextInt()
    var f2: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: Boolean = random.nextInt(1) > 0
    var f5: Int = random.nextInt()
    var f6: Int = random.nextInt()
    var f7: Boolean = random.nextInt(1) > 0
}

class RealWorld4_UnmemoizablePojo_7() {
    var f1: Int = random.nextInt()
    var f2: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f3: Boolean = random.nextInt(1) > 0
    var f4: Boolean = random.nextInt(1) > 0
    var f5: Int = random.nextInt()
    var f6: Boolean = random.nextInt(1) > 0
    var f7: Boolean = random.nextInt(1) > 0
    var f8: String = smallRange().map { createSomeText() }.joinToString("\n")
}

class RealWorld4_UnmemoizablePojo_8() {
    var f1: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f2: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f3: Int = random.nextInt()
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
}

class RealWorld4_UnmemoizablePojo_9() {
    var f1: Int = random.nextInt()
    var f2: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f3: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f4: Int = random.nextInt()
    var f5: String = smallRange().map { createSomeText() }.joinToString("\n")
}

class RealWorld4_UnmemoizablePojo_10() {
    var f1: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f2: Boolean = random.nextInt(1) > 0
    var f3: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Int = random.nextInt()
    var f6: String = smallRange().map { createSomeText() }.joinToString("\n")
}

class RealWorld4_UnmemoizablePojo_11() {
    var f1: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f2: Boolean = random.nextInt(1) > 0
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: Int = random.nextInt()
    var f5: Int = random.nextInt()
    var f6: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f7: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f8: Boolean = random.nextInt(1) > 0
    var f9: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f10: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f11: Boolean = random.nextInt(1) > 0
    var f12: Int = random.nextInt()
    var f13: Boolean = random.nextInt(1) > 0
}

class RealWorld4_UnmemoizablePojo_12() {
    var f1: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f2: Int = random.nextInt()
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: Boolean = random.nextInt(1) > 0
    var f5: Boolean = random.nextInt(1) > 0
    var f6: Boolean = random.nextInt(1) > 0
}

class RealWorld4_UnmemoizablePojo_13() {
    var f1: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f2: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f3: Int = random.nextInt()
    var f4: Int = random.nextInt()
    var f5: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f6: Boolean = random.nextInt(1) > 0
    var f7: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f8: Int = random.nextInt()
    var f9: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f10: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f11: Boolean = random.nextInt(1) > 0
    var f12: Boolean = random.nextInt(1) > 0
    var f13: Boolean = random.nextInt(1) > 0
}

class RealWorld4_UnmemoizablePojo_14() {
    var f1: Int = random.nextInt()
    var f2: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
    var f3: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f4: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f5: Boolean = random.nextInt(1) > 0
    var f6: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f7: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f8: String = smallRange().map { createSomeText() }.joinToString("\n")
    var f9: Color =
        Color(red = random.nextInt(255), green = random.nextInt(255), blue = random.nextInt(255))
}
