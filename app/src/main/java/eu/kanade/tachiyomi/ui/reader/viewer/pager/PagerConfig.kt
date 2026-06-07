package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Color
import androidx.annotation.ColorInt
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration used by pager viewers.
 */
class PagerConfig(
    private val viewer: PagerViewer,
    scope: CoroutineScope,
    readerPreferences: ReaderPreferences = Injekt.get(),
) : ViewerConfig(readerPreferences, scope) {

    var theme = readerPreferences.readerTheme.get()
        private set

    var automaticBackground = false
        private set

    var dualPageSplitChangedListener: ((Boolean) -> Unit)? = null

    var reloadChapterListener: ((Boolean) -> Unit)? = null

    var splitPageMergeChangedListener: (() -> Unit)? = null

    var imageScaleType = 1
        private set

    var imageZoomType = ReaderPageImageView.ZoomStartPosition.LEFT
        private set

    var imageCropBorders = false
        private set

    var navigateToPan = false
        private set

    var landscapeZoom = false
        private set

    // SY -->
    var usePageTransitions = false

    var shiftDoublePage = false

    var doublePages =
        readerPreferences.pageLayout.get() == PageLayout.DOUBLE_PAGES && !readerPreferences.dualPageSplitPaged.get()
        set(value) {
            field = value
            if (!value) {
                shiftDoublePage = false
            }
        }

    var invertDoublePages = false

    var autoDoublePages = readerPreferences.pageLayout.get() == PageLayout.AUTOMATIC

    @ColorInt
    var pageCanvasColor = Color.WHITE

    var centerMarginType = CenterMarginType.NONE

    var splitPageMergeMode = SplitPageMergeMode.NONE

    var splitPageMergeThreshold = SplitPageMergeThreshold.DEFAULT

    var splitPageMergeMaxHeightRatio = SplitPageMergeMaxHeightRatio.DEFAULT

    var splitPageMergeMinimumEdgeVariance = SplitPageMergeMinimumEdgeVariance.DEFAULT

    var splitPageMergeContinuityMultiplier = SplitPageMergeContinuityMultiplier.DEFAULT

    var splitPageMergeMinimumContinuity = SplitPageMergeMinimumContinuity.DEFAULT

    var splitPageMergeSampleColumns = SplitPageMergeSampleColumns.DEFAULT

    var splitPageMergeSampleRows = SplitPageMergeSampleRows.DEFAULT

    // SY <--

    init {
        readerPreferences.readerTheme
            .register(
                {
                    theme = it
                    automaticBackground = it == 3
                },
                { imagePropertyChangedListener?.invoke() },
            )

        readerPreferences.imageScaleType
            .register({ imageScaleType = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.zoomStart
            .register({ zoomTypeFromPreference(it) }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.cropBorders
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.navigateToPan
            .register({ navigateToPan = it })

        readerPreferences.landscapeZoom
            .register({ landscapeZoom = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.navigationModePager
            .register({ navigationMode = it }, { updateNavigation(navigationMode) })

        readerPreferences.pagerNavInverted
            .register({ tappingInverted = it }, { navigator.invertMode = it })
        readerPreferences.pagerNavInverted.changes()
            .drop(1)
            .onEach { navigationModeChangedListener?.invoke() }
            .launchIn(scope)

        readerPreferences.dualPageSplitPaged
            .register(
                { dualPageSplit = it },
                {
                    imagePropertyChangedListener?.invoke()
                    dualPageSplitChangedListener?.invoke(it)
                },
            )

        readerPreferences.dualPageInvertPaged
            .register({ dualPageInvert = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.dualPageRotateToFit
            .register(
                { dualPageRotateToFit = it },
                { imagePropertyChangedListener?.invoke() },
            )

        readerPreferences.dualPageRotateToFitInvert
            .register(
                { dualPageRotateToFitInvert = it },
                { imagePropertyChangedListener?.invoke() },
            )

        // SY -->
        readerPreferences.pageTransitionsPager
            .register({ usePageTransitions = it }, { imagePropertyChangedListener?.invoke() })
        readerPreferences.readerTheme
            .register(
                {
                    themeToColor(it)
                },
                {
                    themeToColor(it)
                    reloadChapterListener?.invoke(doublePages)
                },
            )
        readerPreferences.pageLayout
            .register(
                {
                    autoDoublePages = it == PageLayout.AUTOMATIC
                    if (!autoDoublePages) {
                        doublePages = it == PageLayout.DOUBLE_PAGES && dualPageSplit == false
                    }
                },
                {
                    autoDoublePages = it == PageLayout.AUTOMATIC
                    if (!autoDoublePages) {
                        doublePages = it == PageLayout.DOUBLE_PAGES && dualPageSplit == false
                    }
                    reloadChapterListener?.invoke(doublePages)
                },
            )

        readerPreferences.centerMarginType
            .register({ centerMarginType = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.splitPageMergeMode
            .register(
                { splitPageMergeMode = SplitPageMergeMode.normalize(it) },
                {
                    splitPageMergeMode = SplitPageMergeMode.normalize(it)
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeThreshold
            .register(
                { splitPageMergeThreshold = it },
                {
                    splitPageMergeThreshold = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeMaxHeightRatio
            .register(
                { splitPageMergeMaxHeightRatio = it },
                {
                    splitPageMergeMaxHeightRatio = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeMinimumEdgeVariance
            .register(
                { splitPageMergeMinimumEdgeVariance = it },
                {
                    splitPageMergeMinimumEdgeVariance = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeContinuityMultiplier
            .register(
                { splitPageMergeContinuityMultiplier = it },
                {
                    splitPageMergeContinuityMultiplier = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeMinimumContinuity
            .register(
                { splitPageMergeMinimumContinuity = it },
                {
                    splitPageMergeMinimumContinuity = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeSampleColumns
            .register(
                { splitPageMergeSampleColumns = it },
                {
                    splitPageMergeSampleColumns = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageMergeSampleRows
            .register(
                { splitPageMergeSampleRows = it },
                {
                    splitPageMergeSampleRows = it
                    splitPageMergeChangedListener?.invoke()
                },
            )

        readerPreferences.invertDoublePages
            .register({ invertDoublePages = it && dualPageSplit == false }, { imagePropertyChangedListener?.invoke() })
        // SY <--
    }

    private fun zoomTypeFromPreference(value: Int) {
        imageZoomType = when (value) {
            // Auto
            1 -> when (viewer) {
                is L2RPagerViewer -> ReaderPageImageView.ZoomStartPosition.LEFT
                is R2LPagerViewer -> ReaderPageImageView.ZoomStartPosition.RIGHT
                else -> ReaderPageImageView.ZoomStartPosition.CENTER
            }
            // Left
            2 -> ReaderPageImageView.ZoomStartPosition.LEFT
            // Right
            3 -> ReaderPageImageView.ZoomStartPosition.RIGHT
            // Center
            else -> ReaderPageImageView.ZoomStartPosition.CENTER
        }
    }

    override var navigator: ViewerNavigation = defaultNavigation()
        set(value) {
            field = value.also { it.invertMode = this.tappingInverted }
        }

    override fun defaultNavigation(): ViewerNavigation {
        return when (viewer) {
            is VerticalPagerViewer -> LNavigation()
            else -> RightAndLeftNavigation()
        }
    }

    override fun updateNavigation(navigationMode: Int) {
        navigator = when (navigationMode) {
            0 -> defaultNavigation()
            1 -> LNavigation()
            2 -> KindlishNavigation()
            3 -> EdgeNavigation()
            4 -> RightAndLeftNavigation()
            5 -> DisabledNavigation()
            else -> defaultNavigation()
        }
        navigationModeChangedListener?.invoke()
    }

    object CenterMarginType {
        const val NONE = 0
        const val DOUBLE_PAGE_CENTER_MARGIN = 1
        const val WIDE_PAGE_CENTER_MARGIN = 2
        const val DOUBLE_AND_WIDE_CENTER_MARGIN = 3
    }

    object PageLayout {
        const val SINGLE_PAGE = 0
        const val DOUBLE_PAGES = 1
        const val AUTOMATIC = 2
    }

    object SplitPageMergeMode {
        const val NONE = 0
        const val SEVERAL = 1

        fun normalize(value: Int) = if (value == NONE) NONE else SEVERAL
    }

    object SplitPageMergeThreshold {
        const val MIN = 5
        const val MAX = 100
        const val DEFAULT = 21
    }

    object SplitPageMergeMaxHeightRatio {
        const val MIN = 100
        const val MAX = 400
        const val DEFAULT = 175
    }

    object SplitPageMergeMinimumEdgeVariance {
        const val MIN = 0
        const val MAX = 100
        const val DEFAULT = 8
    }

    object SplitPageMergeContinuityMultiplier {
        const val MIN = 100
        const val MAX = 800
        const val DEFAULT = 300
    }

    object SplitPageMergeMinimumContinuity {
        const val MIN = 0
        const val MAX = 200
        const val DEFAULT = 35
    }

    object SplitPageMergeSampleColumns {
        const val MIN = 8
        const val MAX = 128
        const val DEFAULT = 64
    }

    object SplitPageMergeSampleRows {
        const val MIN = 1
        const val MAX = 16
        const val DEFAULT = 6
    }

    internal fun splitPageDetectorConfig() = SplitPageDetector.Config(
        thresholdPercent = splitPageMergeThreshold,
        maxCombinedHeightRatioPercent = splitPageMergeMaxHeightRatio,
        minimumEdgeVarianceTenthsPercent = splitPageMergeMinimumEdgeVariance,
        continuityMultiplierPercent = splitPageMergeContinuityMultiplier,
        minimumContinuityTenthsPercent = splitPageMergeMinimumContinuity,
        sampleColumns = splitPageMergeSampleColumns,
        sampleRows = splitPageMergeSampleRows,
    )

    fun themeToColor(theme: Int) {
        pageCanvasColor = when (theme) {
            1 -> Color.BLACK
            2 -> 0x202125
            else -> Color.WHITE
        }
    }
}
