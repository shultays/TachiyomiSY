package eu.kanade.tachiyomi.ui.reader.blacklist

import android.content.Context
import android.graphics.Bitmap
import eu.kanade.tachiyomi.data.backup.models.BackupBlacklistedPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import tachiyomi.decoder.ImageDecoder
import java.io.File
import java.util.UUID

class BlacklistedPageStore(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("reader_blacklisted_pages", Context.MODE_PRIVATE)
    private val root = File(appContext.filesDir, "reader_blacklisted_pages")

    fun get(mangaId: Long): List<BlacklistedPage> {
        return preferences.getStringSet(key(mangaId), emptySet()).orEmpty()
            .mapNotNull(::decode)
            .filter { it.thumbnail.exists() }
            .sortedByDescending { it.createdAt }
    }

    fun add(mangaId: Long, page: ReaderPage): BlacklistedPage? {
        val bitmap = decodePage(page) ?: return null
        return try {
            val hash = PerceptualHash.calculate(bitmap)
            val existing = get(mangaId)
            existing.firstOrNull { PerceptualHash.matches(hash, it.hash) }?.let { return it }

            val id = UUID.randomUUID().toString()
            val directory = File(root, mangaId.toString()).apply { mkdirs() }
            val thumbnail = File(directory, "$id.jpg")
            thumbnail.outputStream().buffered().use { output ->
                createThumbnail(bitmap).let { thumb ->
                    try {
                        thumb.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    } finally {
                        if (thumb !== bitmap) thumb.recycle()
                    }
                }
            }

            BlacklistedPage(id, hash, thumbnail, System.currentTimeMillis()).also {
                write(mangaId, existing + it)
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun remove(mangaId: Long, id: String) {
        val entries = get(mangaId)
        entries.firstOrNull { it.id == id }?.thumbnail?.delete()
        write(mangaId, entries.filterNot { it.id == id })
    }

    fun backup(mangaId: Long): List<BackupBlacklistedPage> {
        return get(mangaId).mapNotNull { entry ->
            runCatching {
                BackupBlacklistedPage(
                    id = entry.id,
                    hash = entry.hash,
                    thumbnail = entry.thumbnail.readBytes(),
                    createdAt = entry.createdAt,
                )
            }.getOrNull()
        }
    }

    fun restore(mangaId: Long, backupEntries: List<BackupBlacklistedPage>) {
        if (backupEntries.isEmpty()) return

        val existing = get(mangaId).toMutableList()
        val directory = File(root, mangaId.toString()).apply { mkdirs() }
        backupEntries.forEach { backup ->
            if (backup.thumbnail.isEmpty() || existing.any { PerceptualHash.matches(backup.hash, it.hash) }) return@forEach

            val id = backup.id.takeUnless { candidate -> existing.any { it.id == candidate } }
                ?: UUID.randomUUID().toString()
            val thumbnail = File(directory, "$id.jpg")
            runCatching {
                thumbnail.writeBytes(backup.thumbnail)
                existing += BlacklistedPage(id, backup.hash, thumbnail, backup.createdAt)
            }
        }
        write(mangaId, existing)
    }

    fun hash(page: ReaderPage): ByteArray? {
        page.blacklistHash?.let { return it }
        val bitmap = decodePage(page) ?: return null
        return try {
            PerceptualHash.calculate(bitmap).also { page.blacklistHash = it }
        } finally {
            bitmap.recycle()
        }
    }

    fun matches(page: ReaderPage, entries: List<BlacklistedPage>): Boolean {
        if (entries.isEmpty()) return false
        val hash = hash(page) ?: return false
        return entries.any { PerceptualHash.matches(hash, it.hash) }
    }

    private fun decodePage(page: ReaderPage): Bitmap? {
        val stream = page.stream ?: return null
        return runCatching {
            stream().use { ImageDecoder.newInstance(it)?.decode(sampleSize = 8) }
        }.getOrNull()
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val maxSize = 96f
        val scale = minOf(maxSize / bitmap.width, maxSize / bitmap.height, 1f)
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun write(mangaId: Long, entries: List<BlacklistedPage>) {
        preferences.edit()
            .putStringSet(key(mangaId), entries.map(::encode).toSet())
            .apply()
    }

    private fun encode(entry: BlacklistedPage): String {
        return listOf(
            entry.id,
            entry.hash.toHex(),
            entry.thumbnail.absolutePath,
            entry.createdAt.toString(),
        ).joinToString("|")
    }

    private fun decode(value: String): BlacklistedPage? {
        val parts = value.split('|')
        if (parts.size != 4) return null
        return runCatching {
            BlacklistedPage(
                id = parts[0],
                hash = parts[1].hexToBytes(),
                thumbnail = File(parts[2]),
                createdAt = parts[3].toLong(),
            )
        }.getOrNull()
    }

    private fun key(mangaId: Long) = "manga_$mangaId"

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0)
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
