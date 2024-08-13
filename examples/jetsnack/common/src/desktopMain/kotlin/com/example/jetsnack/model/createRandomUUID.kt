package com.example.jetsnack.model

import java.util.*

actual fun createRandomUUID(): Long {
    return UUID.randomUUID().mostSignificantBits
}