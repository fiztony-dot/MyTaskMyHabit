package com.example.mistareasapp.core.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val WORK_NAME = "backup_diario_automatico"

    fun registrar(context: Context) {
        val ahora = LocalDateTime.now()
        val objetivo = ahora.toLocalDate().atTime(LocalTime.of(3, 0))
        val proximoBackup = if (ahora.isBefore(objetivo)) objetivo else objetivo.plusDays(1)
        val delayMinutos = Duration.between(ahora, proximoBackup).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<BackupAutoWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutos, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
