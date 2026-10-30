/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.model

// Prepares the text data before measuring rendering.
fun textFixture(
    escaped: Boolean = false, count: Int = 1_000,
    foo: String = BAVARIA_FOO, bar: String = BAVARIA_BAR,
): TextData = TextData(
    (1..count).map { index ->
        val prefix = if (escaped) "<item $index> & \"quoted\" 'single' 😀 café α: " else "Item $index: "
        TextPair(prefix + foo, prefix + bar)
    },
)
