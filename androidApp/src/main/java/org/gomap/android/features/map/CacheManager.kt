package org.gomap.android.features.map

import android.content.Context
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.OfflineManager

data class CacheFileStats(
    val bytes: Long = 0,
    val fileCount: Int = 0
)

class CacheManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        CachePreferences,
        Context.MODE_PRIVATE
    )

    var automaticCleanupEnabled: Boolean
        get() = preferences.getBoolean(AutomaticCleanupKey, true)
        set(value) {
            preferences.edit().putBoolean(AutomaticCleanupKey, value).apply()
        }

    suspend fun basemapStats(): CacheFileStats = withContext(Dispatchers.IO) {
        scanCacheFiles(mapLibreCacheFiles())
    }

    suspend fun dataCacheStats(): CacheFileStats = withContext(Dispatchers.IO) {
        scanCacheFiles(listOf(appContext.cacheDir))
    }

    suspend fun clearBasemapTiles(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        OfflineManager.getInstance(appContext).clearAmbientCache(
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() {
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }

                override fun onError(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(
                            Result.failure(IllegalStateException(message.ifBlank { "Unable to clear basemap tiles." }))
                        )
                    }
                }
            }
        )
    }

    suspend fun clearDataCaches(): CacheFileStats = withContext(Dispatchers.IO) {
        appContext.cacheDir.listFiles().orEmpty().forEach { file ->
            file.deleteRecursively()
        }
        scanCacheFiles(listOf(appContext.cacheDir))
    }

    suspend fun pruneOldDataCaches(nowMillis: Long = System.currentTimeMillis()) {
        if (!automaticCleanupEnabled) return
        withContext(Dispatchers.IO) {
            val cutoff = nowMillis - AutomaticCleanupAgeMillis
            appContext.cacheDir.walkBottomUp()
                .filter { it != appContext.cacheDir && it.lastModified() in 1 until cutoff }
                .forEach(File::delete)
        }
    }

    private fun mapLibreCacheFiles(): List<File> {
        return listOf(
            File(appContext.filesDir, "mbgl-offline.db"),
            File(appContext.filesDir, "mbgl-offline.db-wal"),
            File(appContext.filesDir, "mbgl-offline.db-shm")
        )
    }
}

internal fun scanCacheFiles(roots: List<File>): CacheFileStats {
    val files = roots.asSequence()
        .flatMap { root ->
            when {
                !root.exists() -> emptySequence()
                root.isFile -> sequenceOf(root)
                else -> root.walkTopDown().filter(File::isFile)
            }
        }
        .distinctBy(File::getAbsolutePath)
        .toList()
    return CacheFileStats(
        bytes = files.sumOf(File::length),
        fileCount = files.size
    )
}

internal fun formatCacheBytes(bytes: Long): String {
    return when {
        bytes < 1_024 -> "$bytes bytes"
        bytes < 1_048_576 -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
        bytes < 1_073_741_824 -> String.format(Locale.US, "%.2f MB", bytes / 1_048_576.0)
        else -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
    }
}

private const val CachePreferences = "gomap_cache_preferences"
private const val AutomaticCleanupKey = "automatic_cleanup"
private const val AutomaticCleanupAgeMillis = 7L * 24L * 60L * 60L * 1_000L
