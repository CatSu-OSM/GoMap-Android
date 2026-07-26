package org.gomap.android.features.presets

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.PathParser

private data class SvgPaths(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val paths: List<Path>
)

@Composable
fun PresetIcon(
    iconName: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val svg = remember(iconName) {
        iconName
            ?.takeIf { it.startsWith("maki-") || it.startsWith("temaki-") }
            ?.let { name ->
                runCatching {
                    context.assets.open("presets/icons/$name.svg")
                        .bufferedReader()
                        .use { parseSvg(it.readText()) }
                }.getOrNull()
            }
    }
    if (svg == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Place,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Canvas(modifier) {
        val scale = minOf(
            size.width / svg.viewportWidth,
            size.height / svg.viewportHeight
        )
        val left = (size.width - svg.viewportWidth * scale) / 2f
        val top = (size.height - svg.viewportHeight * scale) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint.toArgb()
            style = Paint.Style.FILL
        }
        drawContext.canvas.nativeCanvas.run {
            save()
            translate(left, top)
            scale(scale, scale)
            svg.paths.forEach { drawPath(it, paint) }
            restore()
        }
    }
}

private fun parseSvg(source: String): SvgPaths {
    val viewBox = Regex("""viewBox\s*=\s*["']([^"']+)["']""")
        .find(source)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.split(Regex("""[\s,]+"""))
        ?.map(String::toFloat)
        ?: listOf(0f, 0f, 15f, 15f)
    val paths = Regex("""<path\b[^>]*\bd\s*=\s*["']([^"']+)["'][^>]*/?>""")
        .findAll(source)
        .mapNotNull { match ->
            val data = match.groupValues[1]
                .replace("&#xA;", " ")
                .replace("&#x9;", " ")
                .replace("&quot;", "\"")
            PathParser.createPathFromPathData(data)
        }
        .toList()
    return SvgPaths(
        viewportWidth = viewBox[2],
        viewportHeight = viewBox[3],
        paths = paths
    )
}
