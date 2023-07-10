/*
 * Copyright 2021 The Android Open Source Project
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

package androidx

import androidx.annotation.RequiresApi

/*
 * Character.isSurrogate requires API 19. Prior to addressing b/202415535, the
 * ClassVerificationFailure detector only checked for the @RequiresApi annotation on a method's
 * immediate containing class, and did not factor in @RequiresApi annotations on outer class(es).
 *
 * This sample file covers various cases of nested @RequiresApi usage.
 */

// RequiresApi annotation not needed on MyStaticClass since it's already present in containing class
@RequiresApi(19)
internal class RequiresApiKotlinOuter19Passes {
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}

// @RequiresApi declaration on MyStaticMethod's immediate containing class
internal class RequiresApiKotlinInner19Passes {
     @RequiresApi(19)
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}

// Even though MyStaticClass declares a @RequiresApi that is too low, the outer containing class's
// definition of 19 will override it.
@RequiresApi(19)
internal class RequiresApiKotlinInner16Outer19Passes {
    @RequiresApi(16)
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}

internal class RequiresApiKotlinNoAnnotationFails {
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}

@RequiresApi(16)
internal class RequiresApiKotlinOuter16Fails {
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}

internal class RequiresApiKotlinInner16Fails {
    @RequiresApi(16)
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}

@RequiresApi(16)
internal class RequiresApiKotlinInner16Outer16Fails {
    @RequiresApi(16)
    object MyStaticClass {
        fun MyStaticMethod(c: Char): Int {
            Character.isSurrogate(c)
            return 0
        }
    }
}