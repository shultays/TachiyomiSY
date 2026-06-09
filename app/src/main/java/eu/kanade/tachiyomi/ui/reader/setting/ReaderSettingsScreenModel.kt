package eu.kanade.tachiyomi.ui.reader.setting

import cafe.adriel.voyager.core.model.ScreenModel
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.interactor.SetMangaStitchSettings
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ReaderSettingsScreenModel(
    readerState: StateFlow<ReaderViewModel.State>,
    val onChangeReadingMode: (ReadingMode) -> Unit,
    val onChangeOrientation: (ReaderOrientation) -> Unit,
    val onAddCurrentPageToBlacklist: () -> Unit,
    val onRemoveBlacklistedPage: (String) -> Unit,
    val onStitchSettingsChanged: (Manga) -> Unit,
    val preferences: ReaderPreferences = Injekt.get(),
    private val setMangaStitchSettings: SetMangaStitchSettings = Injekt.get(),
) : ScreenModel {

    val viewerFlow = readerState
        .map { it.viewer }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, null)

    val mangaFlow = readerState
        .map { it.manga }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, null)

    val blacklistedPagesFlow = readerState
        .map { it.blacklistedPages }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, emptyList())

    // SY -->
    fun setStitchSplitPageMode(mode: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageMode(manga, mode)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageMode = mode))
            }
        }
    }

    fun setStitchSplitPageThreshold(threshold: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageThreshold(manga, threshold)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageThreshold = threshold))
            }
        }
    }

    fun setStitchSplitPageMaxHeightRatio(ratio: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageMaxHeightRatio(manga, ratio)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageMaxHeightRatio = ratio))
            }
        }
    }

    fun setStitchSplitPageMinimumEdgeVariance(variance: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageMinimumEdgeVariance(manga, variance)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageMinimumEdgeVariance = variance))
            }
        }
    }

    fun setStitchSplitPageContinuityMultiplier(multiplier: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageContinuityMultiplier(manga, multiplier)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageContinuityMultiplier = multiplier))
            }
        }
    }

    fun setStitchSplitPageMinimumContinuity(continuity: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageMinimumContinuity(manga, continuity)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageMinimumContinuity = continuity))
            }
        }
    }

    fun setStitchSplitPageSampleColumns(columns: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageSampleColumns(manga, columns)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageSampleColumns = columns))
            }
        }
    }

    fun setStitchSplitPageSampleRows(rows: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageSampleRows(manga, rows)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageSampleRows = rows))
            }
        }
    }

    fun setStitchSplitPageMaximumStripHeight(maximumHeight: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageMaximumStripHeight(manga, maximumHeight)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageMaximumStripHeight = maximumHeight))
            }
        }
    }

    fun setStitchSplitPageMaximumStitchCount(maximumCount: Int) {
        val manga = mangaFlow.value ?: return
        ioCoroutineScope.launch {
            if (setMangaStitchSettings.setStitchSplitPageMaximumStitchCount(manga, maximumCount)) {
                onStitchSettingsChanged(manga.copy(stitchSplitPageMaximumStitchCount = maximumCount))
            }
        }
    }
    // SY <--
}
