package example.imageviewer

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

// TODO: Provide a crossfade akin to this:

//AnimatedContent(targetState = navigationStack.lastWithIndex(), transitionSpec = {
//    val previousIdx = initialState.index
//    val currentIdx = targetState.index
//    val multiplier = if (previousIdx < currentIdx) 1 else -1
//    if (initialState.value is GalleryPage && targetState.value is MemoryPage) {
//        fadeIn() with fadeOut(tween(durationMillis = 500, 500))
//    } else if (initialState.value is MemoryPage && targetState.value is GalleryPage) {
//        fadeIn() with fadeOut(tween(delayMillis = 150))
//    } else {
//        slideInHorizontally { w -> multiplier * w } with
//                slideOutHorizontally { w -> multiplier * -1 * w }
//    }
//}

val enterT: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(700)
    )
}

val exitT: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    fadeOut(
        tween(2000)
    )
}

val popExitT: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(700)
    )
}

val popEnterT: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    fadeIn()
}