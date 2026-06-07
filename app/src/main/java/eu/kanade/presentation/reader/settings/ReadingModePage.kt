package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.manga.model.readerOrientation
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun ColumnScope.ReadingModePage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.pref_category_for_this_series)
    val manga by screenModel.mangaFlow.collectAsState()

    val readingMode = remember(manga) { ReadingMode.fromPreference(manga?.readingMode?.toInt()) }
    SettingsChipRow(MR.strings.pref_category_reading_mode) {
        ReadingMode.entries.map {
            FilterChip(
                selected = it == readingMode,
                onClick = { screenModel.onChangeReadingMode(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val orientation = remember(manga) { ReaderOrientation.fromPreference(manga?.readerOrientation?.toInt()) }
    SettingsChipRow(MR.strings.rotation_type) {
        ReaderOrientation.entries.map {
            FilterChip(
                selected = it == orientation,
                onClick = { screenModel.onChangeOrientation(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val viewer by screenModel.viewerFlow.collectAsState()
    if (viewer is WebtoonViewer) {
        WebtoonViewerSettings(screenModel)
        // SY -->
        WebtoonWithGapsViewerSettings(screenModel)
        // SY <--
    } else {
        PagerViewerSettings(screenModel, viewer as? PagerViewer)
    }
}

@Composable
private fun ColumnScope.PagerViewerSettings(
    screenModel: ReaderSettingsScreenModel,
    viewer: PagerViewer?,
) {
    HeadingItem(MR.strings.pager_viewer)

    val navigationModePager by screenModel.preferences.navigationModePager.collectAsState()
    val pagerNavInverted by screenModel.preferences.pagerNavInverted.collectAsState()
    TapZonesItems(
        selected = navigationModePager,
        onSelect = screenModel.preferences.navigationModePager::set,
        invertMode = pagerNavInverted,
        onSelectInvertMode = screenModel.preferences.pagerNavInverted::set,
    )

    val imageScaleType by screenModel.preferences.imageScaleType.collectAsState()
    SettingsChipRow(MR.strings.pref_image_scale_type) {
        ReaderPreferences.ImageScaleType.mapIndexed { index, it ->
            FilterChip(
                selected = imageScaleType == index + 1,
                onClick = { screenModel.preferences.imageScaleType.set(index + 1) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    val zoomStart by screenModel.preferences.zoomStart.collectAsState()
    SettingsChipRow(MR.strings.pref_zoom_start) {
        ReaderPreferences.ZoomStart.mapIndexed { index, it ->
            FilterChip(
                selected = zoomStart == index + 1,
                onClick = { screenModel.preferences.zoomStart.set(index + 1) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    // SY -->
    val pageLayout by screenModel.preferences.pageLayout.collectAsState()
    SettingsChipRow(SYMR.strings.page_layout) {
        ReaderPreferences.PageLayouts.mapIndexed { index, it ->
            FilterChip(
                selected = pageLayout == index,
                onClick = { screenModel.preferences.pageLayout.set(index) },
                label = { Text(stringResource(it)) },
            )
        }
    }
    // SY <--

    CheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        pref = screenModel.preferences.cropBorders,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_landscape_zoom),
        pref = screenModel.preferences.landscapeZoom,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_navigate_pan),
        pref = screenModel.preferences.navigateToPan,
    )

    val dualPageSplitPaged by screenModel.preferences.dualPageSplitPaged.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_dual_page_split),
        pref = screenModel.preferences.dualPageSplitPaged,
    )

    if (dualPageSplitPaged) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_invert),
            pref = screenModel.preferences.dualPageInvertPaged,
        )
    }

    val dualPageRotateToFit by screenModel.preferences.dualPageRotateToFit.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_page_rotate),
        pref = screenModel.preferences.dualPageRotateToFit,
    )

    if (dualPageRotateToFit) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate_invert),
            pref = screenModel.preferences.dualPageRotateToFitInvert,
        )
    }

    // SY -->
    CheckboxItem(
        label = stringResource(MR.strings.pref_page_transitions),
        pref = screenModel.preferences.pageTransitionsPager,
    )

    CheckboxItem(
        label = stringResource(SYMR.strings.invert_double_pages),
        pref = screenModel.preferences.invertDoublePages,
    )

    val centerMarginType by screenModel.preferences.centerMarginType.collectAsState()
    SettingsChipRow(SYMR.strings.pref_center_margin) {
        ReaderPreferences.CenterMarginTypes.mapIndexed { index, it ->
            FilterChip(
                selected = centerMarginType == index,
                onClick = { screenModel.preferences.centerMarginType.set(index) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    val splitPageStitchMode by screenModel.preferences.splitPageStitchMode.collectAsState()
    SettingsChipRow(SYMR.strings.pref_stitch_split_pages) {
        ReaderPreferences.SplitPageStitchModes.mapIndexed { index, it ->
            FilterChip(
                selected = PagerConfig.SplitPageStitchMode.normalize(splitPageStitchMode) == index,
                onClick = { screenModel.preferences.splitPageStitchMode.set(index) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    if (PagerConfig.SplitPageStitchMode.normalize(splitPageStitchMode) != PagerConfig.SplitPageStitchMode.NONE) {
        val splitPageStitchThreshold by screenModel.preferences.splitPageStitchThreshold.collectAsState()
        SliderItem(
            value = splitPageStitchThreshold,
            valueRange = PagerConfig.SplitPageStitchThreshold.MIN..PagerConfig.SplitPageStitchThreshold.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_threshold),
            valueString = "$splitPageStitchThreshold%",
            onChange = screenModel.preferences.splitPageStitchThreshold::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val splitPageStitchMaxHeightRatio by screenModel.preferences.splitPageStitchMaxHeightRatio.collectAsState()
        SliderItem(
            value = splitPageStitchMaxHeightRatio,
            valueRange = PagerConfig.SplitPageStitchMaxHeightRatio.MIN..PagerConfig.SplitPageStitchMaxHeightRatio.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_max_height_ratio),
            valueString = "%.2f".format(splitPageStitchMaxHeightRatio / 100f),
            onChange = screenModel.preferences.splitPageStitchMaxHeightRatio::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val splitPageStitchMinimumEdgeVariance by screenModel.preferences.splitPageStitchMinimumEdgeVariance.collectAsState()
        SliderItem(
            value = splitPageStitchMinimumEdgeVariance,
            valueRange = PagerConfig.SplitPageStitchMinimumEdgeVariance.MIN..PagerConfig.SplitPageStitchMinimumEdgeVariance.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_minimum_edge_variance),
            valueString = "%.1f%%".format(splitPageStitchMinimumEdgeVariance / 10f),
            onChange = screenModel.preferences.splitPageStitchMinimumEdgeVariance::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val splitPageStitchContinuityMultiplier by screenModel.preferences.splitPageStitchContinuityMultiplier.collectAsState()
        SliderItem(
            value = splitPageStitchContinuityMultiplier,
            valueRange = PagerConfig.SplitPageStitchContinuityMultiplier.MIN..PagerConfig.SplitPageStitchContinuityMultiplier.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_continuity_multiplier),
            valueString = "%.1fx".format(splitPageStitchContinuityMultiplier / 100f),
            onChange = screenModel.preferences.splitPageStitchContinuityMultiplier::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val splitPageStitchMinimumContinuity by screenModel.preferences.splitPageStitchMinimumContinuity.collectAsState()
        SliderItem(
            value = splitPageStitchMinimumContinuity,
            valueRange = PagerConfig.SplitPageStitchMinimumContinuity.MIN..PagerConfig.SplitPageStitchMinimumContinuity.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_minimum_continuity),
            valueString = "%.1f%%".format(splitPageStitchMinimumContinuity / 10f),
            onChange = screenModel.preferences.splitPageStitchMinimumContinuity::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val splitPageStitchSampleColumns by screenModel.preferences.splitPageStitchSampleColumns.collectAsState()
        SliderItem(
            value = splitPageStitchSampleColumns,
            valueRange = PagerConfig.SplitPageStitchSampleColumns.MIN..PagerConfig.SplitPageStitchSampleColumns.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_sample_columns),
            onChange = screenModel.preferences.splitPageStitchSampleColumns::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val splitPageStitchSampleRows by screenModel.preferences.splitPageStitchSampleRows.collectAsState()
        SliderItem(
            value = splitPageStitchSampleRows,
            valueRange = PagerConfig.SplitPageStitchSampleRows.MIN..PagerConfig.SplitPageStitchSampleRows.MAX,
            label = stringResource(SYMR.strings.stitch_split_pages_sample_rows),
            onChange = screenModel.preferences.splitPageStitchSampleRows::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val diagnostics = viewer?.splitPageStitchDiagnostics?.collectAsState()?.value
        val diagnosticsText = diagnostics?.let {
            """
            Decision: ${if (it.stitches) "STITCH" else "REJECT"} (${it.reason.name})
            Samples: ${it.firstWidth}x${it.firstHeight} + ${it.secondWidth}x${it.secondHeight}
            Combined ratio: ${formatDiagnostic(it.combinedHeightRatio)} <= ${formatDiagnostic(it.maximumCombinedHeightRatio)}
            Seam difference: ${formatDiagnostic(it.seamDifference)} <= ${formatDiagnostic(it.seamThreshold)}
            Local edge difference: ${formatDiagnostic(it.localDifference)}
            Edge variance: ${formatDiagnostic(it.edgeVariance)} >= ${formatDiagnostic(it.minimumEdgeVariance)}
            Continuity: ${formatDiagnostic(it.seamDifference)} <= ${formatDiagnostic(it.continuityLimit)}
            Continuity settings: ${formatDiagnostic(it.continuityMultiplier)}x, minimum ${formatDiagnostic(it.minimumContinuity)}
            Sampling: ${it.sampleColumns} columns x ${it.sampleRows} rows
            """.trimIndent()
        } ?: stringResource(SYMR.strings.stitch_split_pages_diagnostics_waiting)

        Text(
            text = "${stringResource(SYMR.strings.stitch_split_pages_diagnostics)}\n$diagnosticsText",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    // SY <--
}

private fun formatDiagnostic(value: Float?): String =
    value?.let { String.format(Locale.US, "%.4f", it) } ?: "n/a"

@Composable
private fun ColumnScope.WebtoonViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val numberFormat = remember { NumberFormat.getPercentInstance() }

    HeadingItem(MR.strings.webtoon_viewer)

    val navigationModeWebtoon by screenModel.preferences.navigationModeWebtoon.collectAsState()
    val webtoonNavInverted by screenModel.preferences.webtoonNavInverted.collectAsState()
    TapZonesItems(
        selected = navigationModeWebtoon,
        onSelect = screenModel.preferences.navigationModeWebtoon::set,
        invertMode = webtoonNavInverted,
        onSelectInvertMode = screenModel.preferences.webtoonNavInverted::set,
    )

    val webtoonSidePadding by screenModel.preferences.webtoonSidePadding.collectAsState()
    SliderItem(
        value = webtoonSidePadding,
        valueRange = ReaderPreferences.let { it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX },
        label = stringResource(MR.strings.pref_webtoon_side_padding),
        valueString = numberFormat.format(webtoonSidePadding / 100f),
        onChange = {
            screenModel.preferences.webtoonSidePadding.set(it)
        },
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        pref = screenModel.preferences.cropBordersWebtoon,
    )

    // SY -->
    CheckboxItem(
        label = stringResource(SYMR.strings.pref_smooth_scroll),
        pref = screenModel.preferences.smoothAutoScroll,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_page_transitions),
        pref = screenModel.preferences.pageTransitionsWebtoon,
    )
    // SY <--

    val dualPageSplitWebtoon by screenModel.preferences.dualPageSplitWebtoon.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_dual_page_split),
        pref = screenModel.preferences.dualPageSplitWebtoon,
    )

    if (dualPageSplitWebtoon) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_invert),
            pref = screenModel.preferences.dualPageInvertWebtoon,
        )
    }

    val dualPageRotateToFitWebtoon by screenModel.preferences.dualPageRotateToFitWebtoon.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_page_rotate),
        pref = screenModel.preferences.dualPageRotateToFitWebtoon,
    )

    if (dualPageRotateToFitWebtoon) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate_invert),
            pref = screenModel.preferences.dualPageRotateToFitInvertWebtoon,
        )
    }

    CheckboxItem(
        label = stringResource(MR.strings.pref_double_tap_zoom),
        pref = screenModel.preferences.webtoonDoubleTapZoomEnabled,
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
        pref = screenModel.preferences.webtoonDisableZoomOut,
    )
}

// SY -->
@Composable
private fun ColumnScope.WebtoonWithGapsViewerSettings(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.vertical_plus_viewer)

    CheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        pref = screenModel.preferences.cropBordersContinuousVertical,
    )
}
// SY <--

@Composable
private fun ColumnScope.TapZonesItems(
    selected: Int,
    onSelect: (Int) -> Unit,
    invertMode: ReaderPreferences.TappingInvertMode,
    onSelectInvertMode: (ReaderPreferences.TappingInvertMode) -> Unit,
) {
    SettingsChipRow(MR.strings.pref_viewer_nav) {
        ReaderPreferences.TapZones.mapIndexed { index, it ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(stringResource(it)) },
            )
        }
    }

    if (selected != 5) {
        SettingsChipRow(MR.strings.pref_read_with_tapping_inverted) {
            ReaderPreferences.TappingInvertMode.entries.map {
                FilterChip(
                    selected = it == invertMode,
                    onClick = { onSelectInvertMode(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }
    }
}
