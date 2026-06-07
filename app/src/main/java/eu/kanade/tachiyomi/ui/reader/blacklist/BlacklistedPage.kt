package eu.kanade.tachiyomi.ui.reader.blacklist

import java.io.File

data class BlacklistedPage(
    val id: String,
    val hash: ByteArray,
    val thumbnail: File,
    val createdAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        return other is BlacklistedPage &&
            id == other.id &&
            hash.contentEquals(other.hash) &&
            thumbnail == other.thumbnail &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + hash.contentHashCode()
        result = 31 * result + thumbnail.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
