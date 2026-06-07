package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    // SY -->
    /** Value to check if this page is used to as if it was too wide */
    var shiftedPage: Boolean = false,
    /** Value to check if a page is can be doubled up, but can't because the next page is too wide */
    var isolatedPage: Boolean = false,
    // SY <--
    var stream: (() -> InputStream)? = null,

) : Page(index, url, imageUrl, null), ReaderItem {

    open lateinit var chapter: ReaderChapter

    /** Value to check if a page is too wide to be doubled up */
    var fullPage: Boolean = false
        set(value) {
            field = value
            if (value) shiftedPage = false
        }

    /** Cached result of checking whether this image continues into the next source page. */
    var splitPageNext: Boolean? = null

    var splitPageWidth: Int? = null

    var splitPageHeight: Int? = null

    var splitPageStitchDiagnostics: SplitPageStitchDiagnostics? = null

    var blacklistHash: ByteArray? = null

    var isBlacklisted: Boolean = false
}
