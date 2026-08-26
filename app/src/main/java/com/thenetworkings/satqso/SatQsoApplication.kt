package com.thenetworkings.satqso

import android.app.Application
import com.thenetworkings.satqso.data.TleRefreshScheduler

class SatQsoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TleRefreshScheduler.schedule(this)
    }
}
