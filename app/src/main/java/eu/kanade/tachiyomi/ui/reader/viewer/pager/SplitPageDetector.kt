package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Bitmap
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object SplitPageDetector {

    private const val SAMPLE_COLUMNS = 64
    private const val SAMPLE_ROWS = 6

    fun shouldMerge(upper: Bitmap, lower: Bitmap): Boolean {
        if (upper.width != lower.width || upper.width < 8 || upper.height < 2 || lower.height < 2) return false

        val upperRatio = upper.height.toFloat() / upper.width
        val lowerRatio = lower.height.toFloat() / lower.width
        val bothPageLike = upperRatio in 1.1f..2.0f &&
            lowerRatio in 1.1f..2.0f &&
            min(upper.height, lower.height).toFloat() / max(upper.height, lower.height) > 0.8f

        val seamDifference = seamDifference(upper, lower)
        val localDifference = (
            internalEdgeDifference(upper, atBottom = true) +
                internalEdgeDifference(lower, atBottom = false)
            ) / 2f
        val seamVariance = edgeVariance(upper, lower)

        // Flat white/black borders frequently occur between intentional pages.
        if (seamVariance < 0.008f) return false

        val shortFragment = min(upperRatio, lowerRatio) < 0.75f
        val allowedDifference = when {
            shortFragment -> 0.13f
            bothPageLike -> 0.045f
            else -> 0.085f
        }
        val continuityLimit = max(0.035f, localDifference * if (shortFragment) 3f else 2f)
        return seamDifference <= allowedDifference && seamDifference <= continuityLimit
    }

    private fun seamDifference(upper: Bitmap, lower: Bitmap): Float {
        var difference = 0L
        var samples = 0
        repeat(SAMPLE_ROWS) { row ->
            val upperY = (upper.height - SAMPLE_ROWS + row).coerceIn(0, upper.height - 1)
            val lowerY = row.coerceAtMost(lower.height - 1)
            repeat(SAMPLE_COLUMNS) { column ->
                val x = column * (upper.width - 1) / (SAMPLE_COLUMNS - 1)
                difference += colorDifference(upper.getPixel(x, upperY), lower.getPixel(x, lowerY))
                samples++
            }
        }
        return difference.toFloat() / (samples * 255f * 3f)
    }

    private fun internalEdgeDifference(bitmap: Bitmap, atBottom: Boolean): Float {
        var difference = 0L
        var samples = 0
        repeat(SAMPLE_ROWS) { row ->
            val y = if (atBottom) {
                (bitmap.height - SAMPLE_ROWS + row).coerceIn(1, bitmap.height - 1)
            } else {
                row.coerceIn(0, (bitmap.height - 2).coerceAtLeast(0))
            }
            val adjacentY = if (atBottom) y - 1 else (y + 1).coerceAtMost(bitmap.height - 1)
            repeat(SAMPLE_COLUMNS) { column ->
                val x = column * (bitmap.width - 1) / (SAMPLE_COLUMNS - 1)
                difference += colorDifference(bitmap.getPixel(x, y), bitmap.getPixel(x, adjacentY))
                samples++
            }
        }
        return difference.toFloat() / (samples * 255f * 3f)
    }

    private fun edgeVariance(upper: Bitmap, lower: Bitmap): Float {
        val colors = ArrayList<Int>(SAMPLE_COLUMNS * 2)
        repeat(SAMPLE_COLUMNS) { column ->
            val x = column * (upper.width - 1) / (SAMPLE_COLUMNS - 1)
            colors += upper.getPixel(x, upper.height - 1)
            colors += lower.getPixel(x, 0)
        }
        val mean = colors.sumOf { it.red + it.green + it.blue }.toFloat() / (colors.size * 3f)
        return colors.sumOf {
            (abs(it.red - mean) + abs(it.green - mean) + abs(it.blue - mean)).toDouble()
        }.toFloat() / (colors.size * 3f * 255f)
    }

    private fun colorDifference(first: Int, second: Int): Long =
        abs(first.red - second.red).toLong() +
            abs(first.green - second.green) +
            abs(first.blue - second.blue)
}
