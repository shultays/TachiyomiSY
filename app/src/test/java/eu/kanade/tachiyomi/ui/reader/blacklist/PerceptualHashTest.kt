package eu.kanade.tachiyomi.ui.reader.blacklist

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PerceptualHashTest {

    @Test
    fun `distance counts differing bits across signed bytes`() {
        val first = byteArrayOf(0x00, 0xFF.toByte())
        val second = byteArrayOf(0xFF.toByte(), 0x00)

        assertEquals(16, PerceptualHash.distance(first, second))
    }

    @Test
    fun `matching uses the configured 256 bit threshold`() {
        val first = ByteArray(PerceptualHash.HASH_BITS / Byte.SIZE_BITS)
        val withinThreshold = first.copyOf().apply {
            repeat(PerceptualHash.MATCH_THRESHOLD) { bit ->
                val byteIndex = bit / Byte.SIZE_BITS
                this[byteIndex] = (this[byteIndex].toInt() or (1 shl (bit % Byte.SIZE_BITS))).toByte()
            }
        }
        val outsideThreshold = withinThreshold.copyOf().apply {
            val bit = PerceptualHash.MATCH_THRESHOLD
            val byteIndex = bit / Byte.SIZE_BITS
            this[byteIndex] = (this[byteIndex].toInt() or (1 shl (bit % Byte.SIZE_BITS))).toByte()
        }

        assertTrue(PerceptualHash.matches(first, withinThreshold))
        assertFalse(PerceptualHash.matches(first, outsideThreshold))
    }
}
