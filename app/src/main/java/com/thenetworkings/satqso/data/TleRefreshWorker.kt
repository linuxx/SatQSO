package com.thenetworkings.satqso.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient

class TleRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        CelestrakTleDataSource(
            httpClient = OkHttpClient(),
            cache = SharedPreferencesTleCache(applicationContext),
        ).fetchTles(CuratedSatelliteCatalog.satellites.mapTo(mutableSetOf()) { it.noradId })
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}
