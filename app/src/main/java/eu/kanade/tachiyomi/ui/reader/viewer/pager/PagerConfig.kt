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

    var splitPageStitchMaximumStripHeight = SplitPageStitchMaximumStripHeight.DEFAULT

    var splitPageStitchMaximumStitchCount = SplitPageStitchMaximumStitchCount.DEFAULT

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
        const val EDGE_MATCHING = 1
        const val APPEND_SHORT_PAGES = 2

        fun normalize(value: Int) = value.coerceIn(NONE, APPEND_SHORT_PAGES)
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
        const val DEFAULT = 600
    }

    object SplitPageStitchMinimumContinuity {
        const val MIN = 0
        const val MAX = 600
        const val DEFAULT = 200
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

    object SplitPageStitchMaximumStripHeight {
        const val MIN = 5
        const val MAX = 100
        const val DEFAULT = 25
    }

    object SplitPageStitchMaximumStitchCount {
        const val MIN = 2
        const val MAX = 10
        const val DEFAULT = 2
    }

    // SY -->
    fun loadMangaStitchSettings(
        manga: tachiyomi.domain.manga.model.Manga,
        notifyChanged: Boolean = true,
    ) {
        splitPageStitchMode = SplitPageStitchMode.normalize(manga.stitchSplitPageMode)
        splitPageStitchThreshold = manga.stitchSplitPageThreshold
        splitPageStitchMaxHeightRatio = manga.stitchSplitPageMaxHeightRatio
        splitPageStitchMinimumEdgeVariance = manga.stitchSplitPageMinimumEdgeVariance
        splitPageStitchContinuityMultiplier = manga.stitchSplitPageContinuityMultiplier
        splitPageStitchMinimumContinuity = manga.stitchSplitPageMinimumContinuity
        splitPageStitchSampleColumns = manga.stitchSplitPageSampleColumns
        splitPageStitchSampleRows = manga.stitchSplitPageSampleRows
        splitPageStitchMaximumStripHeight = manga.stitchSplitPageMaximumStripHeight
        splitPageStitchMaximumStitchCount = manga.stitchSplitPageMaximumStitchCount
        if (notifyChanged) {
            splitPageStitchChangedListener?.invoke()
        }
    }
    // SY <--

    internal fun splitPageDetectorConfig() = SplitPageDetector.Config(
        mode = splitPageStitchMode,
        thresholdPercent = splitPageStitchThreshold,
        maxCombinedHeightRatioPercent = splitPageStitchMaxHeightRatio,
        minimumEdgeVarianceTenthsPercent = splitPageStitchMinimumEdgeVariance,
        continuityMultiplierPercent = splitPageStitchContinuityMultiplier,
        minimumContinuityTenthsPercent = splitPageStitchMinimumContinuity,
        sampleColumns = splitPageStitchSampleColumns,
        sampleRows = splitPageStitchSampleRows,
        maximumStripHeightPercent = splitPageStitchMaximumStripHeight,
    )

    fun themeToColor(theme: Int) {
        pageCanvasColor = when (theme) {
            1 -> Color.BLACK
            2 -> 0x202125
            else -> Color.WHITE
        }
    }
}
