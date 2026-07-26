package org.gomap.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.gomap.android.features.map.CacheManager
import org.maplibre.android.MapLibre

class GoMapAndroidApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        applicationScope.launch {
            CacheManager(this@GoMapAndroidApp).pruneOldDataCaches()
        }
    }
}
