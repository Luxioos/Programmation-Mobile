package com.example.ontimego

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Classe qui hérite de WorkManager pour gérer les notifications
 */
class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val endAddress = inputData.getString("endAddress") ?: "Adresse inconnue"
        val appointmentTime = inputData.getString("appointmentTime") ?: "Heure inconnue"
        sendNotification(endAddress, appointmentTime)
        return Result.success()
    }

    private fun sendNotification(endAddress: String, appointmentTime: String) {
        val channelId = "travel_notifications"
        val channelName = "Notifications de trajets"
        val notificationId = 1
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Vous avez rendez-vous dans une heure")
            .setContentText("Trajet à destination de $endAddress , rendez-vous à $appointmentTime")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
