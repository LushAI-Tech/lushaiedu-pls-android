package com.lushaiedupls.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/** Matches [com.lushaiedupls.ui.common.SlideFromRightOverlay] timing. */
object AppMotion {
    const val DurationMs = 280
    const val FadeMs = 200
}

private val SlideSpec = tween<IntOffset>(AppMotion.DurationMs, easing = FastOutSlowInEasing)
private val FadeInSpec = tween<Float>(AppMotion.FadeMs, easing = FastOutSlowInEasing)
private val FadeOutSpec = tween<Float>(160, easing = FastOutSlowInEasing)

private fun NavBackStackEntry.routePattern(): String = destination.route.orEmpty()

private fun isTabSwitch(
    from: String,
    to: String,
    tabRoutes: Set<String>,
): Boolean = from in tabRoutes && to in tabRoutes

private fun shouldFade(
    from: String,
    to: String,
    tabRoutes: Set<String>,
    fadeRoutes: Set<String>,
): Boolean = to in fadeRoutes || from in fadeRoutes || isTabSwitch(from, to, tabRoutes)

fun AnimatedContentTransitionScope<NavBackStackEntry>.lushEnterTransition(
    tabRoutes: Set<String> = emptySet(),
    fadeRoutes: Set<String> = emptySet(),
): EnterTransition {
    val from = initialState.routePattern()
    val to = targetState.routePattern()
    if (shouldFade(from, to, tabRoutes, fadeRoutes)) {
        return fadeIn(animationSpec = FadeInSpec)
    }
    return slideInHorizontally(
        animationSpec = SlideSpec,
        initialOffsetX = { fullWidth -> fullWidth },
    ) + fadeIn(animationSpec = FadeInSpec)
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.lushExitTransition(
    tabRoutes: Set<String> = emptySet(),
    fadeRoutes: Set<String> = emptySet(),
): ExitTransition {
    val from = initialState.routePattern()
    val to = targetState.routePattern()
    if (shouldFade(from, to, tabRoutes, fadeRoutes)) {
        return fadeOut(animationSpec = FadeOutSpec)
    }
    return slideOutHorizontally(
        animationSpec = SlideSpec,
        targetOffsetX = { fullWidth -> -fullWidth / 5 },
    ) + fadeOut(animationSpec = FadeOutSpec)
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.lushPopEnterTransition(
    tabRoutes: Set<String> = emptySet(),
    fadeRoutes: Set<String> = emptySet(),
): EnterTransition {
    val from = initialState.routePattern()
    val to = targetState.routePattern()
    if (shouldFade(from, to, tabRoutes, fadeRoutes)) {
        return fadeIn(animationSpec = FadeInSpec)
    }
    return slideInHorizontally(
        animationSpec = SlideSpec,
        initialOffsetX = { fullWidth -> -fullWidth / 5 },
    ) + fadeIn(animationSpec = FadeInSpec)
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.lushPopExitTransition(
    tabRoutes: Set<String> = emptySet(),
    fadeRoutes: Set<String> = emptySet(),
): ExitTransition {
    val from = initialState.routePattern()
    val to = targetState.routePattern()
    if (shouldFade(from, to, tabRoutes, fadeRoutes)) {
        return fadeOut(animationSpec = FadeOutSpec)
    }
    return slideOutHorizontally(
        animationSpec = SlideSpec,
        targetOffsetX = { fullWidth -> fullWidth },
    ) + fadeOut(animationSpec = FadeOutSpec)
}
