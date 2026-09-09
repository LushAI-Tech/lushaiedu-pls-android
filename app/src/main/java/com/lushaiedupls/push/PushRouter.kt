package com.lushaiedupls.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PushRouter {
    private val _action = MutableStateFlow<PushAction?>(null)
    val action: StateFlow<PushAction?> = _action.asStateFlow()

    fun offer(action: PushAction) {
        _action.value = action
    }

    fun consume() {
        _action.value = null
    }
}
