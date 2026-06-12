package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import androidx.core.view.isVisible
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.SplitPageStitchDiagnostics
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import tachiyomi.i18n.MR
import kotlin.math.max

/**
 * View of the ViewPager that contains a page of a chapter.
 */
@SuppressLint("ViewConstructor")
class PagerPageHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val page: ReaderPage,
    private var extraPage: ReaderPage? = null,
    splitPages: List<ReaderPage> = emptyList(),
    secondSplitPages: List<ReaderPage> = emptyList(),
    private val splitCandidate: ReaderPage? = null,
) : ReaderPageImageView(readerThemedContext), ViewPagerAdapter.PositionableView {

    private val firstPages = splitPages.ifEmpty { listOf(page) }
    private val secondPages = secondSplitPages.ifEmpty { listOfNotNull(extraPage) }
    private val displayedPages = firstPages + secondPages
    private val splitDetectionPage = firstPages.last()
    private val joinedItem = PagerViewerAdapter.JoinedItem(
        first = page,
        second = extraPage,
        splitPages = splitPages,
        secondSplitPages = secondSplitPages,
        splitCandidate = splitCandidate,
    )

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item
        get() = joinedItem

    /**
     * Loading progress bar to indicate the current progress.
     */
    private var progressIndicator: ReaderProgressIndicator? = null // = ReaderProgressIndicator(readerThemedContext)

    /**
     * Error layout to show when the image fails to load.
     */
    private var errorLayout: ReaderErrorBinding? = null

    private val scope = MainScope()

    /**
     * Job for loading the page and processing changes to the page's status.
     */
    private val loadJobs = mutableListOf<Job>()
    private val readyPages = mutableSetOf<ReaderPage>()
    private var rendered = false
    private var detectionStarted = false

    init {
        (displayedPages + listOfNotNull(splitCandidate)).distinct().forEach { readerPage ->
            loadJobs += scope.launch {
                loadPageAndProcessStatus(readerPage, readerPage == splitCandidate)
            }
        }
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressIndicator?.hide()
        loadJobs.forEach { it.cancel() }
        loadJobs.clear()
    }

    private fun initProgressIndicator() {
        if (progressIndicator == null) {
            progressIndicator = ReaderProgressIndicator(context)
            addView(progressIndicator)
        }
    }

    /**
     * Loads the page and processes changes to the page's status.
     *
     * Returns immediately if the page has no PageLoader.
     * Otherwise, this function does not return. It will continue to process status changes until
     * the Job is cancelled.
     */
    private suspend fun loadPageAndProcessStatus(page: ReaderPage, detectionOnly: Boolean) {
        val loader = page.chapter.pageLoader ?: return
        supervisorScope {
            launchIO {
                loader.loadPage(page)
            }
            page.statusFlow.collectLatest { state ->
                when (state) {
                    Page.State.Queue -> if (!detectionOnly) setQueued()
                    Page.State.LoadPage -> if (!detectionOnly) setLoading()
                    Page.State.DownloadImage -> {
                        if (!detectionOnly) {
                            setDownloading()
                            page.progressFlow.collectLatest { value ->
                                progressIndicator?.setProgress(value)
                            }
                        }
                    }
                    Page.State.Ready -> onPageReady(page)
                    is Page.State.Error -> {
                        if (detectionOnly) {
                            viewer.onSplitPageStitchDetection(splitDetectionPage, false)
                            progressIndicator?.hide()
                        } else {
                            setError(state.error)
                        }
                    }
                }
            }
        }
    }

    private fun onPageReady(readyPage: ReaderPage) {
        readyPages += readyPage
        if (splitCandidate != null) {
            if (
                !detectionStarted &&
                readyPages.contains(splitDetectionPage) &&
                readyPages.contains(splitCandidate)
            ) {
                detectionStarted = true
                scope.launch {
                    detectSplitPageStitch(splitCandidate)
                    progressIndicator?.hide()
                }
            }
            return
        }

        if (!rendered && readyPages.containsAll(displayedPages)) {
            rendered = true
            scope.launch { setImage() }
        }
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        initProgressIndicator()
        progressIndicator?.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        initProgressIndicator()
        progressIndicator?.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is downloading.
     */
    private fun setDownloading() {
        initProgressIndicator()
        progressIndicator?.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is ready.
     */
    private suspend fun setImage() {
        if (extraPage == null) {
            progressIndicator?.setProgress(0)
        } else {
            progressIndicator?.setProgress(95)
        }

        try {
            val blacklistedPages = viewer.activity.viewModel.findBlacklistedPages(firstPages)
                ?: viewer.activity.viewModel.findBlacklistedPages(secondPages)
            if (blacklistedPages != null) {
                withUIContext { viewer.onPagesBlacklisted(blacklistedPages) }
                return
            }
            val (source, isAnimated, background) = withIOContext {
                if (firstPages.size > 1 || secondPages.size > 1) {
                    val firstSource = buildLogicalPageSource(firstPages)
                    val secondSource = secondPages.takeIf { it.isNotEmpty() }?.let(::buildLogicalPageSource)
                    val itemSource = mergePages(firstSource, secondSource)
                    val background = if (viewer.config.automaticBackground) {
                        ImageUtil.chooseBackground(context, itemSource.peek())
                    } else {
                        null
                    }
                    Triple(itemSource, false, background)
                } else {
                    val streamFn = page.stream ?: error("Page stream is unavailable")
                    val streamFn2 = extraPage?.stream
                    streamFn().buffered(16).use { source ->
                        if (extraPage != null) {
                            streamFn2?.invoke()?.buffered(16)
                        } else {
                            null
                        }.use { source2 ->
                            val itemSource = if (viewer.config.dualPageSplit) {
                                process(page, Buffer().readFrom(source))
                            } else {
                                mergePages(Buffer().readFrom(source), source2?.let { Buffer().readFrom(it) })
                            }
                            // SY <--
                            val isAnimated = ImageUtil.isAnimatedAndSupported(itemSource)
                            val background = if (!isAnimated && viewer.config.automaticBackground) {
                                ImageUtil.chooseBackground(context, itemSource.peek())
                            } else {
                                null
                            }
                            Triple(itemSource, isAnimated, background)
                        }
                    }
                }
            }
            withUIContext {
                setImage(
                    source,
                    isAnimated,
                    Config(
                        zoomDuration = viewer.config.doubleTapAnimDuration,
                        minimumScaleType = viewer.config.imageScaleType,
                        cropBorders = viewer.config.imageCropBorders,
                        zoomStartPosition = viewer.config.imageZoomType,
                        landscapeZoom = viewer.config.landscapeZoom,
                    ),
                )
                if (!isAnimated) {
                    pageBackground = background
                }
                removeErrorLayout()
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
            withUIContext {
                setError(e)
            }
        }
    }

    private fun buildLogicalPageSource(pages: List<ReaderPage>): BufferedSource {
        if (pages.size == 1) {
            val streamFn = pages.single().stream ?: error("Page stream is unavailable")
            return streamFn().buffered(16).use { Buffer().readFrom(it) }
        }

        val bitmaps = pages.map { readerPage ->
            val streamFn = readerPage.stream ?: error("Page stream is unavailable")
            streamFn().buffered(16).use { source ->
                decodeImage(Buffer().readFrom(source)) ?: error("Cannot decode split page")
            }
        }
        return try {
            ImageUtil.mergeBitmapsVertically(
                bitmaps,
                viewer.config.pageCanvasColor,
                ::updateProgress,
            )
        } finally {
            bitmaps.forEach { it.recycle() }
        }
    }

    private suspend fun detectSplitPageStitch(candidate: ReaderPage) {
        val detectorConfig = viewer.config.splitPageDetectorConfig()
        val detection = withIOContext {
            val firstStream = splitDetectionPage.stream ?: return@withIOContext null
            val secondStream = candidate.stream ?: return@withIOContext null
            firstStream().buffered(16).use { first ->
                secondStream().buffered(16).use { second ->
                    val firstSource = Buffer().readFrom(first)
                    val secondSource = Buffer().readFrom(second)
                    if (
                        ImageUtil.isAnimatedAndSupported(firstSource) ||
                        ImageUtil.isAnimatedAndSupported(secondSource)
                    ) {
                        return@withIOContext null
                    }
                    val firstBitmap = decodeImage(firstSource, sampleSize = 8) ?: return@withIOContext null
                    val secondBitmap = decodeImage(secondSource, sampleSize = 8) ?: return@withIOContext null
                    try {
                        val diagnostics = SplitPageDetector.analyze(firstBitmap, secondBitmap, detectorConfig)
                        SplitPageStitchDetection(
                            diagnostics = diagnostics,
                            config = detectorConfig,
                        )
                    } finally {
                        firstBitmap.recycle()
                        secondBitmap.recycle()
                    }
                }
            }
        }
        viewer.onSplitPageStitchDetection(
            splitDetectionPage,
            detection?.diagnostics?.stitches == true,
            candidate,
            detection,
        )
    }

    internal data class SplitPageStitchDetection(
        val diagnostics: SplitPageStitchDiagnostics,
        val config: SplitPageDetector.Config,
    )

    private fun process(page: ReaderPage, imageSource: BufferedSource): BufferedSource {
        if (viewer.config.dualPageRotateToFit) {
            return rotateDualPage(imageSource)
        }

        if (!viewer.config.dualPageSplit) {
            return imageSource
        }

        if (page is InsertPage) {
            return splitInHalf(imageSource)
        }

        val isDoublePage = ImageUtil.isWideImage(imageSource)
        if (!isDoublePage) {
            return imageSource
        }

        onPageSplit(page)

        return splitInHalf(imageSource)
    }

    private fun rotateDualPage(imageSource: BufferedSource): BufferedSource {
        val isDoublePage = ImageUtil.isWideImage(imageSource)
        return if (isDoublePage) {
            val rotation = if (viewer.config.dualPageRotateToFitInvert) -90f else 90f
            ImageUtil.rotateImage(imageSource, rotation)
        } else {
            imageSource
        }
    }

    private fun mergePages(imageSource: BufferedSource, imageSource2: BufferedSource?): BufferedSource {
        // Handle adding a center margin to wide images if requested
        if (imageSource2 == null) {
            return handleWideImage(imageSource)
        }

        if (page.fullPage) return imageSource
        if (ImageUtil.isAnimatedAndSupported(imageSource)) {
            page.fullPage = true
            splitDoublePages()
            return imageSource
        } else if (ImageUtil.isAnimatedAndSupported(imageSource2)) {
            page.isolatedPage = true
            extraPage?.fullPage = true
            splitDoublePages()
            return imageSource
        }

        val imageBitmap = decodeImage(imageSource)
        if (imageBitmap == null) {
            imageSource2.close()
            page.fullPage = true
            splitDoublePages()
            logcat(LogPriority.ERROR) { "Cannot combine pages" }
            return imageSource
        }

        scope.launch { progressIndicator?.setProgress(96) }
        if (imageBitmap.height < imageBitmap.width) {
            imageSource2.close()
            page.fullPage = true
            splitDoublePages()
            return imageSource
        }

        val imageBitmap2 = decodeImage(imageSource2)
        if (imageBitmap2 == null) {
            imageSource2.close()
            extraPage?.fullPage = true
            page.isolatedPage = true
            splitDoublePages()
            logcat(LogPriority.ERROR) { "Cannot combine pages" }
            return imageSource
        }

        scope.launch { progressIndicator?.setProgress(97) }
        if (imageBitmap2.height < imageBitmap2.width) {
            imageSource2.close()
            extraPage?.fullPage = true
            page.isolatedPage = true
            splitDoublePages()
            return imageSource
        }

        val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages
        val centerMargin = calculateCenterMargin(imageBitmap.height, imageBitmap2.height)

        imageSource.close()
        imageSource2.close()

        return ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, centerMargin, viewer.config.pageCanvasColor) {
            updateProgress(it)
        }
    }

    private fun handleWideImage(imageSource: BufferedSource): BufferedSource {
        return if (
            !ImageUtil.isAnimatedAndSupported(imageSource) &&
            ImageUtil.isWideImage(imageSource) &&
            viewer.config.centerMarginType and PagerConfig.CenterMarginType.WIDE_PAGE_CENTER_MARGIN > 0 &&
            !viewer.config.imageCropBorders
        ) {
            ImageUtil.addHorizontalCenterMargin(imageSource, height, context)
        } else {
            imageSource
        }
    }

    private fun decodeImage(imageSource: BufferedSource, sampleSize: Int = 1): Bitmap? {
        return try {
            ImageDecoder.newInstance(imageSource.inputStream())?.decode(sampleSize = sampleSize)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cannot decode image" }
            null
        }
    }

    private fun calculateCenterMargin(height: Int, height2: Int): Int {
        return if (viewer.config.centerMarginType and PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN > 0 &&
            !viewer.config.imageCropBorders
        ) {
            96 / (this.height.coerceAtLeast(1) / max(height, height2).coerceAtLeast(1)).coerceAtLeast(1)
        } else {
            0
        }
    }

    private fun updateProgress(progress: Int) {
        scope.launch {
            if (progress == 100) {
                progressIndicator?.hide()
            } else {
                progressIndicator?.setProgress(progress)
            }
        }
    }

    private fun splitDoublePages() {
        scope.launch {
            delay(100)
            viewer.splitDoublePages(page)
            if (extraPage?.fullPage == true || page.fullPage) {
                extraPage = null
            }
        }
    }

    private fun splitInHalf(imageSource: BufferedSource): BufferedSource {
        var side = when {
            viewer is L2RPagerViewer && page is InsertPage -> ImageUtil.Side.RIGHT
            viewer !is L2RPagerViewer && page is InsertPage -> ImageUtil.Side.LEFT
            viewer is L2RPagerViewer && page !is InsertPage -> ImageUtil.Side.LEFT
            viewer !is L2RPagerViewer && page !is InsertPage -> ImageUtil.Side.RIGHT
            else -> error("We should choose a side!")
        }

        if (viewer.config.dualPageInvert) {
            side = when (side) {
                ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
                ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
            }
        }

        val sideMargin = if ((viewer.config.centerMarginType and PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN) >
            0 &&
            viewer.config.doublePages &&
            !viewer.config.imageCropBorders
        ) {
            48
        } else {
            0
        }

        return ImageUtil.splitInHalf(imageSource, side, sideMargin)
    }

    private fun onPageSplit(page: ReaderPage) {
        val newPage = InsertPage(page)
        viewer.onPageSplit(page, newPage)
    }

    /**
     * Called when the page has an error.
     */
    private fun setError(error: Throwable?) {
        progressIndicator?.hide()
        showErrorLayout(error)
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        progressIndicator?.hide()
    }

    /**
     * Called when an image fails to decode.
     */
    override fun onImageLoadError(error: Throwable?) {
        super.onImageLoadError(error)
        setError(error)
    }

    /**
     * Called when an image is zoomed in/out.
     */
    override fun onScaleChanged(newScale: Float) {
        super.onScaleChanged(newScale)
        viewer.activity.hideMenu()
    }

    private fun showErrorLayout(error: Throwable?): ReaderErrorBinding {
        if (errorLayout == null) {
            errorLayout = ReaderErrorBinding.inflate(LayoutInflater.from(context), this, true)
            errorLayout?.actionRetry?.viewer = viewer
            errorLayout?.actionRetry?.setOnClickListener {
                page.chapter.pageLoader?.retryPage(page)
            }
        }

        val imageUrl = page.imageUrl
        errorLayout?.actionOpenInWebView?.isVisible = imageUrl != null
        if (imageUrl != null) {
            if (imageUrl.startsWith("http", true)) {
                errorLayout?.actionOpenInWebView?.viewer = viewer
                errorLayout?.actionOpenInWebView?.setOnClickListener {
                    val sourceId = viewer.activity.viewModel.manga?.source

                    val intent = WebViewActivity.newIntent(context, imageUrl, sourceId)
                    context.startActivity(intent)
                }
            }
        }

        errorLayout?.errorMessage?.text = with(context) { error?.formattedMessage }
            ?: context.stringResource(MR.strings.decode_image_error)

        errorLayout?.root?.isVisible = true
        return errorLayout!!
    }

    /**
     * Removes the decode error layout from the holder, if found.
     */
    private fun removeErrorLayout() {
        errorLayout?.root?.isVisible = false
        errorLayout = null
    }
}
