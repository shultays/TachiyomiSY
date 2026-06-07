package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ColumnScope.BlacklistedPagesPage(screenModel: ReaderSettingsScreenModel) {
    val pages by screenModel.blacklistedPagesFlow.collectAsState()

    Button(
        onClick = screenModel.onAddCurrentPageToBlacklist,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(stringResource(MR.strings.blacklist_current_page))
    }

    Spacer(Modifier.height(12.dp))

    if (pages.isEmpty()) {
        Text(
            text = stringResource(MR.strings.no_blacklisted_pages),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    } else {
        pages.forEachIndexed { index, page ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AsyncImage(
                    model = page.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 64.dp, height = 96.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(MR.strings.blacklisted_page_number, index + 1),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { screenModel.onRemoveBlacklistedPage(page.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(MR.strings.action_delete),
                    )
                }
            }
        }
    }
}
