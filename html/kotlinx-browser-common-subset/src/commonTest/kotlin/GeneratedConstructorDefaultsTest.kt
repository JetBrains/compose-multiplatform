// Verifies constructor defaults on every target.
package kotlinx.browser.dom.constructors

import kotlinx.browser.dom.EventInit
import kotlinx.browser.dom.events.Event
import kotlin.test.Test

// A defaulted expect argument must resolve to the browser or JVM actual's default.
class GeneratedConstructorDefaultsTest {
    @Test
    fun defaultedConstructorArgumentIsCallable() {
        Event("portable")
        Event("portable", EventInit(bubbles = true))
    }
}
