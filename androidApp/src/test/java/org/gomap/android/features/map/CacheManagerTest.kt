package org.gomap.android.features.map

import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.writeBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheManagerTest {
    @Test
    fun scanCacheFilesCountsNestedFilesAndBytes() {
        val root = Files.createTempDirectory("gomap-cache-test")
        try {
            root.resolve("first.bin").writeBytes(ByteArray(1_024))
            root.resolve("nested").createDirectory()
                .resolve("second.bin").writeBytes(ByteArray(512))

            assertEquals(
                CacheFileStats(bytes = 1_536, fileCount = 2),
                scanCacheFiles(listOf(root.toFile()))
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun formatCacheBytesUsesReadableUnits() {
        assertEquals("0 bytes", formatCacheBytes(0))
        assertEquals("1.5 KB", formatCacheBytes(1_536))
        assertEquals("2.00 MB", formatCacheBytes(2L * 1_048_576L))
    }
}
