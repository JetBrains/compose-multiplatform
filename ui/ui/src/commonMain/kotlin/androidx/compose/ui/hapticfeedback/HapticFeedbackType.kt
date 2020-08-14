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

package androidx.compose.ui.hapticfeedback

/**
 * Constants to be used to perform haptic feedback effects via
 * [HapticFeedback.performHapticFeedback].
 */
enum class HapticFeedbackType {
    /**
     * The user has performed a long press on an object that is resulting
     * in an action being performed.
     */
    LongPress,
    /**
     * The user has performed a selection/insertion handle move on text field.
     */
    TextHandleMove
}