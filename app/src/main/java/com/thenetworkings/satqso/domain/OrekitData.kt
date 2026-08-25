package com.thenetworkings.satqso.domain

import org.orekit.data.ClasspathCrawler
import org.orekit.data.DataContext

object OrekitData {
    @Volatile
    private var initialized = false

    fun initialize() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            DataContext.getDefault()
                .dataProvidersManager
                .addProvider(ClasspathCrawler("orekit-data/tai-utc.dat"))
            initialized = true
        }
    }
}
