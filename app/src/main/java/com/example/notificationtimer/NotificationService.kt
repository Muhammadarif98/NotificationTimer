package com.example.notificationtimer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class NotificationService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ваш код для отправки уведомления
        intent?.getStringExtra("titleKey")?.let {
            intent?.getIntExtra("delayInSeconds", 0)?.let { it1 ->
                sendNotification(
                    this,
                    it,
                    intent?.getStringExtra("descriptionKey")!!,
                    it1,
                    intent?.getStringExtra("location")!!,
                    getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                )
            }
        }
        return START_STICKY
    }
}

