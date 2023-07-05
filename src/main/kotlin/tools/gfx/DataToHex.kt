package tools.gfx

import mu.KotlinLogging
import picocli.CommandLine
import java.io.File

private val logger = KotlinLogging.logger {}

// tt data2hex -d fn320x24.data -o fn320x24.hex

@CommandLine.Command(
    name = "data2hex",
    description = ["Convert GIMP data output file to raw hex to be included in asm files"]
)
class DataToHex : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["GIMP data file"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["HEX output file"], required = true)
    var outputFileArg: File? = null

    @CommandLine.Option(names = ["-y", "--height"], description = ["Height of input file"], required = true)
    var height: Int = 0

    @CommandLine.Option(names = ["-b", "--bytesPerLine"], description = ["Number of bytes per line, e.g. 40 for wide, 32 for narrow. Default: 40"], required = false)
    var bytesPerLine: Int = 40

    @CommandLine.Option(names = ["-i", "--invertBytes"], description = ["Inverts the bytes to reverse the white/black. Default: true"], required = false)
    var invert: Boolean = true

    override fun run() {
        val dataFile = dataFileArg!!
        val output = outputFileArg!!

        logger.info { "Reading DATA file: $dataFile" }
        val d = dataFile.inputStream().readBytes()
        val a = mutableListOf<Byte>()

        for (y in 0 until height) {
            for (x in 0 until bytesPerLine) {
                val p = y * (bytesPerLine * 8) + x * 8
                // values are -1 or 0 (FF on, 00 off)
                val v = -(d[p] * 128 + d[p + 1] * 64 + d[p + 2] * 32 + d[p + 3] * 16 + d[p + 4] * 8 + d[p + 5] * 4 + d[p + 6] * 2 + d[p + 7])
                a += if (invert) (255 - v).toByte() else v.toByte()
            }
        }

        logger.info { "Writing raw data to $output" }
        output.writeBytes(a.toByteArray())
    }
}