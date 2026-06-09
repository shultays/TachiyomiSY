package eu.kanade.tachiyomi.ui.reader.viewer.pager

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SplitPageDetectorTest {

    private val config = SplitPageDetector.Config(
        mode = PagerConfig.SplitPageStitchMode.APPEND_SHORT_PAGES,
        thresholdPercent = 21,
        maxCombinedHeightRatioPercent = 175,
        minimumEdgeVarianceTenthsPercent = 8,
        continuityMultiplierPercent = 600,
        minimumContinuityTenthsPercent = 200,
        sampleColumns = 64,
        sampleRows = 6,
        maximumStripHeightPercent = 25,
    )

    @Test
    fun `appends a short page at the configured boundary`() {
        assertTrue(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 1000,
                firstHeight = 1400,
                secondWidth = 1000,
                secondHeight = 350,
                config = config,
            ),
        )
    }

    @Test
    fun `rejects a following page taller than the configured boundary`() {
        assertFalse(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 1000,
                firstHeight = 1400,
                secondWidth = 1000,
                secondHeight = 351,
                config = config,
            ),
        )
    }

    @Test
    fun `accepts a following page up to the full first page height at one hundred percent`() {
        assertTrue(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 1000,
                firstHeight = 900,
                secondWidth = 1000,
                secondHeight = 900,
                config = config.copy(
                    maximumStripHeightPercent = 100,
                    maxCombinedHeightRatioPercent = 180,
                ),
            ),
        )
    }

    @Test
    fun `allows additional strips relative to the original page height`() {
        assertTrue(
            SplitPageDetector.isShortPageHeightAllowed(
                firstHeight = 1400,
                nextHeight = 300,
                config = config,
            ),
        )
    }

    @Test
    fun `rejects different image widths`() {
        assertFalse(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 1000,
                firstHeight = 1400,
                secondWidth = 999,
                secondHeight = 200,
                config = config,
            ),
        )
    }

    @Test
    fun `rejects a result exceeding the portrait height ratio`() {
        assertFalse(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 1000,
                firstHeight = 1600,
                secondWidth = 1000,
                secondHeight = 200,
                config = config,
            ),
        )
    }

    @Test
    fun `rejects a result that remains landscape`() {
        assertFalse(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 1000,
                firstHeight = 720,
                secondWidth = 1000,
                secondHeight = 180,
                config = config,
            ),
        )
    }

    @Test
    fun `accepts a dual page result using half width`() {
        assertTrue(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 2000,
                firstHeight = 1400,
                secondWidth = 2000,
                secondHeight = 200,
                config = config,
            ),
        )
    }

    @Test
    fun `rejects a dual page result exceeding the half width ratio`() {
        assertFalse(
            SplitPageDetector.shouldAppendShortPage(
                firstWidth = 2000,
                firstHeight = 1600,
                secondWidth = 2000,
                secondHeight = 200,
                config = config,
            ),
        )
    }
}
