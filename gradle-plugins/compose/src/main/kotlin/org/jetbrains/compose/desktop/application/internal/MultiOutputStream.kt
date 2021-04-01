/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import java.io.FilterOutputStream
import java.io.OutputStream

internal class MultiOutputStream(
    mainStream: OutputStream,
    private val secondaryStream: OutputStream
) : FilterOutputStream(mainStream) {
    override fun write(b: Int) {
        super.write(b)
        secondaryStream.write(b)
    }

    override fun flush() {
        super.flush()
        secondaryStream.flush()
    }

    override fun close() {
        try {
            super.close()
        } finally {
            secondaryStream.close()
        }
    }
}

internal fun OutputStream.alsoOutputTo(secondaryStream: OutputStream): OutputStream =
    MultiOutputStream(this, secondaryStream)