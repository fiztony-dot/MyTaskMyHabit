package com.example.mistareasapp.core.notifications.habits

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HabitoAlertaScheduler {
    fun registrar(context: Context) {
        val request = PeriodicWorkRequestBuilder<HabitoAlertaWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HabitoAlertaWorker.NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

object HabitoAlertaNavigation {
    private var pendingHabitoId: Long? = null

    fun setPending(habitoId: Long) {
        pendingHabitoId = habitoId
    }

    fun consume(): Long? = pendingHabitoId.also { pendingHabitoId = null }
}