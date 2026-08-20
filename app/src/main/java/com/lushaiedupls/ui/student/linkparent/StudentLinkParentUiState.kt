package com.lushaiedupls.ui.student.linkparent

import com.lushaiedupls.data.remote.dto.LinkTokenResponse
import com.lushaiedupls.data.remote.dto.ParentLinkOut

data class StudentLinkParentUiState(
    val token: LinkTokenResponse? = null,
    val remainingSeconds: Int = 0,
    val parents: List<ParentLinkOut> = emptyList(),
    val isIssuing: Boolean = false,
    val isLoadingParents: Boolean = false,
    val errorMessage: String? = null,
)
