import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams

fun main() {

	val urlParams = URLSearchParams(window.location.search)

	val app = urlParams.get("app") ?: "composeApp"

	when (app) {
		"composeApp" -> reactInComposeAppExample()
		"reactApp" -> composeInReactAppExample()
	}
}