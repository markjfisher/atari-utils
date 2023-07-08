package tools.compress

import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

class Compressor {
    val debug = false

    data class MutableInt(var value: Int) {
        override fun toString(): String = value.toString()
        fun set(i: Int) {
            value = i
        }
    }


    fun decompress(compressedData: List<Byte>): List<Byte> {
        val decompressedData = ByteArray(MAX_FILE_SIZE) { _ -> 0 }
        var decompressedSize = 0

        var src = 0
        while (compressedData[src].toInt() != 0) {
            if (compressedData[src].toInt() and 0x80 != 0) {
                val len = ((compressedData[src++].toUByte() and 127u) + 2u).toInt()
                // care must be taken to treat the byte of data as unsigned
                var off = (decompressedSize - 256 + compressedData[src].toUByte().toInt())
                repeat(len) {
                    decompressedData[decompressedSize++] = decompressedData[off++]
                }
            } else {
                val len = (compressedData[src].toUByte() and 127u).toInt()
                repeat(len) {
                    decompressedData[decompressedSize++] = compressedData[++src]
                }
            }
            src++
        }
        return decompressedData.toList().subList(0, decompressedSize)
    }

    private fun findBestSeq(from: MutableInt, dst: Int, bytes: List<Byte>): Int {
        var best = 0
        for (src in max(dst - MAX_OFFSET, 0) until dst) {
            for (num in 0 until min(bytes.size - dst, MAX_SEQ_LEN)) {
                if (((dst + num) >= bytes.size) || (bytes[src + num] != bytes[dst + num])) break
                if (num > best) {
                    from.set(src)
                    best = num
                }
            }
        }
        if (debug) println("best for ${from.value} at $dst is $best")
        return best
    }

    fun compress(bytes: List<Byte>): List<Byte> {
        val compressedData = ByteArray(bytes.size) { _ -> 0 }
        var compressedSize = 0
        var rawCopyLen = 0
        var rawLenAddr = 0
        var dst = 0
        while (dst < bytes.size) {
            val src = MutableInt(0)
            val best = findBestSeq(src, dst, bytes)
            if (best >= (COST + if (rawCopyLen == 0) 0 else 1)) {
                if (rawCopyLen != 0) {
                    // compressedData[rawLenAddr] = rawCopyLen.toByte()
                    setCompressedData(compressedData, rawLenAddr, rawCopyLen.toByte())
                    rawCopyLen = 0
                }
                val b1 = (best - 2 or 0x80).toByte()
                val b2 = (src.value - dst + 0x100).toByte()
                setCompressedData(compressedData, compressedSize++, b1)
                setCompressedData(compressedData, compressedSize++, b2)
                dst += best
            } else {
                if (rawCopyLen == 127) { // max RAW copy len
                    setCompressedData(compressedData, rawLenAddr, rawCopyLen.toByte())
                    // compressedData[rawLenAddr] = rawCopyLen.toByte()
                    rawCopyLen = 0
                }
                if (rawCopyLen == 0) {
                    rawLenAddr = compressedSize++
                }
                setCompressedData(compressedData, compressedSize++, bytes[dst++])
                // compressedData[compressedSize++] = bytes[dst++]
                rawCopyLen++
            }
        }
        if (rawCopyLen != 0) {
            setCompressedData(compressedData, rawLenAddr, rawCopyLen.toByte())
            // compressedData[rawLenAddr] = rawCopyLen.toByte()
        }
        setCompressedData(compressedData, compressedSize++, 0)
//        compressedData[compressedSize++] = 0
        return compressedData.toList().subList(0, compressedSize)
    }

    private fun setCompressedData(data: ByteArray, dst: Int, v: Byte) {
        if (debug) println("c[$dst] -> ${if (v<0) v + 256 else v}")
        data[dst] = v
    }


    companion object {
        const val MAX_OFFSET = 256
        const val MAX_SEQ_LEN = 127 + 2
        const val COST = 2
        const val MAX_FILE_SIZE = 2_000_000
    }

}