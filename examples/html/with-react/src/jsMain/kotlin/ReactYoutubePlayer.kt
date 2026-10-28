@file:JsModule("react-youtube-lite")
@file:JsNonModule

import react.ComponentType
import react.Props

@JsName("ReactYouTubeLite")
external val reactPlayer: ComponentType<ReactYouTubeProps>

external interface ReactYouTubeProps : Props {
	var url: String
}
