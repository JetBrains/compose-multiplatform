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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.packFloats
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.stubbing.OngoingStubbing

class LongAnswer(private val value: Long) : Answer<Any> {
    override fun answer(invocation: InvocationOnMock?): Any = value
}

infix fun <T> OngoingStubbing<T>.doAnswer(offset: Offset): OngoingStubbing<T> {
    return thenAnswer(LongAnswer(packFloats(offset.x, offset.y)))
}