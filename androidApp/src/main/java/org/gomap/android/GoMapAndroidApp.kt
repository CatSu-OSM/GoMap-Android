package org.gomap.android

import android.app.Application
import org.maplibre.android.MapLibre

class GoMapAndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
