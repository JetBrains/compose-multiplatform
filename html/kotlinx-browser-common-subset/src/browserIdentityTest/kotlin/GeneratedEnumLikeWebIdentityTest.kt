// Verifies browser identity for generated enum-like values on each leaf web target.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.browser.dom.enumlike

import kotlinx.browser.JsAny
import kotlinx.browser.dom.AUTO
import kotlinx.browser.dom.BORDER
import kotlinx.browser.dom.CENTER
import kotlinx.browser.dom.COMPLETE
import kotlinx.browser.dom.CSSBoxType
import kotlinx.browser.dom.CanPlayTypeResult
import kotlinx.browser.dom.DocumentReadyState
import kotlinx.browser.dom.EMPTY
import kotlinx.browser.dom.END
import kotlinx.browser.dom.LOADING
import kotlinx.browser.dom.NEAREST
import kotlinx.browser.dom.SMOOTH
import kotlinx.browser.dom.ScrollBehavior
import kotlinx.browser.dom.ScrollLogicalPosition
import kotlinx.browser.dom.ShadowRootMode
import kotlinx.browser.dom.START
import kotlinx.browser.dom.SUBTITLES
import kotlinx.browser.dom.TextTrackKind
import kotlinx.browser.dom.OPEN
import org.w3c.dom.AUTO as browserAUTO
import org.w3c.dom.BORDER as browserBORDER
import org.w3c.dom.CENTER as browserCENTER
import org.w3c.dom.CSSBoxType as BrowserCSSBoxType
import org.w3c.dom.COMPLETE as browserCOMPLETE
import org.w3c.dom.CanPlayTypeResult as BrowserCanPlayTypeResult
import org.w3c.dom.DocumentReadyState as BrowserDocumentReadyState
import org.w3c.dom.EMPTY as browserEMPTY
import org.w3c.dom.END as browserEND
import org.w3c.dom.LOADING as browserLOADING
import org.w3c.dom.NEAREST as browserNEAREST
import org.w3c.dom.OPEN as browserOPEN
import org.w3c.dom.SMOOTH as browserSMOOTH
import org.w3c.dom.START as browserSTART
import org.w3c.dom.SUBTITLES as browserSUBTITLES
import org.w3c.dom.ScrollBehavior as BrowserScrollBehavior
import org.w3c.dom.ScrollLogicalPosition as BrowserScrollLogicalPosition
import org.w3c.dom.ShadowRootMode as BrowserShadowRootMode
import org.w3c.dom.TextTrackKind as BrowserTextTrackKind
import kotlin.test.Test
import kotlin.test.assertTrue

// Facade enum-like values must be identical to the browser values they forward.
class GeneratedEnumLikeWebIdentityTest {
    @Test
    fun facadeValuesAreTheBrowsersOwn() {
        assertSame(BrowserScrollBehavior.browserAUTO, ScrollBehavior.AUTO)
        assertSame(BrowserScrollBehavior.browserSMOOTH, ScrollBehavior.SMOOTH)
        assertSame(BrowserScrollLogicalPosition.browserSTART, ScrollLogicalPosition.START)
        assertSame(BrowserScrollLogicalPosition.browserCENTER, ScrollLogicalPosition.CENTER)
        assertSame(BrowserScrollLogicalPosition.browserEND, ScrollLogicalPosition.END)
        assertSame(BrowserScrollLogicalPosition.browserNEAREST, ScrollLogicalPosition.NEAREST)
        assertSame(BrowserDocumentReadyState.browserLOADING, DocumentReadyState.LOADING)
        assertSame(BrowserDocumentReadyState.browserCOMPLETE, DocumentReadyState.COMPLETE)
        assertSame(BrowserCSSBoxType.browserBORDER, CSSBoxType.BORDER)
        assertSame(BrowserShadowRootMode.browserOPEN, ShadowRootMode.OPEN)
        assertSame(BrowserTextTrackKind.browserSUBTITLES, TextTrackKind.SUBTITLES)
        // The value whose IDL literal is the empty string, matched to its Kotlin name by position.
        assertSame(BrowserCanPlayTypeResult.browserEMPTY, CanPlayTypeResult.EMPTY)
    }

    /** Reuses the common helper, so identity means the same thing here as it does on every target. */
    private fun assertSame(browser: JsAny, facade: JsAny) {
        assertTrue(areIdentical(browser, facade))
    }
}
