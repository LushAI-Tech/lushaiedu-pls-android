package com.lushaiedupls.ui.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val message: String? = null,
)
