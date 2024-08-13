package com.example.jetsnack.model

import org.jetbrains.skiko.currentNanoTime

actual fun createRandomUUID(): Long {
    // TODO: implement. Create random UUID
    return currentNanoTime()
}