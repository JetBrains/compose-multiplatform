/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import kotlin.test.Test
import kotlin.test.assertEquals

class UserStateTest {
    @Test
    fun publicStatePreservesUnicodeScriptTextAndLargeIds() {
        val user = User(Long.MAX_VALUE, "Zoë \"Chen\"", "Design\nengineer", "日本", "</script><script>example</script> & \\ text")
        assertEquals(user, deserializeUser(serializeUser(user)))
    }
}
