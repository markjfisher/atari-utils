package tools.mads

import picocli.CommandLine
import java.io.File

@CommandLine.Command(
    name = "obxread",
    description = ["Read MADS obx file and print details about it"]
)
class OBXReader: Runnable {
    @CommandLine.Option(names = ["-f", "--obxfile"], description = ["target file to analyse"], required = true)
    var obxFileArg: File? = null

    override fun run() {
        val obxFile = obxFileArg!!

        val data = obxFile.readBytes()
        val header = hexFromRange(data, 0..1)
        if (header != "ffff") throw Exception("Unknown file header: $header")
        val startAddress = hexFromRange(data = data, range = 2..3)
        when(startAddress) {
            "0000" -> handleRelocBlock(data)
            else -> handleDOSBlock(data)
        }
    }

    private fun hexFromRange(data: ByteArray, range: IntRange, separator: String = "", eachPrefix: String = "", allPrefix: String = "", reverse: Boolean = true): String {
        val copy = data.sliceArray(range)
        // little endian to correct reading order, e.g. bytes $34, $12 => $1234
        if (reverse) copy.reverse()
        return allPrefix + copy.joinToString(separator = separator) { eachByte -> "${eachPrefix}%02x".format(eachByte) }
    }

    private fun handleDOSBlock(data: ByteArray) {
        val xex = DOSFile(data)
        xex.dump()
    }

    private fun handleRelocBlock(data: ByteArray) {
        val endAddress = hexFromRange(data = data, range = 4..5)
        val len = endAddress.toInt(16) + 1

        // validate MADS reloc header
        val madHeader = hexFromRange(data = data, range = 6..7)
        if (madHeader != "524d") throw Exception("Unexpected mads reloc header, should be 524d, found: $madHeader")

        val config = data[9].toUInt().toInt()
        val configString = "Assembled Starting " + if (config == 0) "\$0000" else "\$0100"
        val sp = hexFromRange(data = data, range = 10..11)
        val sa = hexFromRange(data = data, range = 12..13)
        val pva = hexFromRange(data = data, range = 14..15)

        println("""
            Relocatable Block
                       Header: ${'$'}ffff
                Start Address: ${'$'}0000
                  End Address: ${'$'}${endAddress}
                       Length: ${'$'}${"%04x".format(len)}
                       Config: $configString
                Stack Pointer: ${'$'}$sp
                Stack Address: ${'$'}$sa
               Proc Vars Addr: ${'$'}$pva
        """.trimIndent())

        // Now process the sub-blocks
        var i = 16
        while (i < data.size) {
            i += when (val blockHeader = hexFromRange(data, i .. (i+1))) {
                "ffef" -> processRelocSubBlock(data, i)
                "ffee" -> processExternalSubBlock(data, i)
                "ffed" -> processPublicSubBlock(data, i)
                else -> throw Exception("Unknown sub-block header: $blockHeader")
            }
        }

    }

    private fun processPublicSubBlock(data: ByteArray, i: Int): Int {
        return 0
    }

    private fun processExternalSubBlock(data: ByteArray, i: Int): Int {
        return 0
    }

    private fun processRelocSubBlock(data: ByteArray, i: Int): Int {
        var index = i + 2
        val type = data[index++].toUInt().toInt().toChar()
        val dataLength = hexFromRange(data, index .. (index+1))
        val len = dataLength.toInt(16)
        (0 until len).forEach { n ->

        }
        return 0
    }

}