package eu.kanade.tachiyomi.ui.reader.model

data class SplitPageStitchDiagnostics(
    val stitches: Boolean,
    val reason: Reason,
    val firstWidth: Int,
    val firstHeight: Int,
    val secondWidth: Int,
    val secondHeight: Int,
    val combinedHeightRatio: Float?,
    val maximumCombinedHeightRatio: Float,
    val seamDifference: Float?,
    val seamThreshold: Float,
    val localDifference: Float?,
    val edgeVariance: Float?,
    val minimumEdgeVariance: Float,
    val continuityLimit: Float?,
    val continuityMultiplier: Float,
    val minimumContinuity: Float,
    val sampleColumns: Int,
    val sampleRows: Int,
) {
    enum class Reason {
        STITCHED,
        INVALID_SIZE,
        WIDTH_MISMATCH,
        COMBINED_IMAGE_TOO_TALL,
        EDGE_VARIANCE_TOO_LOW,
        SEAM_DIFFERENCE_TOO_HIGH,
        CONTINUITY_TOO_LOW,
    }
}
