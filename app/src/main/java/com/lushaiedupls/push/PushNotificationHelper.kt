package com.lushaiedupls.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.lushaiedupls.MainActivity
import com.lushaiedupls.R

object PushNotificationHelper {
    const val CHANNEL_ID = "lushai_default"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.push_channel_description)
            },
        )
    }

    fun show(context: Context, message: RemoteMessage) {
        val notification = message.notification
        val data = message.data
        if (PushActions.isTest(data)) return

        val type = PushActions.typeOf(data)
        val title: String
        val body: String
        when {
            notification != null -> {
                title = notification.title?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.app_name)
                body = notification.body.orEmpty()
            }
            type == PushActions.TYPE_PERIOD_REMINDER -> {
                title = context.getString(R.string.teacher_period_reminder_title)
                body = context.getString(R.string.teacher_period_reminder_body)
            }
            type == PushActions.TYPE_PENDING_APPROVAL -> {
                title = pendingApprovalTitle(context, data[PushActions.EXTRA_ROLE])
                body = context.getString(R.string.push_pending_approval_body)
            }
            else -> return
        }

        ensureChannel(context)
        val launch = PendingIntent.getActivity(
            context,
            pendingIntentRequestCode(message),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                data.forEach { (key, value) -> putExtra(key, value) }
                type?.let { putExtra(PushActions.EXTRA_PUSH_TYPE, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(launch)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val id = message.messageId?.hashCode()
            ?: data[PushActions.EXTRA_USER_ID]?.hashCode()
            ?: data[PushActions.EXTRA_SLOT_ID]?.hashCode()
            ?: System.currentTimeMillis().toInt()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, built)
        }
    }

    private fun pendingApprovalTitle(context: Context, role: String?): String = when (role?.uppercase()) {
        "STUDENT" -> context.getString(R.string.push_pending_approval_student_title)
        "PARENT" -> context.getString(R.string.push_pending_approval_parent_title)
        else -> context.getString(R.string.push_pending_approval_user_title)
    }

    private fun pendingIntentRequestCode(message: RemoteMessage): Int {
        val data = message.data
        val key = data[PushActions.EXTRA_USER_ID]
            ?: data[PushActions.EXTRA_NOTIFICATION_ID]
            ?: data[PushActions.EXTRA_SLOT_ID]
            ?: message.messageId
            ?: PushActions.typeOf(data)
        return key?.hashCode() ?: 0
    }
}
