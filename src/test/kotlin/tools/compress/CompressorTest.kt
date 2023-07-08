package tools.compress

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.resourceByteArray

class CompressorTest {
    private val compressor = Compressor()

    private val inputString = "this is some text this is some text this is some text this is some text"
    private val inputData = inputString.toByteArray().toList()

    private val expectedCompressedData = listOf(
        0x12, 0x74, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x73, 0x6f,
        0x6d, 0x65, 0x20, 0x74, 0x65, 0x78, 0x74, 0x20, 0xb2, 0xee, 0x01,
        0x74, 0x00)
        .map { it.toByte() }

    @Test
    fun `can compress text string`() {
        val compressedData = compressor.compress(inputData)
        assertArrayEquals(expectedCompressedData.toByteArray(), compressedData.toByteArray())
    }

    @Test
    fun `can decompress back to text string`() {
        val decompressedData = compressor.decompress(expectedCompressedData)
        assertArrayEquals(inputData.toByteArray(), decompressedData.toByteArray())
        assertEquals(inputString, String(decompressedData.toByteArray()))
    }

    @Test
    fun `can compress complex binary image 1`() {
        val data = resourceByteArray("/butt.hex")
        val compressed = compressor.compress(data.toList()).toByteArray()
        val expected = resourceByteArray("/butt.z")
        assertArrayEquals(expected, compressed)
    }

    @Test
    fun `can decompress complex binary image 1`() {
        val data = resourceByteArray("/butt.z")
        val decompressed = compressor.decompress(data.toList()).toByteArray()
        val expected = resourceByteArray("/butt.hex")
        assertArrayEquals(expected, decompressed)
    }

    @Test
    fun `can compress complex binary image 2`() {
        val data = resourceByteArray("/fn-160x28x4c.hex")
        val compressed = compressor.compress(data.toList()).toByteArray()
        val expected = resourceByteArray("/fn-160x28x4c.z")
        assertArrayEquals(expected, compressed)
    }

    @Test
    fun `can decompress complex binary image 2`() {
        val data = resourceByteArray("/fn-160x28x4c.z")
        val decompressed = compressor.decompress(data.toList()).toByteArray()
        val expected = resourceByteArray("/fn-160x28x4c.hex")
        assertArrayEquals(expected, decompressed)
    }

    private fun foo(v: Int) : Int {
        println("got $v")
        return v
    }

    @Test
    fun `post inc happens after call to function`() {
        var i = 0
        assertEquals(0, foo(i++))
        assertEquals(1, i)
    }
}