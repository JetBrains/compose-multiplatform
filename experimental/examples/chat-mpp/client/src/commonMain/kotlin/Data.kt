/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Message(
    val user: User,
    val timeMs: Long,
    val text: String,
)

data class User(
    val name: String,
    val pictureColor: Color = Color(
        red = Random.nextInt(0xff),
        green = Random.nextInt(0xff),
        blue = Random.nextInt(0xff)
    ),
)
