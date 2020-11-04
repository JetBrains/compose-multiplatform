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

package androidx.compose.ui.tooling.preview.datasource

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

private val LOREM_IPSUM_SOURCE = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer sodales
laoreet commodo. Phasellus a purus eu risus elementum consequat. Aenean eu
elit ut nunc convallis laoreet non ut libero. Suspendisse interdum placerat
risus vel ornare. Donec vehicula, turpis sed consectetur ullamcorper, ante
nunc egestas quam, ultricies adipiscing velit enim at nunc. Aenean id diam
neque. Praesent ut lacus sed justo viverra fermentum et ut sem. Fusce
convallis gravida lacinia. Integer semper dolor ut elit sagittis lacinia.
Praesent sodales scelerisque eros at rhoncus. Duis posuere sapien vel ipsum
ornare interdum at eu quam. Vestibulum vel massa erat. Aenean quis sagittis
purus. Phasellus arcu purus, rutrum id consectetur non, bibendum at nibh.

Duis nec erat dolor. Nulla vitae consectetur ligula. Quisque nec mi est. Ut
quam ante, rutrum at pellentesque gravida, pretium in dui. Cras eget sapien
velit. Suspendisse ut sem nec tellus vehicula eleifend sit amet quis velit.
Phasellus quis suscipit nisi. Nam elementum malesuada tincidunt. Curabitur
iaculis pretium eros, malesuada faucibus leo eleifend a. Curabitur congue
orci in neque euismod a blandit libero vehicula.
""".trim().split(" ")

/**
 * Generate a Lorem Ipsum [words] long.
 */
private fun generateLoremIpsum(words: Int): String {
    var wordsUsed = 0
    val loremIpsumMaxSize = LOREM_IPSUM_SOURCE.size
    return generateSequence {
        LOREM_IPSUM_SOURCE[wordsUsed++ % loremIpsumMaxSize]
    }.take(words).joinToString(" ")
}

/**
 * [PreviewParameterProvider] with 1 value containing Lorem Ipsum.
 *
 * @param words Number of words from "Lorem Ipsum" to use.
 */
class LoremIpsum(private val words: Int) : PreviewParameterProvider<String> {
    // Unfortunately using default parameters seem to fail to be instantiated via reflection.
    // We can workaround it by creating the default constructor manually.
    constructor() : this(500)

    override val values: Sequence<String>
        get() = sequenceOf(generateLoremIpsum(words))
}
