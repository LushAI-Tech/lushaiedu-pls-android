package com.lushaiedupls.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lushaiedupls.LushAIEduApp
import com.lushaiedupls.ui.auth.selectrole.UserRole
import kotlinx.coroutines.launch

class LushFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = application as? LushAIEduApp ?: return
        if (!app.isContainerReady) return
        app.container.pushTokenSynchronizer.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        PushNotificationHelper.show(applicationContext, message)
        val app = application as? LushAIEduApp ?: return
        if (!app.isContainerReady) return
        val action = PushActions.parse(message.data) ?: return
        if (action !is PushAction.PendingApproval) return
        if (app.container.userSessionStore.getRole() != UserRole.Admin) return
        app.container.pushRouter.offer(action)
        app.container.applicationScope.launch {
            app.container.adminRepository.refreshPendingApprovalCount()
        }
    }
}
