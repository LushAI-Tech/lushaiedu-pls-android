package com.lushaiedupls.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.lushaiedupls.ui.theme.BgWhite

/**
 * Role shell with bottom navigation and centered max-width content on larger screens.
 *
 * Hides the bottom bar while the IME is open so form screens keep enough room to scroll.
 */
@Composable
fun AdaptiveRoleScaffold(
    showNav: Boolean,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding(),
        containerColor = BgWhite,
        bottomBar = {
            if (showNav && !imeVisible) {
                bottomBar()
            }
        },
    ) { innerPadding ->
        LushShellContentWidth(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
