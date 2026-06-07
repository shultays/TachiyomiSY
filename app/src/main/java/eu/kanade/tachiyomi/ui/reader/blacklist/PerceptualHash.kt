package eu.kanade.tachiyomi.ui.reader.blacklist

import android.graphics.Bitmap
import android.graphics.Color

object PerceptualHash {

    const val HASH_BITS = 256
    const val MATCH_THRESHOLD = 20

    fun calculate(bitmap: Bitmap): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, 17, 16, true)
        return try {
            val hash = ByteArray(HASH_BITS / Byte.SIZE_BITS)
            var bitIndex = 0
            for (y in 0 until scaled.height) {
                for (x in 0 until scaled.width - 1) {
                    val left = luminance(scaled.getPixel(x, y))
                    val right = luminance(scaled.getPixel(x + 1, y))
                    if (left > right) {
                        val byteIndex = bitIndex / Byte.SIZE_BITS
                        val bitInByte = 7 - (bitIndex % Byte.SIZE_BITS)
                        hash[byteIndex] = (hash[byteIndex].toInt() or (1 shl bitInByte)).toByte()
                    }
                    bitIndex++
                }
            }
            hash
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    fun distance(first: ByteArray, second: ByteArray): Int {
        if (first.size != second.size) return Int.MAX_VALUE
        return first.indices.sumOf { index ->
            ((first[index].toInt() xor second[index].toInt()) and 0xFF).countOneBits()
        }
    }

    fun matches(first: ByteArray, second: ByteArray): Boolean {
        return distance(first, second) <= MATCH_THRESHOLD
    }

    private fun luminance(color: Int): Int {
        return (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
    }
}
