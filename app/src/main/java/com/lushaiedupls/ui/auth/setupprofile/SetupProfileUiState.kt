package com.lushaiedupls.ui.auth.setupprofile

import android.net.Uri
import com.lushaiedupls.ui.auth.signup.GenderOption

data class SetupProfileUiState(
    val username: String = "",
    val phone: String = "",
    val address: String = "",
    val gender: GenderOption? = null,
    val avatarUri: Uri? = null,
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successRoute: String? = null,
)
