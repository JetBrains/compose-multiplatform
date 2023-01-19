/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.common

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

@Serializable
public class Id(public val id: String)

@Composable
fun Abc(id: Id) {
    println("Id = $id")
}

@Composable
fun App() {
    Abc(Id("1000"))
}
