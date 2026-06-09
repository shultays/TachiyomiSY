package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class SetMangaStitchSettings(
    private val mangaRepository: MangaRepository,
) {
    suspend fun setStitchSplitPageMode(manga: Manga, mode: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageMode = mode,
            ),
        )
    }

    suspend fun setStitchSplitPageThreshold(manga: Manga, threshold: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageThreshold = threshold,
            ),
        )
    }

    suspend fun setStitchSplitPageMaxHeightRatio(manga: Manga, ratio: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageMaxHeightRatio = ratio,
            ),
        )
    }

    suspend fun setStitchSplitPageMinimumEdgeVariance(manga: Manga, variance: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageMinimumEdgeVariance = variance,
            ),
        )
    }

    suspend fun setStitchSplitPageContinuityMultiplier(manga: Manga, multiplier: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageContinuityMultiplier = multiplier,
            ),
        )
    }

    suspend fun setStitchSplitPageMinimumContinuity(manga: Manga, continuity: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageMinimumContinuity = continuity,
            ),
        )
    }

    suspend fun setStitchSplitPageSampleColumns(manga: Manga, columns: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageSampleColumns = columns,
            ),
        )
    }

    suspend fun setStitchSplitPageSampleRows(manga: Manga, rows: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageSampleRows = rows,
            ),
        )
    }

    suspend fun setStitchSplitPageMaximumStripHeight(manga: Manga, maximumHeight: Int): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageMaximumStripHeight = maximumHeight,
            ),
        )
    }

    suspend fun setAllStitchSettings(
        manga: Manga,
        mode: Int? = null,
        threshold: Int? = null,
        maxHeightRatio: Int? = null,
        minimumEdgeVariance: Int? = null,
        continuityMultiplier: Int? = null,
        minimumContinuity: Int? = null,
        sampleColumns: Int? = null,
        sampleRows: Int? = null,
        maximumStripHeight: Int? = null,
    ): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = manga.id,
                stitchSplitPageMode = mode,
                stitchSplitPageThreshold = threshold,
                stitchSplitPageMaxHeightRatio = maxHeightRatio,
                stitchSplitPageMinimumEdgeVariance = minimumEdgeVariance,
                stitchSplitPageContinuityMultiplier = continuityMultiplier,
                stitchSplitPageMinimumContinuity = minimumContinuity,
                stitchSplitPageSampleColumns = sampleColumns,
                stitchSplitPageSampleRows = sampleRows,
                stitchSplitPageMaximumStripHeight = maximumStripHeight,
            ),
        )
    }
}
