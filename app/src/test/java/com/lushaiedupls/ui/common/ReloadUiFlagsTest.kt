package com.lushaiedupls.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ReloadUiFlagsTest {

    @Test
    fun initialSkeletonKeepsLoading() {
        assertEquals(true to false, reloadUiFlags(currentlyLoading = true, hasContent = false))
    }

    @Test
    fun pullWithContentUsesRefreshing() {
        assertEquals(false to true, reloadUiFlags(currentlyLoading = false, hasContent = true))
    }

    @Test
    fun pullOnEmptyListAfterLoadUsesRefreshing() {
        assertEquals(false to true, reloadUiFlags(currentlyLoading = false, hasContent = false))
    }
}
