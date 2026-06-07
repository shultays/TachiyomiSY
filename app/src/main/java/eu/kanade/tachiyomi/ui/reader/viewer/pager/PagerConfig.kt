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

    var splitPageStitchChangedListener: (() -> Unit)? = null

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

    var splitPageStitchMode = SplitPageStitchMode.NONE

    var splitPageStitchThreshold = SplitPageStitchThreshold.DEFAULT

    var splitPageStitchMaxHeightRatio = SplitPageStitchMaxHeightRatio.DEFAULT

    var splitPageStitchMinimumEdgeVariance = SplitPageStitchMinimumEdgeVariance.DEFAULT

    var splitPageStitchContinuityMultiplier = SplitPageStitchContinuityMultiplier.DEFAULT

    var splitPageStitchMinimumContinuity = SplitPageStitchMinimumContinuity.DEFAULT

    var splitPageStitchSampleColumns = SplitPageStitchSampleColumns.DEFAULT

    var splitPageStitchSampleRows = SplitPageStitchSampleRows.DEFAULT

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

        readerPreferences.splitPageStitchMode
            .register(
                { splitPageStitchMode = SplitPageStitchMode.normalize(it) },
                {
                    splitPageStitchMode = SplitPageStitchMode.normalize(it)
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchThreshold
            .register(
                { splitPageStitchThreshold = it },
                {
                    splitPageStitchThreshold = it
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchMaxHeightRatio
            .register(
                { splitPageStitchMaxHeightRatio = it },
                {
                    splitPageStitchMaxHeightRatio = it
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchMinimumEdgeVariance
            .register(
                { splitPageStitchMinimumEdgeVariance = it },
                {
                    splitPageStitchMinimumEdgeVariance = it
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchContinuityMultiplier
            .register(
                { splitPageStitchContinuityMultiplier = it },
                {
                    splitPageStitchContinuityMultiplier = it
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchMinimumContinuity
            .register(
                { splitPageStitchMinimumContinuity = it },
                {
                    splitPageStitchMinimumContinuity = it
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchSampleColumns
            .register(
                { splitPageStitchSampleColumns = it },
                {
                    splitPageStitchSampleColumns = it
                    splitPageStitchChangedListener?.invoke()
                },
            )

        readerPreferences.splitPageStitchSampleRows
            .register(
                { splitPageStitchSampleRows = it },
                {
                    splitPageStitchSampleRows = it
                    splitPageStitchChangedListener?.invoke()
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

    object SplitPageStitchMode {
        const val NONE = 0
        const val SEVERAL = 1

        fun normalize(value: Int) = if (value == NONE) NONE else SEVERAL
    }

    object SplitPageStitchThreshold {
        const val MIN = 5
        const val MAX = 100
        const val DEFAULT = 21
    }

    object SplitPageStitchMaxHeightRatio {
        const val MIN = 100
        const val MAX = 400
        const val DEFAULT = 175
    }

    object SplitPageStitchMinimumEdgeVariance {
        const val MIN = 0
        const val MAX = 100
        const val DEFAULT = 8
    }

    object SplitPageStitchContinuityMultiplier {
        const val MIN = 100
        const val MAX = 800
        const val DEFAULT = 300
    }

    object SplitPageStitchMinimumContinuity {
        const val MIN = 0
        const val MAX = 200
        const val DEFAULT = 35
    }

    object SplitPageStitchSampleColumns {
        const val MIN = 8
        const val MAX = 128
        const val DEFAULT = 64
    }

    object SplitPageStitchSampleRows {
        const val MIN = 1
        const val MAX = 16
        const val DEFAULT = 6
    }

    internal fun splitPageDetectorConfig() = SplitPageDetector.Config(
        thresholdPercent = splitPageStitchThreshold,
        maxCombinedHeightRatioPercent = splitPageStitchMaxHeightRatio,
        minimumEdgeVarianceTenthsPercent = splitPageStitchMinimumEdgeVariance,
        continuityMultiplierPercent = splitPageStitchContinuityMultiplier,
        minimumContinuityTenthsPercent = splitPageStitchMinimumContinuity,
        sampleColumns = splitPageStitchSampleColumns,
        sampleRows = splitPageStitchSampleRows,
    )

    fun themeToColor(theme: Int) {
        pageCanvasColor = when (theme) {
            1 -> Color.BLACK
            2 -> 0x202125
            else -> Color.WHITE
        }
    }
}
