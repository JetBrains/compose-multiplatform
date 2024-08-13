/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.nav_cupcake.data

import cupcake.composeapp.generated.resources.Res
import cupcake.composeapp.generated.resources.*

object DataSource {
    val flavors = listOf(
        Res.string.vanilla,
        Res.string.red_velvet,
        Res.string.chocolate,
        Res.string.salted_caramel,
        Res.string.coffee
    )

    val quantityOptions = listOf(
        Pair(Res.string.one_cupcake, 1),
        Pair(Res.string.six_cupcakes, 6),
        Pair(Res.string.twelve_cupcakes, 12)
    )
}
