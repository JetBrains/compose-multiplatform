import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

actual fun timestampMs(): Long {
    return (NSDate().timeIntervalSince1970() * 1000).toLong()
}
