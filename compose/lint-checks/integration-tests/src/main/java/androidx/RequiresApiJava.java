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

package androidx;

import androidx.annotation.RequiresApi;

/**
 * Character.isSurrogate requires API 19. Prior to addressing b/202415535, lint did not detect the
 * presence of @RequiresApi annotation in an outer class, and incorrectly flagged MyStaticClass
 * as needing a @RequiresApi annotation.
 */
@RequiresApi(19)
final class RequiresApiJava {

    // RequiresApi annotation should not be needed here; already present in containing class
    // @RequiresApi(19)
    public static final class MyStaticClass {

        private MyStaticClass() {
            // Not instantiable.
        }

        static int myStaticMethod(char c) {
            Character.isSurrogate(c);
            return 0;
        }
    }
}
