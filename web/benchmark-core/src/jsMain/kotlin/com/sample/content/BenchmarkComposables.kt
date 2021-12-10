/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.sample.content

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
internal fun AddItems(count: Int) {
    repeat(count) {
        Div {
            P {
                Text("$it")
            }
            P {
                Text("$it")
            }
            Div {
                P {
                    Text("$it")
                }
            }
        }
    }
}

@Composable
internal fun AddItems(list: List<String>) {
    list.forEach {
        Div {
            P {
                Text(it)
            }
            P {
                Text(it)
            }
            Div {
                P {
                    Text(it)
                }
            }
        }
    }
}
