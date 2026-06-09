package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Bitmap
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import eu.kanade.tachiyomi.ui.reader.model.SplitPageStitchDiagnostics
import kotlin.math.abs
import kotlin.math.max

internal object SplitPageDetector {

    data class Config(
        val mode: Int,
        val thresholdPercent: Int,
        val maxCombinedHeightRatioPercent: Int,
        val minimumEdgeVarianceTenthsPercent: Int,
        val continuityMultiplierPercent: Int,
        val minimumContinuityTenthsPercent: Int,
        val sampleColumns: Int,
        val sampleRows: Int,
        val maximumStripHeightPercent: Int,
    )

    fun analyze(upper: Bitmap, lower: Bitmap, config: Config): SplitPageStitchDiagnostics {
        val maximumCombinedHeightRatio = config.maxCombinedHeightRatioPercent / 100f
        val seamThreshold = config.thresholdPercent.coerceIn(1, 100) / 100f
        val minimumEdgeVariance = config.minimumEdgeVarianceTenthsPercent.coerceAtLeast(0) / 1000f
        val minimumContinuity = config.minimumContinuityTenthsPercent.coerceAtLeast(0) / 1000f
        val continuityMultiplier = config.continuityMultiplierPercent.coerceAtLeast(0) / 100f
        val maximumStripHeightRatio = config.maximumStripHeightPercent.coerceAtLeast(0) / 100f
        val combinedHeightRatio = if (upper.width > 0) {
            (upper.height + lower.height).toFloat() / upper.width
        } else {
            null
        }

        fun result(
            stitches: Boolean,
            reason: SplitPageStitchDiagnostics.Reason,
            seamDifference: Float? = null,
            localDifference: Float? = null,
            edgeVariance: Float? = null,
            continuityLimit: Float? = null,
            stripHeightRatio: Float? = null,
        ) = SplitPageStitchDiagnostics(
            stitches = stitches,
            reason = reason,
            firstWidth = upper.width,
            firstHeight = upper.height,
            secondWidth = lower.width,
            secondHeight = lower.height,
            combinedHeightRatio = combinedHeightRatio,
            maximumCombinedHeightRatio = maximumCombinedHeightRatio,
            seamDifference = seamDifference,
            seamThreshold = seamThreshold,
            localDifference = localDifference,
            edgeVariance = edgeVariance,
            minimumEdgeVariance = minimumEdgeVariance,
            continuityLimit = continuityLimit,
            continuityMultiplier = continuityMultiplier,
            minimumContinuity = minimumContinuity,
            sampleColumns = config.sampleColumns,
            sampleRows = config.sampleRows,
            stripHeightRatio = stripHeightRatio,
            maximumStripHeightRatio = maximumStripHeightRatio,
        )

        if (upper.width < 2 || upper.height < 2 || lower.height < 2) {
            return result(false, SplitPageStitchDiagnostics.Reason.INVALID_SIZE)
        }
        if (upper.width != lower.width) {
            return result(false, SplitPageStitchDiagnostics.Reason.WIDTH_MISMATCH)
        }
        if (!isCombinedHeightAllowed(upper.width, upper.height + lower.height, config)) {
            return result(false, SplitPageStitchDiagnostics.Reason.COMBINED_IMAGE_TOO_TALL)
        }

        if (config.mode == PagerConfig.SplitPageStitchMode.APPEND_SHORT_PAGES) {
            val stripHeightRatio = lower.height.toFloat() / upper.height
            val appends = shouldAppendShortPage(
                firstWidth = upper.width,
                firstHeight = upper.height,
                secondWidth = lower.width,
                secondHeight = lower.height,
                config = config,
            )
            return result(
                stitches = appends,
                reason = when {
                    appends -> SplitPageStitchDiagnostics.Reason.SHORT_PAGE_APPENDED
                    stripHeightRatio > maximumStripHeightRatio -> SplitPageStitchDiagnostics.Reason.STRIP_TOO_TALL
                    else -> SplitPageStitchDiagnostics.Reason.COMBINED_IMAGE_NOT_PORTRAIT
                },
                stripHeightRatio = stripHeightRatio,
            )
        }

        val seamDifference = seamDifference(upper, lower, config)
        val localDifference = (
            internalEdgeDifference(upper, atBottom = true, config) +
                internalEdgeDifference(lower, atBottom = false, config)
            ) / 2f
        val seamVariance = edgeVariance(upper, lower, config)

        // Flat white/black borders frequently occur between intentional pages.
        if (seamVariance < minimumEdgeVariance) {
            return result(
                false,
                SplitPageStitchDiagnostics.Reason.EDGE_VARIANCE_TOO_LOW,
                seamDifference,
                localDifference,
                seamVariance,
            )
        }

        val continuityLimit = max(minimumContinuity, localDifference * continuityMultiplier)
        if (seamDifference > seamThreshold) {
            return result(
                false,
                SplitPageStitchDiagnostics.Reason.SEAM_DIFFERENCE_TOO_HIGH,
                seamDifference,
                localDifference,
                seamVariance,
                continuityLimit,
            )
        }
        if (seamDifference > continuityLimit) {
            return result(
                false,
                SplitPageStitchDiagnostics.Reason.CONTINUITY_TOO_LOW,
                seamDifference,
                localDifference,
                seamVariance,
                continuityLimit,
            )
        }
        return result(
            true,
            SplitPageStitchDiagnostics.Reason.STITCHED,
            seamDifference,
            localDifference,
            seamVariance,
            continuityLimit,
        )
    }

    fun isCombinedHeightAllowed(width: Int, totalHeight: Int, config: Config): Boolean =
        width > 0 && totalHeight.toFloat() / width <= config.maxCombinedHeightRatioPercent / 100f

    fun shouldAppendShortPage(
        firstWidth: Int,
        firstHeight: Int,
        secondWidth: Int,
        secondHeight: Int,
        config: Config,
    ): Boolean {
        return firstWidth >= 2 &&
            firstHeight >= 2 &&
            secondHeight >= 2 &&
            firstWidth == secondWidth &&
            isShortPageHeightAllowed(firstHeight, secondHeight, config) &&
            isCombinedPageShapeAllowed(firstWidth, firstHeight + secondHeight, config)
    }

    fun isShortPageHeightAllowed(firstHeight: Int, nextHeight: Int, config: Config): Boolean {
        return firstHeight >= 2 &&
            nextHeight >= 2 &&
            nextHeight.toFloat() / firstHeight <= config.maximumStripHeightPercent / 100f
    }

    fun isCombinedPageShapeAllowed(width: Int, totalHeight: Int, config: Config): Boolean {
        if (width <= 0 || totalHeight <= 0) return false
        val maximumRatio = config.maxCombinedHeightRatioPercent / 100f
        val portraitRatio = totalHeight.toFloat() / width
        val spreadRatio = totalHeight.toFloat() / (width / 2f)
        return portraitRatio in 1f..maximumRatio || spreadRatio in 1f..maximumRatio
    }

    private fun seamDifference(upper: Bitmap, lower: Bitmap, config: Config): Float {
        var difference = 0L
        var samples = 0
        val sampleRows = config.sampleRows.coerceIn(1, minOf(upper.height, lower.height))
        val sampleColumns = config.sampleColumns.coerceAtLeast(2)
        repeat(sampleRows) { row ->
            val upperY = (upper.height - sampleRows + row).coerceIn(0, upper.height - 1)
            val lowerY = row.coerceAtMost(lower.height - 1)
            repeat(sampleColumns) { column ->
                val x = column * (upper.width - 1) / (sampleColumns - 1)
                difference += colorDifference(upper.getPixel(x, upperY), lower.getPixel(x, lowerY))
                samples++
            }
        }
        return difference.toFloat() / (samples * 255f * 3f)
    }

    private fun internalEdgeDifference(bitmap: Bitmap, atBottom: Boolean, config: Config): Float {
        var difference = 0L
        var samples = 0
        val sampleRows = config.sampleRows.coerceIn(1, bitmap.height - 1)
        val sampleColumns = config.sampleColumns.coerceAtLeast(2)
        repeat(sampleRows) { row ->
            val y = if (atBottom) {
                (bitmap.height - sampleRows + row).coerceIn(1, bitmap.height - 1)
            } else {
                row.coerceIn(0, (bitmap.height - 2).coerceAtLeast(0))
            }
            val adjacentY = if (atBottom) y - 1 else (y + 1).coerceAtMost(bitmap.height - 1)
            repeat(sampleColumns) { column ->
                val x = column * (bitmap.width - 1) / (sampleColumns - 1)
                difference += colorDifference(bitmap.getPixel(x, y), bitmap.getPixel(x, adjacentY))
                samples++
            }
        }
        return difference.toFloat() / (samples * 255f * 3f)
    }

    private fun edgeVariance(upper: Bitmap, lower: Bitmap, config: Config): Float {
        val sampleColumns = config.sampleColumns.coerceAtLeast(2)
        val colors = ArrayList<Int>(sampleColumns * 2)
        repeat(sampleColumns) { column ->
            val x = column * (upper.width - 1) / (sampleColumns - 1)
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
